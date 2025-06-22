package com.vectormind.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
public class SearchController {

    private final RestTemplate restTemplate = new RestTemplate();

    /** Inject from application.properties (openai.api.key) or --openai.api.key */
    @Value("${openai.api.key:}")
    private String configuredOpenAiKey;   // empty string if not set

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody Map<String, String> payload) {

        try {
            String query = payload.get("query");
            System.out.println("Received query: " + query);

            /* ---------- 1) Embed query ------------------------------------ */
            String embedderUrl = "http://localhost:5001/embed";
            Map<String, Object> embedReq = Map.of("texts", List.of(query));

            HttpHeaders json = new HttpHeaders();
            json.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> embedRes = restTemplate.postForEntity(
                    embedderUrl, new HttpEntity<>(embedReq, json), Map.class);

            if (!embedRes.getStatusCode().is2xxSuccessful())
                return mock("Embedder failed: " + embedRes.getStatusCode());

            List<List<Double>> vecs =
                    (List<List<Double>>) embedRes.getBody().get("embeddings");
            if (vecs == null || vecs.isEmpty())
                return mock("No embeddings");

            /* ---------- 2) Weaviate search -------------------------------- */
            List<Double> vector = vecs.get(0);
            String weaviateUrl = "http://localhost:8080/v1/graphql";
            String graphqlQuery = "{ Get { Document(nearVector:{vector:" + vector + ",certainty:0.3},limit:5){ text } } }";

            Map<String, String> graphqlBody = new HashMap<>();
            graphqlBody.put("query", graphqlQuery);

            ResponseEntity<Map> wv = restTemplate.postForEntity(
                    weaviateUrl,
                    new HttpEntity<>(graphqlBody, json), Map.class);

            List<Map<String, Object>> docs =
                (List<Map<String, Object>>)
                    ((Map) ((Map) wv.getBody().get("data"))
                           .get("Get")).get("Document");

            if (docs == null || docs.isEmpty())
                return mock("No context");

            StringBuilder context = new StringBuilder();
            docs.forEach(d -> context.append(d.get("text")).append('\n'));

            /* ---------- 3) OpenAI call ------------------------------------ */
            String openaiKey = !configuredOpenAiKey.isBlank()
                               ? configuredOpenAiKey
                               : System.getenv("OPENAI_API_KEY");

            if (openaiKey == null || openaiKey.isBlank())
                return mock("Missing OpenAI key");

            HttpHeaders oaHead = new HttpHeaders();
            oaHead.setContentType(MediaType.APPLICATION_JSON);
            oaHead.setBearerAuth(openaiKey);

            Map<String, Object> oaReq = Map.of(
                    "model", "gpt-3.5-turbo",
                    "messages", List.of(
                            Map.of("role", "system",
                                   "content", "Answer strictly from context."),
                            Map.of("role", "user",
                                   "content", "Context:\n" + context +
                                              "\n\nQuestion:\n" + query)
                    ),
                    "max_tokens", 256,
                    "temperature", 0.7
            );

            ResponseEntity<Map> oaRes = restTemplate.postForEntity(
                    "https://api.openai.com/v1/chat/completions",
                    new HttpEntity<>(oaReq, oaHead), Map.class);

            System.out.println("OpenAI status: " + oaRes.getStatusCode());

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) oaRes.getBody().get("choices");
            if (choices == null || choices.isEmpty())
                return mock("Empty choices");

            String answer =
                (String) ((Map) choices.get(0).get("message")).get("content");

            return ResponseEntity.ok(Map.of("answer", answer.trim()));

        } catch (Exception e) {
            e.printStackTrace();
            return mock("Exception: " + e.getMessage());
        }
    }

    /* --------------------------- helper ---------------------------------- */
    private ResponseEntity<Map<String, String>> mock(String reason) {
        System.out.println("Falling back to mock — " + reason);
        return ResponseEntity.ok(
                Map.of("answer",
                       "Based on your query, here's a mock answer:")
        );
    }
}
