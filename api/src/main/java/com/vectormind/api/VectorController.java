package com.vectormind.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
public class VectorController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/store")
    public ResponseEntity<?> storeVector(@RequestBody Map<String, String> payload) {
        String text = payload.get("text");

        // Send to embedder
        Map<String, Object> request = new HashMap<>();
        request.put("texts", List.of(text));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:5001/embed", entity, Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(500).body("Embedding failed");
        }

        List<List<Double>> embeddings = (List<List<Double>>) response.getBody().get("embeddings");
        List<Double> vector = embeddings.get(0);

        // Store in Weaviate
        Map<String, Object> weaviatePayload = Map.of(
            "class", "Document",
            "properties", Map.of("text", text),
            "vector", vector
        );

        HttpEntity<Map<String, Object>> weaviateEntity = new HttpEntity<>(weaviatePayload, headers);
        restTemplate.postForEntity("http://localhost:8080/v1/objects", weaviateEntity, String.class);

        return ResponseEntity.ok("Stored in Weaviate");
    }

    // Removed the /search endpoint to avoid mapping conflict with SearchController
}
