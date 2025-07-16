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
  public ResponseEntity<?> search(@RequestBody Map<String, String> body) {
    try {
      // 1 — Read and trim inputs, never null
      String query = Optional.ofNullable(body.get("query")).orElse("").trim();
      String docId  = Optional.ofNullable(body.get("docId")).orElse("").trim();

      // —— Logging for debugging ——
      System.out.println("[SearchController] incoming query=\"" + query + "\" docId=\"" + docId + "\"");

      // 2 — Validate inputs
      if (query.isBlank() || docId.isBlank()) {
        return ResponseEntity
          .badRequest()
          .body(Map.of("error", "missing query or docId"));
      }

      // 3 — Embed the query
      HttpHeaders hJson = new HttpHeaders();
      hJson.setContentType(MediaType.APPLICATION_JSON);
      Map<String, Object> eReq = Map.of("texts", List.of(query));
      List<?> vec = (List<?>)
        ((Map<?, ?>) rest.postForObject(
          "http://localhost:5001/embed",
          new HttpEntity<>(eReq, hJson),
          Map.class
        )).get("embeddings");
      List<?> vector = (List<?>) vec.get(0);

      // 4 — Build GraphQL query, scoped to this docId
      String gql =
        "{"
      + " Get {"
      + "   Chunk("
      + "     where: { path: [\"docId\"], operator: Equal, valueText: \"" 
                    + docId.replace("\"","\\\"") 
                + "\" }"
      + "     nearVector: { vector: " + vector.toString() + " }"
      + "     limit: 4"
      + "   ) {"
      + "     text"
      + "     page"
      + "     _additional { certainty }"
      + "   }"
      + " }"
      + "}";

      System.out.println("[SearchController] GraphQL payload: " + gql);

      Map<?, ?> weav = rest.postForObject(
        "http://localhost:8080/v1/graphql",
        new HttpEntity<>(Map.of("query", gql), hJson),
        Map.class
      );

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> chunks = (List<Map<String, Object>>)
        ((Map<?, ?>) ((Map<?, ?>) weav.get("data")).get("Get")).get("Chunk");

      // 5 — Fallback if no chunks found
      if (chunks == null || chunks.isEmpty()) {
        return callOpenAI(query, hJson);
      }

      // 6 — Build context and call OpenAI
      StringBuilder ctx = new StringBuilder();
      for (Map<String, Object> c : chunks) {
        ctx.append("Page ")
           .append(c.get("page"))
           .append(": ")
           .append(c.get("text"))
           .append("\n\n");
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

    Map<String, Object> oaReq = Map.of(
      "model", "gpt-4o-mini",
      "messages", List.of(
        Map.of(
          "role", "system",
          "content",
          "You are a helpful assistant. Answer from context if provided; otherwise from your general knowledge."
        ),
        Map.of("role", "user", "content", prompt)
      ),
      "temperature", 0.2,
      "max_tokens", 512
    );

    Map<?, ?> oa = rest.postForObject(
      "https://api.openai.com/v1/chat/completions",
      new HttpEntity<>(oaReq, oh),
      Map.class
    );

    @SuppressWarnings("unchecked")
    Map<String, Object> choice = (Map<String, Object>) ((List<?>) oa.get("choices")).get(0);
    @SuppressWarnings("unchecked")
    Map<String, Object> message = (Map<String, Object>) choice.get("message");
    String answer = (String) message.get("content");

    return ResponseEntity.ok(Map.of(
      "answer", answer.trim(),
      "sources", List.of()  // no PDF sources when falling back
    ));
  }
}
