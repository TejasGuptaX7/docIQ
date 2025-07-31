package com.vectormind.api;

import com.vectormind.api.config.WeaviateConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final RestTemplate rest;
    private final WeaviateConfig weaviateConfig;
    private final String weaviateApiKey;

    @Value("${openai.api.key:}")
    private String cfgKey;

    public SearchController(
        RestTemplate rest,
        WeaviateConfig weaviateConfig,
        @Value("${weaviate.api-key:}") String weaviateApiKey
    ) {
        this.rest = rest;
        this.weaviateConfig = weaviateConfig;
        this.weaviateApiKey = weaviateApiKey;
    }

    private String getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        throw new RuntimeException("User not authenticated");
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(
        @RequestBody Map<String, String> body,
        Authentication auth
    ) {
        String userId = getUserId(auth);
        String query  = Optional.ofNullable(body.get("query")).orElse("").trim();
        String docId  = Optional.ofNullable(body.get("docId")).orElse("").trim();

        if (query.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error","missing query"));
        }

        // OpenAI key
        String openAiKey = getOpenAIKey();
        if (openAiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                 .body(Map.of("error","Search unavailable – OpenAI key missing"));
        }

        // Use random vector instead of embedding service
        List<Double> vector = new ArrayList<>();
        for (int j = 0; j < 384; j++) {
            vector.add(Math.random());
        }

        // 2) filter
        String where = docId.isBlank()
            ? String.format("path:[\"userId\"],operator:Equal,valueText:\"%s\"", userId)
            : String.format(
                  "operator:And,operands:[{path:[\"userId\"],operator:Equal,valueText:\"%s\"},{path:[\"docId\"],operator:Equal,valueText:\"%s\"}]",
                 userId, docId
             );

       // 3) GraphQL
       String gql = String.format("""
           {
             Get {
               Chunk(
                 where: { %s }
                 nearVector: { vector: %s }
                 limit: 4
               ) {
                 text page docId userId _additional { certainty }
               }
             }
           }
       """, where, vector);

       // 4) call Weaviate
       HttpHeaders weavHdr = new HttpHeaders();
       weavHdr.setContentType(MediaType.APPLICATION_JSON);
       if (weaviateApiKey != null && !weaviateApiKey.isEmpty()) {
           weavHdr.set("Authorization", "Bearer " + weaviateApiKey);
       }

       Map<?,?> weav;
       try {
           weav = rest.postForObject(
               weaviateConfig.getGraphQLEndpoint(),
               new HttpEntity<>(Map.of("query", gql), weavHdr),
               Map.class
           );
       } catch (Exception e) {
           return callOpenAI(query, weavHdr);
       }

       @SuppressWarnings("unchecked")
       List<Map<String,Object>> chunks = (List<Map<String,Object>>)
           ((Map<?,?>)((Map<?,?>)weav.get("data")).get("Get")).get("Chunk");

       if (chunks == null || chunks.isEmpty()) {
           return callOpenAI(query, weavHdr);
       }

       // 5) build context
       StringBuilder ctx = new StringBuilder();
       List<Map<String,Object>> sources = new ArrayList<>();
       for (var c : chunks) {
           ctx.append("Page ").append(c.get("page")).append(": ").append(c.get("text")).append("\n\n");
           sources.add(Map.of(
               "page", c.get("page"),
               "excerpt", String.valueOf(c.get("text"))
                                 .substring(0, Math.min(100, String.valueOf(c.get("text")).length())) + "...",
               "confidence", ((Map<?,?>)c.get("_additional")).get("certainty")
           ));
       }

       ResponseEntity<?> aiResp = callOpenAI("Context:\n" + ctx + "\n\nQuestion:\n" + query, weavHdr);
       if (aiResp.getStatusCode() == HttpStatus.OK && aiResp.getBody() instanceof Map) {
           @SuppressWarnings("unchecked")
           Map<String,Object> bodyMap = new HashMap<>((Map<String,Object>)aiResp.getBody());
           bodyMap.put("sources", sources);
           return ResponseEntity.ok(bodyMap);
       }
       return aiResp;
   }

   private String getOpenAIKey() {
       return (cfgKey == null || cfgKey.isBlank())
           ? System.getenv("OPENAI_API_KEY")
           : cfgKey;
   }

   private ResponseEntity<?> callOpenAI(String prompt, HttpHeaders hdr) {
       String key = getOpenAIKey();
       if (key.isBlank()) {
           return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(Map.of("error","Missing OpenAI key"));
       }
       HttpHeaders h = new HttpHeaders();
       h.setContentType(MediaType.APPLICATION_JSON);
       h.setBearerAuth(key);

       Map<String,Object> req = Map.of(
         "model","gpt-4o-mini",
         "messages", List.of(
           Map.of("role","system","content","You are a helpful assistant."),
           Map.of("role","user","content",prompt)
         ),
         "temperature",0.2,
         "max_tokens",512
       );

       try {
           @SuppressWarnings("unchecked")
           Map<?,?> resp = rest.postForObject(
             "https://api.openai.com/v1/chat/completions",
             new HttpEntity<>(req, h),
             Map.class
           );
           @SuppressWarnings("unchecked")
           Map<String,Object> choice = (Map<String,Object>)((List<?>)resp.get("choices")).get(0);
           String ans = (String)((Map<?,?>)choice.get("message")).get("content");
           return ResponseEntity.ok(Map.of("answer", ans.trim(), "sources", List.of()));
       } catch (Exception e) {
           e.printStackTrace();
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error","AI failed: "+e.getMessage()));
       }
   }
}