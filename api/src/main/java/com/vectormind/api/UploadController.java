package com.vectormind.api;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles file upload → chunk → embed → store.
 * Supports .pdf and .txt (add more types as needed).
 */
@RestController
public class UploadController {
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    private final RestTemplate restTemplate = new RestTemplate();

    /* ---------- Public endpoint ---------- */
    @PostMapping("/upload")
    public ResponseEntity<?> handleUpload(@RequestParam("file") MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            if (filename == null) {
                logger.error("No filename provided");
                return ResponseEntity.badRequest().body("No file name");
            }

            logger.info("Received file: {}", filename);
            String extension = StringUtils.getFilenameExtension(filename).toLowerCase();
            logger.info("File extension: {}", extension);

            List<String> textsToEmbed = new ArrayList<>();
            String rawText = "";

            if (extension.equals("pdf")) {
                try (PDDocument doc = PDDocument.load(file.getInputStream())) {
                    rawText = new PDFTextStripper().getText(doc);
                    textsToEmbed = chunkText(rawText, 400);
                    logger.info("Extracted {} chunks from PDF", textsToEmbed.size());
                }
            } else if (extension.equals("txt")) {
                rawText = new String(file.getBytes(), StandardCharsets.UTF_8);
                textsToEmbed = chunkText(rawText, 400);
                logger.info("Extracted {} chunks from text file", textsToEmbed.size());
            } else if (extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg")) {
                String base64 = Base64.getEncoder().encodeToString(file.getBytes());
                textsToEmbed.add(base64);
                logger.info("Converted image to base64");
            } else {
                logger.error("Unsupported file type: {}", extension);
                return ResponseEntity.badRequest().body("Unsupported file type");
            }

            Map<String, Object> request = new HashMap<>();
            request.put("texts", textsToEmbed);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            logger.info("Sending {} chunks to embedder", textsToEmbed.size());
            ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:5001/embed", entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Embedding failed with status: {}", response.getStatusCode());
                return ResponseEntity.status(500).body("Embedding failed");
            }

            List<List<Double>> embeddings = (List<List<Double>>) response.getBody().get("embeddings");
            logger.info("Received {} embeddings from embedder", embeddings.size());

            for (int i = 0; i < textsToEmbed.size(); i++) {
                Map<String, Object> obj = Map.of(
                    "class", "Document",
                    "properties", Map.of("text", textsToEmbed.get(i)),
                    "vector", embeddings.get(i)
                );
                HttpEntity<Map<String, Object>> weaviateEntity = new HttpEntity<>(obj, headers);
                ResponseEntity<String> weaviateResponse = restTemplate.postForEntity(
                    "http://localhost:8080/v1/objects", 
                    weaviateEntity, 
                    String.class
                );
                if (!weaviateResponse.getStatusCode().is2xxSuccessful()) {
                    logger.error("Failed to store chunk {} in Weaviate", i);
                }
            }

            logger.info("Successfully processed and stored file: {}", filename);
            
            // Calculate word count
            int wordCount = rawText.split("\\s+").length;
            
            // Create a terminal-friendly response
            String responseString = String.format("""
                Successfully processed %s
                Divided into %d chunks
                Total words: %d
                Words per chunk: %d
                """, 
                filename,
                textsToEmbed.size(),
                wordCount,
                400
            );
            
            return ResponseEntity.ok(responseString);

        } catch (Exception e) {
            logger.error("Error processing file: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /* ---------- Helper: word-based chunker ---------- */
    private List<String> chunkText(String text, int maxTokens) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < words.length; i += maxTokens) {
            chunks.add(Arrays.stream(words, i, Math.min(i + maxTokens, words.length))
                    .collect(Collectors.joining(" ")));
        }
        return chunks;
    }
    
}
