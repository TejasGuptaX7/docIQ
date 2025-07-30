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

  @Value("${embedding.service.url:http://localhost:5001}")
  private String embeddingServiceUrl;

  public SearchController(RestTemplate rest) {
    this.rest = rest;
  }

  private String getUserId(Authentication auth) {
    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      String userId = jwt.getSubject();
      System.out.println("[SearchController] Extracted EXACT userId: '" + userId + "'");
      return userId;
    }
    throw new RuntimeException("User not authenticated");
  }

  @PostMapping("/search")
  public ResponseEntity<?> search(@RequestBody Map<String, String> body, Authentication auth) {
    String userId = getUserId(auth);
    String query  = Optional.ofNullable(body.get("query")).orElse("").trim();
    String docId  = Optional.ofNullable(body.get("docId")).orElse("").trim();

    // Add logging
    System.out.println("[SearchController] ====== SEARCH REQUEST ======");
    System.out.println("[SearchController] userId: '" + userId + "'");
    System.out.println("[SearchController] query: '" + query + "'");
    System.out.println("[SearchController] docId: '" + docId + "'");
    System.out.println("[SearchController] isGlobal: " + docId.isBlank());
    System.out.println("[SearchController] ==========================");

    if (query.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "missing query"));
    }

    // Check if OpenAI key is available early
    String openAiKey = getOpenAIKey();
    if (openAiKey == null || openAiKey.isBlank()) {
      System.err.println("[SearchController] OpenAI API key not configured - returning error");
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                           .body(Map.of("error", "Search service is currently unavailable. OpenAI API key not configured."));
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // 1) get embedding vector
    Map<String,Object> embedReq = Map.of("texts", List.of(query));
    List<?> vector;
    try {
      @SuppressWarnings("unchecked")
      List<?> embeddings = (List<?>)
        ((Map<?,?>) rest.postForObject(
          embeddingServiceUrl + "/embed",
          new HttpEntity<>(embedReq, headers),
          Map.class
        )).get("embeddings");
      vector = (List<?>) embeddings.get(0);
      System.out.println("[SearchController] Got embedding vector of length: " + vector.size());
    } catch (Exception e) {
      System.err.println("[SearchController] Embedding failed: " + e.getMessage());
      e.printStackTrace();
      // fallback to pure AI if embedding fails
      return callOpenAI(query, headers);
    }

    // 2) build GraphQL 'where' filter - FIXED VERSION
    String whereClause;
    if (!docId.isBlank()) {
      // Search within specific document
      whereClause = String.format(
        "operator: And, operands: [" +
        "{ path: [\"userId\"], operator: Equal, valueText: \"%s\" }, " +
        "{ path: [\"docId\"], operator: Equal, valueText: \"%s\" }]",
        userId.replace("\"", "\\\""),
        docId.replace("\"", "\\\"")
      );
    } else {
      // Global search - only filter by userId
      whereClause = String.format(
        "path: [\"userId\"], operator: Equal, valueText: \"%s\"",
        userId.replace("\"", "\\\"")
      );
    }

    // 3) build and send GraphQL query
    String gql = String.format("""
      {
        Get {
          Chunk(
            where: { %s }
            nearVector: { vector: %s }
            limit: 4
          ) {
            text
            page
            docId
            userId
            _additional { certainty }
          }
        }
      }
      """, whereClause, vector.toString());

    // Log the GraphQL query
    System.out.println("[SearchController] GraphQL query:");
    System.out.println(gql);
    System.out.println("[SearchController] ==========================");

    Map<?,?> weav;
    try {
      String weaviateUrl = System.getenv("WEAVIATE_URL");
      if (weaviateUrl == null || weaviateUrl.isBlank()) {
        weaviateUrl = "http://localhost:8080";
      }
      
      // Ensure the URL has a protocol
      if (!weaviateUrl.startsWith("http://") && !weaviateUrl.startsWith("https://")) {
        weaviateUrl = "https://" + weaviateUrl;
      }
      
      // Remove any quotes that might have been included
      weaviateUrl = weaviateUrl.replace("\"", "");
      
      weav = rest.postForObject(
        weaviateUrl + "/v1/graphql",
        new HttpEntity<>(Map.of("query", gql), headers),
        Map.class
      );
      System.out.println("[SearchController] Weaviate raw response: " + weav);
    } catch (Exception e) {
      System.err.println("[SearchController] Weaviate query failed: " + e.getMessage());
      e.printStackTrace();
      return callOpenAI(query, headers);
    }

    @SuppressWarnings("unchecked")
    List<Map<String,Object>> chunks = (List<Map<String,Object>>)
      ((Map<?,?>) ((Map<?,?>) weav.get("data")).get("Get")).get("Chunk");

    // Log the results
    System.out.println("[SearchController] Found " + 
                       (chunks != null ? chunks.size() : 0) + " chunks");
    if (chunks != null && !chunks.isEmpty()) {
      System.out.println("[SearchController] First chunk:");
      System.out.println("  docId: " + chunks.get(0).get("docId"));
      System.out.println("  userId: " + chunks.get(0).get("userId"));
      System.out.println("  text preview: " + 
        String.valueOf(chunks.get(0).get("text")).substring(0, 
          Math.min(100, String.valueOf(chunks.get(0).get("text")).length())) + "...");
    }

    // 4) if no context, fallback to AI
    if (chunks == null || chunks.isEmpty()) {
      System.out.println("[SearchController] No chunks found, falling back to OpenAI");
      return callOpenAI(query, headers);
    }

    // 5) build prompt with context
    StringBuilder ctx = new StringBuilder();
    List<Map<String, Object>> sources = new ArrayList<>();
    
    for (var c : chunks) {
      ctx.append("Page ").append(c.get("page")).append(": ").append(c.get("text")).append("\n\n");
      
      // Add source information
      Map<String, Object> source = new HashMap<>();
      source.put("page", c.get("page"));
      source.put("excerpt", String.valueOf(c.get("text")).substring(0, Math.min(100, String.valueOf(c.get("text")).length())) + "...");
      source.put("confidence", ((Map<?,?>) c.get("_additional")).get("certainty"));
      sources.add(source);
    }
    
    // Call OpenAI with context
    ResponseEntity<?> aiResponse = callOpenAI("Context:\n" + ctx + "\n\nQuestion:\n" + query, headers);
    
    // Add sources to response
    if (aiResponse.getStatusCode() == HttpStatus.OK && aiResponse.getBody() instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> responseBody = new HashMap<>((Map<String, Object>) aiResponse.getBody());
      responseBody.put("sources", sources);
      return ResponseEntity.ok(responseBody);
    }
    
    return aiResponse;
  }

  @GetMapping("/debug/weaviate-count")
  public ResponseEntity<?> debugWeaviate(Authentication auth) {
    String userId = getUserId(auth);
    
    System.out.println("[Debug] ====== WEAVIATE DEBUG ======");
    System.out.println("[Debug] Checking Weaviate for userId: '" + userId + "'");
    
    // Count all documents for user
    String gql = String.format("""
        {
          Aggregate {
            Document(where: {
              path: ["userId"],
              operator: Equal,
              valueText: "%s"
            }) {
              meta { count }
              source { 
                count 
                topOccurrences(limit: 10) { 
                  value 
                  occurs 
                } 
              }
              workspace {
                count
                topOccurrences(limit: 10) {
                  value
                  occurs
                }
              }
            }
            Chunk(where: {
              path: ["userId"],
              operator: Equal,
              valueText: "%s"
            }) {
              meta { count }
              docId {
                count
                topOccurrences(limit: 5) {
                  value
                  occurs
                }
              }
            }
          }
        }
        """, userId.replace("\"", "\\\""), userId.replace("\"", "\\\""));
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    
    System.out.println("[Debug] GraphQL query:");
    System.out.println(gql);
    
    try {
      String weaviateUrl = System.getenv("WEAVIATE_URL");
      if (weaviateUrl == null || weaviateUrl.isBlank()) {
        weaviateUrl = "http://localhost:8080";
      }
      
      // Ensure the URL has a protocol
      if (!weaviateUrl.startsWith("http://") && !weaviateUrl.startsWith("https://")) {
        weaviateUrl = "https://" + weaviateUrl;
      }
      
      // Remove any quotes that might have been included
      weaviateUrl = weaviateUrl.replace("\"", "");
      
      Map<?, ?> response = rest.postForObject(
          weaviateUrl + "/v1/graphql",
          new HttpEntity<>(Map.of("query", gql), headers),
          Map.class
      );
      
      System.out.println("[Debug] Weaviate response: " + response);
      System.out.println("[Debug] =============================");
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      System.err.println("[Debug] Weaviate query failed: " + e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
  }

  private String getOpenAIKey() {
    // Try config value first, then environment variable
    String key = cfgKey;
    if (key == null || key.isBlank()) {
      key = System.getenv("OPENAI_API_KEY");
    }
    return key;
  }

  private ResponseEntity<?> callOpenAI(String prompt, HttpHeaders headers) {
    String key = getOpenAIKey();
    if (key == null || key.isBlank()) {
      System.err.println("[SearchController] OpenAI API key not found in config or environment");
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                           .body(Map.of("error","Search service unavailable - OpenAI key not configured"));
    }
    
    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
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
      Map<String,Object> choice = (Map<String,Object>) ((List<?>) resp.get("choices")).get(0);
      String answer = (String) ((Map<?,?>) choice.get("message")).get("content");
      return ResponseEntity.ok(Map.of("answer",answer.trim(),"sources",List.of()));
    } catch (Exception e) {
      System.err.println("[SearchController] OpenAI call failed: " + e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body(Map.of("error","AI response failed: "+e.getMessage()));
    }
  }
}