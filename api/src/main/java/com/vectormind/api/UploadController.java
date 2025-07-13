// ✅ UploadController.java (Spring Boot backend)
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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UploadController {

  private final RestTemplate restTemplate = new RestTemplate();
  private static final int TOKENS_PER_CHUNK = 400;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
    try {
      String filename = Objects.requireNonNull(file.getOriginalFilename());
      String ext = StringUtils.getFilenameExtension(filename).toLowerCase();
      String docId = UUID.randomUUID().toString();

      // Extract raw text
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

      // Save PDF file to /uploads/{docId}.pdf
      Path path = Paths.get("uploads", docId + ".pdf");
      Files.createDirectories(path.getParent());
      Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

      // Chunk & embed
      List<String> chunks = chunkText(rawText, TOKENS_PER_CHUNK);
      Map<String, Object> embedReq = Map.of("texts", chunks);
      HttpHeaders jsonHeaders = new HttpHeaders();
      jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

      @SuppressWarnings("unchecked")
      List<List<Double>> vectors = (List<List<Double>>) restTemplate
        .postForEntity("http://localhost:5001/embed", new HttpEntity<>(embedReq, jsonHeaders), Map.class)
        .getBody().get("embeddings");

      // Store chunks in Weaviate
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
        restTemplate.postForEntity("http://localhost:8080/v1/objects", new HttpEntity<>(obj, jsonHeaders), String.class);
      }

      // Save metadata Document object
      restTemplate.postForEntity("http://localhost:8080/v1/objects",
        new HttpEntity<>(Map.of(
          "class", "Document",
          "id", docId,
          "properties", Map.of(
            "title", filename,
            "pages", chunks.size(),
            "processed", true
          )
        ), jsonHeaders), String.class);

      int wordCount = rawText.split("\\s+").length;
      return ResponseEntity.ok(Map.of(
        "docId", docId,
        "chunks", chunks.size(),
        "words", wordCount,
        "name", filename
      ));

    } catch (Exception e) {
      return ResponseEntity.status(500).body(e.getMessage());
    }
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
    var res  = restTemplate.postForObject(
        "http://localhost:8080/v1/graphql",
        new HttpEntity<>(body, headers),
        Map.class);
  
    // unwrap → return JUST the array
    Map<?, ?> data = (Map<?, ?>) res.get("data");
    Map<?, ?> get  = (Map<?, ?>) data.get("Get");
  
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> docs = (List<Map<String, Object>>) get.get("Document");
    return docs;              
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