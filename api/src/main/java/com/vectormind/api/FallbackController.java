package com.vectormind.api;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/fallback")
public class FallbackController {

    private final RestTemplate restTemplate;

    private static final int TOKENS_PER_CHUNK = 400;
    private static final String DEMO_USER = "demo-user";

    public FallbackController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/drive/status")
    public ResponseEntity<Map<String,Boolean>> driveStatus() {
        // your existing fallback logic…
        return ResponseEntity.ok(Map.of("hasToken",false));
    }

    @PostMapping(value="/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value="workspace",defaultValue="default") String workspace
    ) {
        String userId = DEMO_USER;
        String filename = Objects.requireNonNull(file.getOriginalFilename());
        String ext = Optional.ofNullable(StringUtils.getFilenameExtension(filename))
                             .orElse("").toLowerCase();
        String docId = UUID.randomUUID().toString();

        try {
            String rawText;
            if ("pdf".equals(ext)) {
                try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
                    rawText = new PDFTextStripper().getText(pdf);
                }
            } else if ("txt".equals(ext)) {
                rawText = new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                return ResponseEntity.badRequest().body("Unsupported file: " + ext);
            }

            Path out = Paths.get("uploads", docId + ".pdf");
            Files.createDirectories(out.getParent());
            Files.copy(file.getInputStream(), out, StandardCopyOption.REPLACE_EXISTING);

            ingestText(rawText, filename, docId, workspace, userId, "upload");

            return ResponseEntity.ok(Map.of(
                "docId",  docId,
                "name",   filename,
                "words",  rawText.split("\\s+").length,
                "chunks", chunkText(rawText,TOKENS_PER_CHUNK).size()
            ));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Fallback upload failed: " + e.getMessage());
        }
    }

    private void ingestText(String rawText,
                            String filename,
                            String docId,
                            String workspace,
                            String userId,
                            String source) {
        try {
            // chunk
            List<String> chunks = chunkText(rawText, TOKENS_PER_CHUNK);

            // OpenAI embeddings
            String openAiKey = System.getenv("OPENAI_API_KEY");
            HttpHeaders embH = new HttpHeaders();
            embH.setContentType(MediaType.APPLICATION_JSON);
            embH.setBearerAuth(openAiKey);
            Map<String,Object> embReq = Map.of("model","text-embedding-ada-002","input",chunks);
            @SuppressWarnings("unchecked")
            Map<?,?> embResp = restTemplate.postForObject(
              "https://api.openai.com/v1/embeddings",
              new HttpEntity<>(embReq,embH),
              Map.class
            );
            @SuppressWarnings("unchecked")
            List<Map<String,Object>> data = (List<Map<String,Object>>)embResp.get("data");
            List<List<Double>> vectors = new ArrayList<>();
            for (var item : data) {
                @SuppressWarnings("unchecked")
                List<Double> v = (List<Double>) item.get("embedding");
                vectors.add(v);
            }

            // Weaviate ingest
            String endpoint = System.getenv("WEAVIATE_URL").replaceAll("/$","") + "/v1/objects";
            HttpHeaders jsonH = new HttpHeaders();
            jsonH.setContentType(MediaType.APPLICATION_JSON);
            String weavKey = System.getenv("WEAVIATE_API_KEY");
            if (weavKey != null && !weavKey.isBlank()) {
                jsonH.set("X-API-KEY", weavKey);
            }

            // store chunks & document (same as UploadController)…
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
                restTemplate.postForEntity(endpoint, new HttpEntity<>(obj,jsonH), String.class);
            }
            Map<String,Object> docObj = Map.of(
              "class","Document",
              "id",docId,
              "properties",Map.of(
                "title",   filename,
                "pages",   chunks.size(),
                "processed",true,
                "workspace",workspace,
                "userId",  userId,
                "source",  source
              )
            );
            restTemplate.postForEntity(endpoint, new HttpEntity<>(docObj,jsonH), String.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> chunkText(String text, int max) {
        String[] w = text.split("\\s+");
        List<String> out = new ArrayList<>();
        for (int i = 0; i < w.length; i += max) {
            out.add(String.join(" ", Arrays.copyOfRange(w, i,
                       Math.min(i + max, w.length))));
        }
        return out;
    }
}
