package com.vectormind.api;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UploadController {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final int TOKENS_PER_CHUNK = 400;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "workspace", required = false) String workspace) {
        try {
            String filename = Objects.requireNonNull(file.getOriginalFilename());
            String ext = StringUtils.getFilenameExtension(filename).toLowerCase();
            String docId = UUID.randomUUID().toString();
            workspace = (workspace != null && !workspace.isBlank()) ? workspace.trim() : "default";

            String rawText;
            if (ext.equals("pdf")) {
                try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
                    rawText = new PDFTextStripper().getText(pdf);
                }
            } else if (ext.equals("txt")) {
                rawText = new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                return ResponseEntity.badRequest().body("Unsupported file type: " + ext);
            }

            Path path = Paths.get("uploads", docId + ".pdf");
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            ingestText(rawText, filename, docId, workspace);
            int wordCount = rawText.split("\\s+").length;
            return ResponseEntity.ok(Map.of(
                    "docId", docId,
                    "chunks", chunkText(rawText, TOKENS_PER_CHUNK).size(),
                    "words", wordCount,
                    "name", filename
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/upload/external")
    public ResponseEntity<Void> saveExternal(@RequestBody Map<String, String> body) throws Exception {
        String url = body.get("url");
        String name = body.get("name");
        String workspace = body.getOrDefault("workspace", "default");
        if (url == null || url.isBlank()) return ResponseEntity.badRequest().build();

        byte[] bytes = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
                .body();

        String docId = UUID.randomUUID().toString();
        Path path = Paths.get("uploads", docId + ".pdf");
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);

        try (PDDocument pdf = PDDocument.load(bytes)) {
            String rawText = new PDFTextStripper().getText(pdf);
            ingestText(rawText, name != null ? name : url, docId, workspace);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/documents")
    public List<Map<String, Object>> listDocs() {
        String gql = """
            {
              Get {
                Document {
                  _additional { id }
                  title
                  processed
                  pages
                }
              }
            }
        """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<?, ?> body = Map.of("query", gql);

        var res = restTemplate.postForObject(
                "http://localhost:8080/v1/graphql",
                new HttpEntity<>(body, headers),
                Map.class);

        Map<?, ?> data = (Map<?, ?>) res.get("data");
        Map<?, ?> get = (Map<?, ?>) data.get("Get");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) get.get("Document");
        return docs;
    }

    private void ingestText(String rawText, String filename, String docId, String workspace) {
        List<String> chunks = chunkText(rawText, TOKENS_PER_CHUNK);
        Map<String, Object> embedReq = Map.of("texts", chunks);

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

        @SuppressWarnings("unchecked")
        List<List<Double>> vectors = (List<List<Double>>) restTemplate
                .postForEntity("http://localhost:5001/embed", new HttpEntity<>(embedReq, jsonHeaders), Map.class)
                .getBody().get("embeddings");

        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> obj = Map.of(
                    "class", "Chunk",
                    "id", UUID.randomUUID().toString(),
                    "properties", Map.of(
                            "docId", docId,
                            "text", chunks.get(i),
                            "page", i + 1
                    ),
                    "vector", vectors.get(i)
            );
            restTemplate.postForEntity("http://localhost:8080/v1/objects",
                    new HttpEntity<>(obj, jsonHeaders), String.class);
        }

        restTemplate.postForEntity("http://localhost:8080/v1/objects",
                new HttpEntity<>(Map.of(
                        "class", "Document",
                        "id", docId,
                        "properties", Map.of(
                                "title", filename,
                                "pages", chunks.size(),
                                "processed", true,
                                "workspace", workspace
                        )
                ), jsonHeaders), String.class);
    }

    private List<String> chunkText(String text, int maxTokens) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < words.length; i += maxTokens) {
            chunks.add(String.join(" ", Arrays.copyOfRange(words, i, Math.min(i + maxTokens, words.length))));
        }
        return chunks;
    }
}
