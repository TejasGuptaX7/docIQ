package com.vectormind.api;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UploadController {

  private final RestTemplate restTemplate = new RestTemplate();
  private static final int TOKENS_PER_CHUNK = 400;

  /* ---------- POST /api/upload ---------- */
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {

    try {
      String filename = Objects.requireNonNull(file.getOriginalFilename());
      String ext      = StringUtils.getFilenameExtension(filename).toLowerCase();
      String docId    = UUID.randomUUID().toString();

      /* ---- 1. Extract raw text ------------------------------------- */
      String rawText;
      if (ext.equals("pdf")) {
        try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
          rawText = new PDFTextStripper().getText(pdf);
        }
      } else if (ext.equals("txt")) {
        rawText = new String(file.getBytes(), StandardCharsets.UTF_8);
      } else {
        return ResponseEntity.badRequest().body("Unsupported type " + ext);
      }

      /* ---- 2. Chunk & embed --------------------------------------- */
      List<String> chunks = chunkText(rawText, TOKENS_PER_CHUNK);

      Map<String, Object> embedReq = Map.of("texts", chunks);
      HttpHeaders jsonHeaders = new HttpHeaders();
      jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

      @SuppressWarnings("unchecked")
      List<List<Double>> vectors = (List<List<Double>>) restTemplate
          .postForEntity("http://localhost:5001/embed",
                         new HttpEntity<>(embedReq, jsonHeaders), Map.class)
          .getBody()
          .get("embeddings");

      /* ---- 3. Store in Weaviate ----------------------------------- */
      for (int i = 0; i < chunks.size(); i++) {
        Map<String, Object> obj = Map.of(
          "class", "Chunk",
          "id", UUID.randomUUID().toString(),
          "properties", Map.of(
              "docId", docId,
              "text",  chunks.get(i),
              "page",  i + 1
          ),
          "vector", vectors.get(i)
        );
        restTemplate.postForEntity("http://localhost:8080/v1/objects",
                           new HttpEntity<>(obj, jsonHeaders), String.class);
      }

      /* ---- 4. Save metadata Document object ----------------------- */
      ResponseEntity<String> response = restTemplate.postForEntity(
        "http://localhost:8080/v1/objects",
        new HttpEntity<>(Map.of(
          "class", "Document",
          "id",    docId,
          "properties", Map.of(
              "title", filename,
              "pages", chunks.size(),
              "processed", true)
        ), jsonHeaders),
        String.class
      );
      
      System.out.println("✅ Document Save Response: " + response.getBody());
      

      /* ---- 5. Return payload to UI ------------------------------- */
      int wordCount = rawText.split("\\s+").length;
      return ResponseEntity.ok(Map.of(
          "docId",   docId,
          "chunks",  chunks.size(),
          "words",   wordCount,
          "name",    filename
      ));

    } catch (Exception e) {
      return ResponseEntity.status(500).body(e.getMessage());
    }
  }

  /* ---------- GET /api/documents for sidebar ---------- */
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

    HttpHeaders jsonHeaders = new HttpHeaders();
    jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

    Map<?, ?> res = restTemplate.postForObject(
        "http://localhost:8080/v1/graphql",
        new HttpEntity<>(Map.of("query", gql), jsonHeaders), 
        Map.class);

    Map<?, ?> data = (Map<?, ?>) res.get("data");
    Map<?, ?> get  = (Map<?, ?>) data.get("Get");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> docs = (List<Map<String, Object>>) get.get("Document");

    return docs;
  }

  /* ---------- helper ---------- */
  private List<String> chunkText(String text, int maxTokens) {
    String[] words = text.split("\\s+");
    List<String> out = new ArrayList<>();
    for (int i = 0; i < words.length; i += maxTokens) {
      out.add(Arrays.stream(words, i, Math.min(i + maxTokens, words.length))
                    .collect(Collectors.joining(" ")));
    }
    return out;
  }
}