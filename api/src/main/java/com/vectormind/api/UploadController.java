package com.vectormind.api;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import com.vectormind.api.config.WeaviateConfig;

import java.io.*;
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

    private static final int TOKENS_PER_CHUNK = 400;

    public UploadController(RestTemplate restTemplate,
                            DocumentReferenceRepository documentReferenceRepository,
                            WeaviateConfig weaviateConfig) {
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
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "workspace", required = false) String workspace,
                                    Authentication auth) {
        try {
            String userId = getUserId(auth);
            String filename = Objects.requireNonNull(file.getOriginalFilename());
            String ext = Optional.ofNullable(StringUtils.getFilenameExtension(filename))
                                 .orElse("").toLowerCase();
            String docId = UUID.randomUUID().toString();
            workspace = (workspace != null && !workspace.isBlank()) ? workspace.trim() : "default";

            // 1) Extract text
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

            // 2) Save file locally (for download later)
            Path path = Paths.get("uploads", docId + ".pdf");
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            // 3) Ingest into Weaviate
            ingestText(rawText, filename, docId, workspace, userId, "upload");

            // 4) Save metadata
            DocumentReference ref = new DocumentReference(docId, userId, filename, null, "upload");
            ref.setFileSize(file.getSize());
            ref.setCreatedAt(Instant.now());
            documentReferenceRepository.save(ref);

            // 5) Return summary
            int wordCount = rawText.split("\\s+").length;
            int chunkCount = chunkText(rawText, TOKENS_PER_CHUNK).size();
            return ResponseEntity.ok(Map.of(
                "docId", docId,
                "name", filename,
                "words", wordCount,
                "chunks", chunkCount
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Upload failed: " + e.getMessage());
        }
    }

    private void ingestText(String rawText,
                            String filename,
                            String docId,
                            String workspace,
                            String userId,
                            String source) {
        try {
            // 1) Chunk the text
            List<String> chunks = chunkText(rawText, TOKENS_PER_CHUNK);

            // 2) Get embeddings from OpenAI
            String openAiKey = System.getenv("OPENAI_API_KEY");
            HttpHeaders embH = new HttpHeaders();
            embH.setContentType(MediaType.APPLICATION_JSON);
            embH.setBearerAuth(openAiKey);
            Map<String,Object> embReq = Map.of(
              "model", "text-embedding-ada-002",
              "input", chunks
            );
            @SuppressWarnings("unchecked")
            Map<?,?> embResp = restTemplate.postForObject(
              "https://api.openai.com/v1/embeddings",
              new HttpEntity<>(embReq, embH),
              Map.class
            );
            @SuppressWarnings("unchecked")
            List<Map<String,Object>> data = (List<Map<String,Object>>) embResp.get("data");
            List<List<Double>> vectors = new ArrayList<>();
            for (var item : data) {
                @SuppressWarnings("unchecked")
                List<Double> v = (List<Double>) item.get("embedding");
                vectors.add(v);
            }

            // 3) POST each chunk to Weaviate
            String endpoint = weaviateConfig.getObjectsEndpoint();
            HttpHeaders jsonH = new HttpHeaders();
            jsonH.setContentType(MediaType.APPLICATION_JSON);
            String weavKey = System.getenv("WEAVIATE_API_KEY");
            if (weavKey != null && !weavKey.isBlank()) {
                jsonH.set("X-API-KEY", weavKey);
            }

            for (int i = 0; i < chunks.size(); i++) {
                Map<String,Object> obj = Map.of(
                  "class",    "Chunk",
                  "id",       UUID.randomUUID().toString(),
                  "properties", Map.of(
                      "docId",  docId,
                      "text",   chunks.get(i),
                      "page",   i + 1,
                      "userId", userId
                  ),
                  "vector", vectors.get(i)
                );
                restTemplate.postForEntity(endpoint,
                                          new HttpEntity<>(obj, jsonH),
                                          String.class);
            }

            // 4) POST the Document object
            Map<String,Object> docObj = Map.of(
              "class", "Document",
              "id",    docId,
              "properties", Map.of(
                  "title",     filename,
                  "pages",     chunks.size(),
                  "processed", true,
                  "workspace", workspace,
                  "userId",    userId,
                  "source",    source
              )
            );
            restTemplate.postForEntity(endpoint,
                                      new HttpEntity<>(docObj, jsonH),
                                      String.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> chunkText(String text, int maxTokens) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < words.length; i += maxTokens) {
            chunks.add(String.join(" ",
              Arrays.copyOfRange(words, i, Math.min(i + maxTokens, words.length))
            ));
        }
        return chunks;
    }
}
