package com.vectormind.api;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);
    private final RestTemplate restTemplate;
    private final DriveTokenRepository driveTokenRepository;

    @Value("${openai.api.key:}")
    private String cfgKey;

    private static final int TOKENS_PER_CHUNK = 400;
    private static final String DEMO_USER = "demo-user";

    public FallbackController(RestTemplate restTemplate,
                              DriveTokenRepository driveTokenRepository) {
        this.restTemplate = restTemplate;
        this.driveTokenRepository = driveTokenRepository;
    }

    @GetMapping("/drive/status")
    public ResponseEntity<Map<String, Boolean>> driveStatus() {
        log.info("Fallback /drive/status called");
        boolean has = driveTokenRepository.findByUserId(DEMO_USER).isPresent();
        return ResponseEntity.ok(Map.of("hasToken", has));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "workspace", defaultValue = "default") String workspace
    ) {
        String userId   = DEMO_USER;
        String filename = Objects.requireNonNull(file.getOriginalFilename());
        String ext      = Optional.ofNullable(StringUtils.getFilenameExtension(filename))
                       .orElse("").toLowerCase();
        String docId    = UUID.randomUUID().toString();

        try {
            String rawText;
            if ("pdf".equals(ext)) {
                try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
                    rawText = new PDFTextStripper().getText(pdf);
                }
            } else if ("txt".equals(ext)) {
                rawText = new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Unsupported: " + ext));
            }

            Path out = Paths.get("uploads", docId + ".pdf");
            Files.createDirectories(out.getParent());
            Files.copy(file.getInputStream(), out, StandardCopyOption.REPLACE_EXISTING);

            ingestText(rawText, filename, docId, workspace, userId, "upload");

            int words  = rawText.split("\\s+").length;
            int chunks = chunkText(rawText, TOKENS_PER_CHUNK).size();
            return ResponseEntity.ok(Map.of(
                "docId",  docId,
                "name",   filename,
                "words",  words,
                "chunks", chunks
            ));
        } catch (IOException e) {
            log.error("Fallback upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload/external")
    public ResponseEntity<?> saveExternal(@RequestBody Map<String, String> body) {
        String url       = body.get("url");
        String name      = body.getOrDefault("name", url);
        String workspace = body.getOrDefault("workspace", "default");

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error","url is required"));
        }

        try {
            byte[] bytes = HttpClient.newHttpClient()
              .send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray())
              .body();

            String docId = UUID.randomUUID().toString();
            Path out = Paths.get("uploads", docId + ".pdf");
            Files.createDirectories(out.getParent());
            Files.write(out, bytes);

            String rawText;
            try (PDDocument pdf = PDDocument.load(bytes)) {
                rawText = new PDFTextStripper().getText(pdf);
            }
            ingestText(rawText, name, docId, workspace, DEMO_USER, "external");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Fallback external upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", e.getMessage()));
        }
    }

    private void ingestText(String text,
                            String filename,
                            String docId,
                            String workspace,
                            String userId,
                            String source) {
        try {
            List<String> chunks = chunkText(text, TOKENS_PER_CHUNK);

            String key = cfgKey.isBlank() ? System.getenv("OPENAI_API_KEY") : cfgKey;
            HttpHeaders hdr = new HttpHeaders();
            hdr.setContentType(MediaType.APPLICATION_JSON);
            hdr.setBearerAuth(key);
            Map<String,Object> req = Map.of("model","text-embedding-ada-002","input",chunks);
            @SuppressWarnings("unchecked")
            Map<?,?> emb = restTemplate.postForEntity(
              "https://api.openai.com/v1/embeddings",
              new HttpEntity<>(req, hdr),
              Map.class
            ).getBody();

            @SuppressWarnings("unchecked")
            List<Map<String,Object>> data = (List<Map<String,Object>>) emb.get("data");
            List<List<Double>> vectors = new ArrayList<>();
            for (var item : data) {
              @SuppressWarnings("unchecked")
              List<Double> v = (List<Double>) item.get("embedding");
              vectors.add(v);
            }

            String weav = System.getenv("WEAVIATE_URL");
            if (!weav.startsWith("http")) weav = "https://" + weav;
            String endpoint = weav + "/v1/objects";
            HttpHeaders jsonH = new HttpHeaders();
            jsonH.setContentType(MediaType.APPLICATION_JSON);

            for (int i = 0; i < chunks.size(); i++) {
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
              restTemplate.postForEntity(endpoint, new HttpEntity<>(obj, jsonH), String.class);
            }

            Map<String,Object> docObj = Map.of(
              "class","Document",
              "id",docId,
              "properties",Map.of(
                "title",     filename,
                "pages",     chunks.size(),
                "processed", true,
                "workspace", workspace,
                "userId",    userId,
                "source",    source
              )
            );
            restTemplate.postForEntity(endpoint, new HttpEntity<>(docObj, jsonH), String.class);

        } catch (Exception e) {
            log.error("Fallback ingest failed for {}", filename, e);
        }
    }

    private List<String> chunkText(String text, int max) {
        String[] w = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < w.length; i += max) {
          chunks.add(String.join(" ",
            Arrays.copyOfRange(w, i, Math.min(i+max, w.length))));
        }
        return chunks;
    }
}
