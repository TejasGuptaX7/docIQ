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
  private final WeaviateConfig weavConfig;

  @Value("${openai.api.key:}")
  private String cfgKey;

  public SearchController(RestTemplate rest, WeaviateConfig weavConfig) {
    this.rest       = rest;
    this.weavConfig = weavConfig;
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

    if (query.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "missing query"));
    }

    // 1) Embedding
    String openAiKey = cfgKey.isBlank() ? System.getenv("OPENAI_API_KEY") : cfgKey;
    if (openAiKey == null || openAiKey.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                           .body(Map.of("error","Search service unavailable. OpenAI key not configured."));
    }

    HttpHeaders embH = new HttpHeaders();
    embH.setContentType(MediaType.APPLICATION_JSON);
    embH.setBearerAuth(openAiKey);

    Map<String,Object> embReq = Map.of("model","text-embedding-ada-002","input", List.of(query));
    List<Double> vector;
    try {
      @SuppressWarnings("unchecked")
      Map<?,?> embResp = rest.postForObject(
        "https://api.openai.com/v1/embeddings",
        new HttpEntity<>(embReq, embH),
        Map.class
      );
      @SuppressWarnings("unchecked")
      List<Map<String,Object>> data = (List<Map<String,Object>>) embResp.get("data");
      vector = (List<Double>) data.get(0).get("embedding");
    } catch (Exception e) {
      return fallbackToOpenAI(query);
    }

    // 2) Build GraphQL
    String where = docId.isBlank()
        ? String.format("path:[\"userId\"],operator:Equal,valueText:\"%s\"", userId)
        : String.format(
            "operator:And,operands:[{path:[\"userId\"],operator:Equal,valueText:\"%s\"}," +
            "{path:[\"docId\"],operator:Equal,valueText:\"%s\"}]",
            userId, docId
          );

    String gql = String.format("""
      {
        Get { Chunk(
          where:{ %s }
          nearVector:{ vector:%s }
          limit:4
        ) {
          text page docId userId _additional{certainty}
        }}
      }""",
      where, vector.toString()
    );

    // 3) Call Weaviate
    HttpHeaders gqlH = new HttpHeaders();
    gqlH.setContentType(MediaType.APPLICATION_JSON);
    String weavKey = System.getenv("WEAVIATE_API_KEY");
    if (weavKey != null && !weavKey.isBlank()) {
      gqlH.set("X-API-KEY", weavKey);
    }

    Map<?,?> weavResp;
    try {
      weavResp = rest.postForObject(
        weavConfig.getGraphQLEndpoint(),
        new HttpEntity<>(Map.of("query", gql), gqlH),
        Map.class
      );
    } catch (Exception e) {
      return fallbackToOpenAI(query);
    }

    @SuppressWarnings("unchecked")
    List<Map<String,Object>> chunks = (List<Map<String,Object>>)
      ((Map<?,?>)((Map<?,?>)weavResp.get("data")).get("Get")).get("Chunk");

    if (chunks == null || chunks.isEmpty()) {
      return fallbackToOpenAI(query);
    }

    // 4) Build context + call OpenAI for answer
    StringBuilder ctx = new StringBuilder();
    List<Map<String,Object>> sources = new ArrayList<>();
    for (var c : chunks) {
      ctx.append("Page ").append(c.get("page")).append(": ").append(c.get("text")).append("\n\n");
      sources.add(Map.of(
        "page", c.get("page"),
        "excerpt", c.get("text").toString().substring(0, Math.min(100, c.get("text").toString().length())) + "...",
        "confidence", ((Map<?,?>)c.get("_additional")).get("certainty")
      ));
    }

    ResponseEntity<?> aiResp = callOpenAI("Context:\n"+ctx+"\nQuestion:\n"+query, gqlH);
    if (aiResp.getStatusCode() == HttpStatus.OK && aiResp.getBody() instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String,Object> map = new HashMap<>((Map<String,Object>) aiResp.getBody());
      map.put("sources", sources);
      return ResponseEntity.ok(map);
    }
    return aiResp;
  }

  private ResponseEntity<?> fallbackToOpenAI(String prompt) {
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    return callOpenAI(prompt, h);
  }

  private ResponseEntity<?> callOpenAI(String prompt, HttpHeaders headers) {
    String key = cfgKey.isBlank() ? System.getenv("OPENAI_API_KEY") : cfgKey;
    if (key == null || key.isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                           .body(Map.of("error","Search service unavailable."));
    }
    headers.setBearerAuth(key);
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
        new HttpEntity<>(req, headers),
        Map.class
      );
      @SuppressWarnings("unchecked")
      Map<String,Object> choice = (Map<String,Object>) ((List<?>)resp.get("choices")).get(0);
      return ResponseEntity.ok(Map.of(
        "answer", ((Map<?,?>)choice.get("message")).get("content").toString().trim()
      ));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body(Map.of("error","AI call failed"));
    }
  }
}
