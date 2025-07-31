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

import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final RestTemplate restTemplate;
    private final DocumentReferenceRepository documentReferenceRepository;
    private static final int TOKENS_PER_CHUNK = 400;

    public UploadController(RestTemplate restTemplate,
                            DocumentReferenceRepository documentReferenceRepository) {
        this.restTemplate = restTemplate;
        this.documentReferenceRepository = documentReferenceRepository;
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
            String userId   = getUserId(auth);
            String filename = Objects.requireNonNull(file.getOriginalFilename());
            String ext      = StringUtils.getFilenameExtension(filename).toLowerCase();
            String docId    = UUID.randomUUID().toString();
            workspace = (workspace!=null&&!workspace.isBlank())?workspace.trim():"default";

            String rawText;
            if ("pdf".equals(ext)) {
                try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
                    rawText = new PDFTextStripper().getText(pdf);
                }
            } else if ("txt".equals(ext)) {
                rawText = new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                return ResponseEntity.badRequest().body("Unsupported: " + ext);
            }

            Path path = Paths.get("uploads", docId+".pdf");
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            ingestText(rawText, filename, docId, workspace, userId, "upload");

            DocumentReference ref = new DocumentReference(
                docId, userId, filename, null, "upload"
            );
            ref.setFileSize(file.getSize());
            ref.setCreatedAt(Instant.now());
            documentReferenceRepository.save(ref);

            int words  = rawText.split("\\s+").length;
            int chunks = chunkText(rawText,TOKENS_PER_CHUNK).size();
            return ResponseEntity.ok(Map.of(
                "docId",docId,"name",filename,"words",words,"chunks",chunks
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Upload failed: "+e.getMessage());
        }
    }

    @PostMapping("/upload/external")
    public ResponseEntity<Void> saveExternal(@RequestBody Map<String,String> body) throws Exception {
        String url       = body.get("url");
        String name      = body.get("name");
        String workspace = body.getOrDefault("workspace","default");
        String userId    = body.get("userId");
        if (url==null||url.isBlank()||userId==null||userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        byte[] bytes = HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder(java.net.URI.create(url)).GET().build(),
                  HttpResponse.BodyHandlers.ofByteArray())
            .body();
        String docId = UUID.randomUUID().toString();
        Path path = Paths.get("uploads",docId+".pdf");
        Files.createDirectories(path.getParent());
        Files.write(path,bytes);

        String rawText;
        try (PDDocument pdf = PDDocument.load(bytes)) {
            rawText = new PDFTextStripper().getText(pdf);
        }
        ingestText(rawText, name!=null?name:url, docId, workspace, userId, "drive");

        DocumentReference ref = new DocumentReference(
            docId, userId, name!=null?name:url, docId, "drive"
        );
        ref.setFileSize((long)bytes.length);
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
                Document(where:{path:["userId"],operator:Equal,valueText:"%s"}) {
                  _additional{id}
                  title processed pages workspace source userId
                }
              }
            }
        """.formatted(userId.replace("\"","\\\""));

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        String weav = System.getenv("WEAVIATE_URL");
        if (weav==null||weav.isBlank()) weav="http://localhost:8080";
        if (!weav.startsWith("http")) weav="https://"+weav;
        String weavKey = System.getenv("WEAVIATE_API_KEY");
        if (weavKey!=null&&!weavKey.isBlank()) h.set("X-API-KEY",weavKey);

        try {
          @SuppressWarnings("unchecked")
          Map<?,?> resp = restTemplate.postForObject(
            weav+"/v1/graphql",
            new HttpEntity<>(Map.of("query",gql),h),
            Map.class
          );
          @SuppressWarnings("unchecked")
          List<Map<String,Object>> docs = (List<Map<String,Object>>)
            ((Map<?,?>)((Map<?,?>)resp.get("data")).get("Get")).get("Document");
          return docs!=null?docs:List.of();
        } catch(Exception e) {
          e.printStackTrace();
          return List.of();
        }
    }

    private void ingestText(String rawText,
                            String filename,
                            String docId,
                            String workspace,
                            String userId,
                            String source) {
        try {
            List<String> chunks = chunkText(rawText, TOKENS_PER_CHUNK);

            // OpenAI embeddings
            String openAiKey = System.getenv("OPENAI_API_KEY");
            HttpHeaders embH = new HttpHeaders();
            embH.setContentType(MediaType.APPLICATION_JSON);
            embH.setBearerAuth(openAiKey);
            Map<String,Object> embReq = Map.of("model","text-embedding-ada-002","input",chunks);
            @SuppressWarnings("unchecked")
            Map<?,?> embResp = restTemplate.postForEntity(
              "https://api.openai.com/v1/embeddings",
              new HttpEntity<>(embReq,embH),
              Map.class
            ).getBody();
            @SuppressWarnings("unchecked")
            List<Map<String,Object>> data = (List<Map<String,Object>>)embResp.get("data");
            List<List<Double>> vectors = new ArrayList<>();
            for (var item : data) {
              @SuppressWarnings("unchecked")
              List<Double> v = (List<Double>) item.get("embedding");
              vectors.add(v);
            }

            // Weaviate store
            String weav = System.getenv("WEAVIATE_URL");
            if (!weav.startsWith("http")) weav="https://"+weav;
            String endpoint = weav + "/v1/objects";
            HttpHeaders jsonH = new HttpHeaders();
            jsonH.setContentType(MediaType.APPLICATION_JSON);
            String weavKey = System.getenv("WEAVIATE_API_KEY");
            if (weavKey!=null&&!weavKey.isBlank()) jsonH.set("X-API-KEY",weavKey);

            for (int i=0;i<chunks.size();i++) {
                Map<String,Object> obj = Map.of(
                  "class","Chunk",
                  "id",UUID.randomUUID().toString(),
                  "properties",Map.of(
                    "docId", docId,
                    "text",  chunks.get(i),
                    "page",  i+1,
                    "userId",userId
                  ),
                  "vector", vectors.get(i)
                );
                restTemplate.postForEntity(endpoint,new HttpEntity<>(obj,jsonH),String.class);
            }

            Map<String,Object> docObj = Map.of(
                "class","Document",
                "id",   docId,
                "properties",Map.of(
                  "title",     filename,
                  "pages",     chunks.size(),
                  "processed", true,
                  "workspace", workspace,
                  "userId",    userId,
                  "source",    source
                )
            );
            restTemplate.postForEntity(endpoint,new HttpEntity<>(docObj,jsonH),String.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> chunkText(String text, int maxTokens) {
        String[] w = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        for (int i=0;i<w.length;i+=maxTokens) {
          chunks.add(String.join(" ",
            Arrays.copyOfRange(w,i,Math.min(i+maxTokens,w.length))));
        }
        return chunks;
    }
}
