// src/main/java/com/vectormind/api/SearchController.java
package com.vectormind.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api")
public class SearchController {

  private final RestTemplate rest = new RestTemplate();

  @Value("${openai.api.key:}")
  private String cfgKey;

  @PostMapping("/search")
  public ResponseEntity<?> search(@RequestBody Map<String,String> body) {
    try {
      String query = body.getOrDefault("query", "").trim();
      String docId = body.get("docId");
      if (query.isBlank() || docId == null) {
        return ResponseEntity
          .badRequest()
          .body(Map.of("error", "missing query or docId"));
      }

      // 1 — embed query
      HttpHeaders hJson = new HttpHeaders();
      hJson.setContentType(MediaType.APPLICATION_JSON);
      Map<String,Object> eReq = Map.of("texts", List.of(query));
      List<?> vec = (List<?>)
        ((Map<?,?>) rest.postForObject(
            "http://localhost:5001/embed",
            new HttpEntity<>(eReq, hJson),
            Map.class
          )).get("embeddings");
      List<?> vector = (List<?>) vec.get(0);

      // 2 — search in Weaviate, scoped to this docId
      String gql = """
        {
          Get {
            Chunk(
              where:{ path:["docId"], operator:Equal, valueText:"%s" }
              nearVector:{ vector:%s }
              limit:4
            ){
              text
              page
              _additional { certainty }
            }
          }
        }
      """.formatted(docId, vector.toString());

      Map<?,?> weav = rest.postForObject(
        "http://localhost:8080/v1/graphql",
        new HttpEntity<>(Map.of("query", gql), hJson),
        Map.class
      );

      List<Map<String,Object>> chunks = (List<Map<String,Object>>)
        ((Map<?,?>)((Map<?,?>) weav.get("data")).get("Get")).get("Chunk");

      // 3 — if we got no chunks, FALL BACK to a plain OpenAI call
      if (chunks == null || chunks.isEmpty()) {
        return callOpenAI(query, hJson);
      }

      // 4 — build context from chunks and call OpenAI with context
      StringBuilder ctx = new StringBuilder();
      for (Map<String,Object> c : chunks) {
        ctx.append("Page ").append(c.get("page")).append(": ")
           .append(c.get("text")).append("\n\n");
      }

      return callOpenAI("Context:\n" + ctx + "\n\nQuestion:\n" + query, hJson);

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", "backend error"));
    }
  }

  /** 
   * Helper to invoke OpenAI ChatCompletion with either pure query
   * or with context+query. Returns full answer + empty sources list.
   */
  private ResponseEntity<?> callOpenAI(String prompt, HttpHeaders hJson) {
    String key = !cfgKey.isBlank() ? cfgKey : System.getenv("OPENAI_API_KEY");
    if (key == null || key.isBlank()) {
      return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", "OpenAI key not set"));
    }

    HttpHeaders oh = new HttpHeaders();
    oh.setContentType(MediaType.APPLICATION_JSON);
    oh.setBearerAuth(key);

    // If we passed only the raw query, prompt will be the query itself.
    // Otherwise, prompt includes "Context:" + ctx + "Question:" etc.
    Map<String,Object> oaReq = Map.of(
      "model", "gpt-4o-mini",
      "messages", List.of(
        Map.of("role","system", "content",
          "You are a helpful assistant. Answer from context if provided; otherwise from your general knowledge."
        ),
        Map.of("role","user", "content", prompt)
      ),
      "temperature", 0.2,
      "max_tokens", 512
    );

    Map<?,?> oa = rest.postForObject(
      "https://api.openai.com/v1/chat/completions",
      new HttpEntity<>(oaReq, oh),
      Map.class
    );

    Map<?,?> choice = (Map<?,?>)((List<?>) oa.get("choices")).get(0);
    Map<?,?> message = (Map<?,?>) choice.get("message");
    String answer = (String) message.get("content");

    return ResponseEntity.ok(Map.of(
      "answer", answer.trim(),
      "sources", List.of()  // no PDF sources when falling back
    ));
  }
}
