package com.vectormind.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
public class SearchController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody Map<String, String> payload) {
        try {
            String query = payload.get("query");

            // STEP 1: Embed the query via Flask
            String embedderUrl = "http://localhost:5001/embed";
            Map<String, Object> request = new HashMap<>();
            request.put("texts", List.of(query));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> embedResponse = restTemplate.postForEntity(embedderUrl, entity, Map.class);

            List<List<Double>> embeddings = (List<List<Double>>) embedResponse.getBody().get("embeddings");
            List<Double> queryVector = embeddings.get(0);

            // STEP 2: Search Weaviate with vector
            String weaviateUrl = "http://localhost:8080/v1/graphql";
            String graphqlQuery = "{\n" +
                "  Get {\n" +
                "    Document(\n" +
                "      nearVector: {\n" +
                "        vector: " + queryVector.toString() + ",\n" +
                "        certainty: 0.7\n" +
                "      },\n" +
                "      limit: 3\n" +
                "    ) {\n" +
                "      text\n" +
                "    }\n" +
                "  }\n" +
                "}";

            Map<String, String> graphqlBody = new HashMap<>();
            graphqlBody.put("query", graphqlQuery);

            HttpEntity<Map<String, String>> weaviateRequest = new HttpEntity<>(graphqlBody, headers);
            ResponseEntity<Map> weaviateResponse = restTemplate.postForEntity(weaviateUrl, weaviateRequest, Map.class);

            List<Map<String, Object>> docs = (List<Map<String, Object>>)
                ((Map<String, Object>) ((Map<String, Object>) weaviateResponse.getBody().get("data")).get("Get")).get("Document");

            // STEP 3: Concatenate top texts for LLM
            StringBuilder context = new StringBuilder();
            for (Map<String, Object> doc : docs) {
                context.append(doc.get("text")).append("\n");
            }

            // STEP 4: Send to AWS Bedrock (Mocked here)
            // Replace with your Bedrock logic if integrated
            String answer = "Based on your query, here's a mock answer:\n\n" + context.toString();

            Map<String, String> response = new HashMap<>();
            response.put("answer", answer);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
