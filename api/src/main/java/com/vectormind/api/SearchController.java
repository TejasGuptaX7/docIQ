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

    System.out.println("[SearchController] user=" + userId + " q=" + query + " doc=" + docId);

    if (query.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error","missing query"));
    }

    // 1) get embedding from OpenAI
    String openAiKey = cfgKey.isBlank() ? System.getenv("OPENAI_API_KEY") : cfgKey;
    if (openAiKey == null || openAiKey.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("error","Search unavailable – missing OpenAI key"));
    }
    HttpHeaders embH = new HttpHeaders();
    embH.setContentType(MediaType.APPLICATION_JSON);
    embH.setBearerAuth(openAiKey);
    Map<String,Object> embReq = Map.of(
      "model","text-embedding-ada-002",
      "input", List.of(query)
    );

    List<Double> vector;
    try {
      @SuppressWarnings("unchecked")
      Map<String,Object> embResp = rest.postForObject(
        "https://api.openai.com/v1/embeddings",
        new HttpEntity<>(embReq,embH),
        Map.class
      );
      @SuppressWarnings("unchecked")
      List<Map<String,Object>> data = (List<Map<String,Object>>) embResp.get("data");
      @SuppressWarnings("unchecked")
      List<Double> v = (List<Double>) data.get(0).get("embedding");
      vector = v;
    } catch (Exception e) {
      e.printStackTrace();
      return callOpenAI(query);
    }

    // 2) build GraphQL query
    String where = docId.isBlank()
      ? String.format("path:[\"userId\"],operator:Equal,valueText:\"%s\"", userId)
      : String.format(
          "operator:And,operands:["
          +"{path:[\"userId\"],operator:Equal,valueText:\"%s\"},"
          +"{path:[\"docId\"],operator:Equal,valueText:\"%s\"}]",
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
    System.out.println("[SearchController] GraphQL:\n" + gql);

    // 3) call Weaviate
    String weav = System.getenv("WEAVIATE_URL");
    if (weav == null || weav.isBlank()) weav = "http://localhost:8080";
    if (!weav.startsWith("http")) weav = "https://" + weav;
    HttpHeaders gqlH = new HttpHeaders();
    gqlH.setContentType(MediaType.APPLICATION_JSON);
    String weavKey = System.getenv("WEAVIATE_API_KEY");
    if (weavKey != null && !weavKey.isBlank()) gqlH.set("X-API-KEY", weavKey);

    Map<?,?> weavResp;
    try {
      weavResp = rest.postForObject(
        weav + "/v1/graphql",
        new HttpEntity<>(Map.of("query",gql),gqlH),
        Map.class
      );
    } catch (Exception e) {
      e.printStackTrace();
      return callOpenAI(query);
    }

    @SuppressWarnings("unchecked")
    List<Map<String,Object>> chunks = (List<Map<String,Object>>)
      ((Map<?,?>)((Map<?,?>)weavResp.get("data")).get("Get")).get("Chunk");

    if (chunks == null || chunks.isEmpty()) {
      return callOpenAI(query);
    }

    // 4) build context + call AI
    StringBuilder ctx = new StringBuilder();
    for (var c : chunks) {
      ctx.append("Page ").append(c.get("page"))
         .append(": ").append(c.get("text")).append("\n\n");
    }
    return callOpenAI("Context:\n" + ctx + "\n\nQuestion:\n" + query);
  }

  private ResponseEntity<?> callOpenAI(String prompt) {
    String key = cfgKey.isBlank() ? System.getenv("OPENAI_API_KEY") : cfgKey;
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
        new HttpEntity<>(req,h),
        Map.class
      );
      @SuppressWarnings("unchecked")
      Map<String,Object> choice = (Map<String,Object>)((List<?>)resp.get("choices")).get(0);
      String ans = (String)((Map<?,?>)choice.get("message")).get("content");
      return ResponseEntity.ok(Map.of("answer",ans.trim(),"sources",List.of()));
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error","AI call failed"));
    }
  }

  @GetMapping("/debug/weaviate-count")
  public ResponseEntity<?> debugWeaviate(Authentication auth) {
    String userId = getUserId(auth);
    String gql = String.format("""
      {
        Aggregate {
          Document(where:{path:["userId"],operator:Equal,valueText:"%s"}) {
            meta{count}
          }
        }
      }
      """, userId.replace("\"","\\\""));
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    String weav = System.getenv("WEAVIATE_URL");
    if (weav == null||weav.isBlank()) weav="http://localhost:8080";
    if (!weav.startsWith("http")) weav="https://"+weav;
    String weavKey = System.getenv("WEAVIATE_API_KEY");
    if (weavKey!=null&&!weavKey.isBlank()) h.set("X-API-KEY",weavKey);

    try {
      @SuppressWarnings("unchecked")
      Map<?,?> resp = rest.postForObject(
        weav+"/v1/graphql",
        new HttpEntity<>(Map.of("query",gql),h),
        Map.class
      );
      return ResponseEntity.ok(resp);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error",e.getMessage()));
    }
  }
}
