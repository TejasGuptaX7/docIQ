package com.vectormind.api;

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

  @Value("${openai.api.key:}")
  private String cfgKey;

  public SearchController(RestTemplate rest) {
    this.rest = rest;
  }

  private String getUserId(Authentication auth) {
    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      return jwt.getSubject();
    }
    throw new RuntimeException("User not authenticated");
  }

  @PostMapping("/search")
  public ResponseEntity<?> search(@RequestBody Map<String, String> body, Authentication auth) {
    String userId = getUserId(auth);
    String query  = Optional.ofNullable(body.get("query")).orElse("").trim();
    String docId  = Optional.ofNullable(body.get("docId")).orElse("").trim();

    System.out.println("[SearchController] ====== SEARCH REQUEST ======");
    System.out.println("[SearchController] userId: '" + userId + "'");
    System.out.println("[SearchController] query: '" + query + "'");
    System.out.println("[SearchController] docId: '" + docId + "'");
    System.out.println("[SearchController] isGlobal: " + docId.isBlank());
    System.out.println("[SearchController] ==========================");

    if (query.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "missing query"));
    }

    // 1) Get embedding from OpenAI
    String openAiKey = cfgKey.isBlank() ? System.getenv("OPENAI_API_KEY") : cfgKey;
    if (openAiKey == null || openAiKey.isBlank()) {
      System.err.println("[SearchController] Missing OpenAI API key");
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("error", "Search unavailable – missing OpenAI API key"));
    }
    HttpHeaders embedHeaders = new HttpHeaders();
    embedHeaders.setContentType(MediaType.APPLICATION_JSON);
    embedHeaders.setBearerAuth(openAiKey);
    Map<String,Object> embedReq = Map.of(
      "model", "text-embedding-ada-002",
      "input", List.of(query)
    );

    List<Double> vector;
    try {
      @SuppressWarnings("unchecked")
      Map<String,Object> embResp = rest.postForObject(
        "https://api.openai.com/v1/embeddings",
        new HttpEntity<>(embedReq, embedHeaders),
        Map.class
      );
      @SuppressWarnings("unchecked")
      List<Map<String,Object>> data = (List<Map<String,Object>>) embResp.get("data");
      @SuppressWarnings("unchecked")
      List<Double> emb = (List<Double>) data.get(0).get("embedding");
      vector = emb;
      System.out.println("[SearchController] Got vector length: " + vector.size());
    } catch (Exception e) {
      e.printStackTrace();
      return callOpenAI(query);
    }

    // 2) Build where-clause
    String where = docId.isBlank()
      ? String.format("path:[\"userId\"],operator:Equal,valueText:\"%s\"", userId)
      : String.format(
          "operator:And,operands:["
          + "{path:[\"userId\"],operator:Equal,valueText:\"%s\"},"
          + "{path:[\"docId\"],operator:Equal,valueText:\"%s\"}]",
          userId.replace("\"","\\\""),
          docId.replace("\"","\\\"")
        );

    String gql = String.format("""
      {
        Get {
          Chunk(
            where:{%s}
            nearVector:{vector:%s}
            limit:4
          ) {
            text page docId userId _additional{certainty}
          }
        }
      }
      """, where, vector.toString());
    System.out.println("[SearchController] GraphQL query:\n" + gql);

    // 3) Query Weaviate
    String weav = System.getenv("WEAVIATE_URL");
    if (weav == null || weav.isBlank()) weav = "http://localhost:8080";
    if (!weav.startsWith("http")) weav = "https://" + weav;
    HttpHeaders gqlHeaders = new HttpHeaders();
    gqlHeaders.setContentType(MediaType.APPLICATION_JSON);
    Map<?,?> weavResp;
    try {
      weavResp = rest.postForObject(
        weav + "/v1/graphql",
        new HttpEntity<>(Map.of("query", gql), gqlHeaders),
        Map.class
      );
    } catch (Exception e) {
      e.printStackTrace();
      return callOpenAI(query);
    }

    @SuppressWarnings("unchecked")
    List<Map<String,Object>> chunks = (List<Map<String,Object>>)
      ((Map<?,?>)((Map<?,?>) weavResp.get("data")).get("Get")).get("Chunk");
    System.out.println("[SearchController] Found " + (chunks==null?0:chunks.size()) + " chunks");

    if (chunks == null || chunks.isEmpty()) {
      return callOpenAI(query);
    }

    // 4) Build context + call AI
    StringBuilder ctx = new StringBuilder();
    for (var c : chunks) {
      ctx.append("Page ").append(c.get("page")).append(": ").append(c.get("text")).append("\n\n");
    }
    return callOpenAI("Context:\n" + ctx + "\n\nQuestion:\n" + query);
  }

  private ResponseEntity<?> callOpenAI(String prompt) {
    String key = cfgKey.isBlank() ? System.getenv("OPENAI_API_KEY") : cfgKey;
    HttpHeaders hdr = new HttpHeaders();
    hdr.setContentType(MediaType.APPLICATION_JSON);
    hdr.setBearerAuth(key);
    Map<String,Object> req = Map.of(
      "model", "gpt-4o-mini",
      "messages", List.of(
        Map.of("role","system","content","You are a helpful assistant."),
        Map.of("role","user","content",prompt)
      ),
      "temperature", 0.2,
      "max_tokens", 512
    );
    try {
      @SuppressWarnings("unchecked")
      Map<?,?> resp = rest.postForObject(
        "https://api.openai.com/v1/chat/completions",
        new HttpEntity<>(req, hdr),
        Map.class
      );
      @SuppressWarnings("unchecked")
      Map<String,Object> choice = (Map<String,Object>) ((List<?>)resp.get("choices")).get(0);
      String ans = (String)((Map<?,?>)choice.get("message")).get("content");
      return ResponseEntity.ok(Map.of("answer", ans.trim(), "sources", List.of()));
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error","AI call failed: "+e.getMessage()));
    }
  }

  @GetMapping("/debug/weaviate-count")
  public ResponseEntity<?> debugWeaviate(Authentication auth) {
    String userId = getUserId(auth);
    System.out.println("[Debug] Checking Weaviate for user: " + userId);
    String gql = String.format("""
      {
        Aggregate {
          Document(where:{path:["userId"],operator:Equal,valueText:"%s"}) {
            meta{count}
            source{
              count
              topOccurrences(limit:10){value,occurs}
            }
            workspace{
              count
              topOccurrences(limit:10){value,occurs}
            }
          }
          Chunk(where:{path:["userId"],operator:Equal,valueText:"%s"}) {
            meta{count}
            docId{
              count
              topOccurrences(limit:5){value,occurs}
            }
          }
        }
      }
      """, userId.replace("\"","\\\""), userId.replace("\"","\\\""));

    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);

    String weav = System.getenv("WEAVIATE_URL");
    if (weav == null || weav.isBlank()) weav = "http://localhost:8080";
    if (!weav.startsWith("http")) weav = "https://" + weav;

    try {
      @SuppressWarnings("unchecked")
      Map<?,?> resp = rest.postForObject(
        weav + "/v1/graphql",
        new HttpEntity<>(Map.of("query", gql), h),
        Map.class
      );
      System.out.println("[Debug] Weaviate response: " + resp);
      return ResponseEntity.ok(resp);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body(Map.of("error", e.getMessage()));
    }
  }
}
