package com.vectormind.api;

import com.vectormind.api.config.WeaviateConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final RestTemplate restTemplate;
    private final DocumentReferenceRepository documentReferenceRepository;
    private final WeaviateConfig weaviateConfig;

    @Value("${weaviate.api-key:}")
    private String weaviateApiKey;

    @Value("${embedding.service.url}")
    private String embeddingServiceUrl;

    private static final int TOKENS_PER_CHUNK = 400;

    public UploadController(
        RestTemplate restTemplate,
        DocumentReferenceRepository documentReferenceRepository,
        WeaviateConfig weaviateConfig
    ) {
        this.restTemplate = restTemplate;
        this.documentReferenceRepository = documentReferenceRepository;
        this.weaviateConfig = weaviateConfig;
    }

    private String getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        throw new RuntimeException("User not authenticated");
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "workspace", required = false) String workspace,
        Authentication auth
    ) {
        try {
            String userId   = getUserId(auth);
            String filename = Objects.requireNonNull(file.getOriginalFilename());
            String ext      = StringUtils.getFilenameExtension(filename).toLowerCase();
            String docId    = UUID.randomUUID().toString();
            workspace       = (workspace != null && !workspace.isBlank()) ? workspace.trim() : "default";

            // 1) extract raw text
            String rawText;
            if ("pdf".equals(ext)) {
                try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
                    rawText = new PDFTextStripper().getText(pdf);
                }
            } else if ("txt".equals(ext)) {
                rawText = new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                return ResponseEntity.badRequest().body("Unsupported file type: " + ext);
            }

            // 2) save file to disk
            Path path = Paths.get("uploads", docId + ".pdf");
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            // 3) ingest into Weaviate
            ingestText(rawText, filename, docId, workspace, userId, "upload");

            // 4) save metadata
            DocumentReference ref = new DocumentReference(
                docId, userId, filename, null, "upload"
            );
            ref.setFileSize(file.getSize());
            ref.setCreatedAt(Instant.now());
            documentReferenceRepository.save(ref);

            int wordCount  = rawText.split("\\s+").length;
            int chunkCount = chunkText(rawText, TOKENS_PER_CHUNK).size();

            return ResponseEntity.ok(Map.of(
                "docId",  docId,
                "name",   filename,
                "words",  wordCount,
                "chunks", chunkCount
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload/external")
    public ResponseEntity<Void> saveExternal(@RequestBody Map<String, String> body) throws Exception {
        String url       = body.get("url");
        String name      = body.get("name");
        String workspace = body.getOrDefault("workspace","default");
        String userId    = body.get("userId");

        if (url == null || url.isBlank() || userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] bytes = HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                  HttpResponse.BodyHandlers.ofByteArray())
            .body();

        String docId = UUID.randomUUID().toString();
        Path path   = Paths.get("uploads", docId + ".pdf");
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);

        String rawText;
        try (PDDocument pdf = PDDocument.load(bytes)) {
            rawText = new PDFTextStripper().getText(pdf);
        }

        ingestText(rawText, name != null ? name : url, docId, workspace, userId, "drive");

        DocumentReference ref = new DocumentReference(
            docId, userId, name != null ? name : url, docId, "drive"
        );
        ref.setFileSize((long) bytes.length);
        ref.setCreatedAt(Instant.now());
        documentReferenceRepository.save(ref);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/documents")
    public List<Map<String,Object>> listDocs(Authentication auth) {
        String userId = getUserId(auth);
        String gql = """
            {
              Get {
                Document(
                  where:{ path:["userId"], operator:Equal, valueText:"%s" }
                ) {
                  _additional { id }
                  title processed pages workspace source userId
                }
              }
            }
        """.formatted(userId.replace("\"","\\\""));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", weaviateApiKey);

        @SuppressWarnings("unchecked")
        Map<?,?> res = restTemplate.postForObject(
            weaviateConfig.getGraphQLEndpoint(),
            new HttpEntity<>(Map.of("query",gql), headers),
            Map.class
        );

        if (res == null || res.get("data")==null) return List.of();
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> docs = (List<Map<String,Object>>)
          ((Map<?,?>)((Map<?,?>)res.get("data")).get("Get")).get("Document");
        return docs!=null ? docs : List.of();
    }

    private void ingestText(
        String rawText,
        String filename,
        String docId,
        String workspace,
        String userId,
        String source
    ) {
        try {
            List<String> chunks = chunkText(rawText, TOKENS_PER_CHUNK);
            Map<String,Object> embedReq = Map.of("texts", chunks);

            // ——— 1) fetch embeddings from your injected service ———
            HttpHeaders embedHdr = new HttpHeaders();
            embedHdr.setContentType(MediaType.APPLICATION_JSON);
            @SuppressWarnings("unchecked")
            List<List<Double>> vectors = (List<List<Double>>)
              restTemplate.postForObject(
                embeddingServiceUrl + "/embed",
                new HttpEntity<>(embedReq, embedHdr),
                Map.class
              ).get("embeddings");

            // ——— 2) push to Weaviate ———
            HttpHeaders weavHdr = new HttpHeaders();
            weavHdr.setContentType(MediaType.APPLICATION_JSON);
            weavHdr.set("X-API-KEY", weaviateApiKey);

            for (int i = 0; i < chunks.size(); i++) {
                Map<String,Object> obj = Map.of(
                  "class","Chunk",
                  "id", UUID.randomUUID().toString(),
                  "properties", Map.of(
                    "docId",  docId,
                    "text",   chunks.get(i),
                    "page",   i+1,
                    "userId", userId
                  ),
                  "vector", vectors.get(i)
                );
                restTemplate.postForEntity(
                  weaviateConfig.getObjectsEndpoint(),
                  new HttpEntity<>(obj, weavHdr),
                  String.class
                );
            }

            // ——— finally, store the document record ———
            restTemplate.postForEntity(
              weaviateConfig.getObjectsEndpoint(),
              new HttpEntity<>(Map.of(
                "class","Document",
                "id",    docId,
                "properties", Map.of(
                  "title",     filename,
                  "pages",     chunks.size(),
                  "processed", true,
                  "workspace", workspace,
                  "userId",    userId,
                  "source",    source
                )
              ), weavHdr),
              String.class
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> chunkText(String text, int maxTokens) {
        String[] words = text.split("\\s+");
        List<String> out = new ArrayList<>();
        for (int i = 0; i < words.length; i += maxTokens) {
            out.add(String.join(" ",
              Arrays.copyOfRange(words, i, Math.min(i+maxTokens, words.length))
            ));
        }
        return out;
    }
}
