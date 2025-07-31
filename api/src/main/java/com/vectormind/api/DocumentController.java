package com.vectormind.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.vectormind.api.config.WeaviateConfig;

import java.util.*;

@RestController
@RequestMapping("/api")
public class DocumentController {

  private final RestTemplate rest;
  private final WeaviateConfig weaviateConfig;

  @Autowired
  public DocumentController(RestTemplate rest, WeaviateConfig weaviateConfig) {
    this.rest            = rest;
    this.weaviateConfig  = weaviateConfig;
  }

  @GetMapping("/documents")
  public ResponseEntity<List<Map<String,Object>>> listDocuments(Authentication auth) {
    // 1) Extract the current user's ID from the Clerk JWT
    String userId = null;
    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      userId = jwt.getSubject();
    }
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // 2) Build the GraphQL query
    String gql = String.format("""
      {
        Get {
          Document(
            where: {
              path: ["userId"],
              operator: Equal,
              valueText: "%s"
            }
          ) {
            _additional { id }
            title
            processed
            pages
            workspace
            source
            userId
          }
        }
      }
      """, userId.replace("\"","\\\""));

    // 3) Prepare headers (including your Weaviate API key)
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String weavKey = System.getenv("WEAVIATE_API_KEY");
    if (weavKey != null && !weavKey.isBlank()) {
      headers.set("X-API-KEY", weavKey);
    }

    // 4) Fire the request
    Map<String,Object> requestBody = Map.of("query", gql);
    List<Map<String,Object>> docs = List.of();
    try {
      @SuppressWarnings("unchecked")
      Map<?,?> resp = rest.postForObject(
        weaviateConfig.getGraphQLEndpoint(),
        new HttpEntity<>(requestBody, headers),
        Map.class
      );
      @SuppressWarnings("unchecked")
      Map<?,?> data = (Map<?,?>) resp.get("data");
      @SuppressWarnings("unchecked")
      Map<?,?> get   = (Map<?,?>) data.get("Get");
      @SuppressWarnings("unchecked")
      List<Map<String,Object>> list = (List<Map<String,Object>>) get.get("Document");
      docs = list != null ? list : List.of();
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
    }

    // 5) Return
    return ResponseEntity.ok(docs);
  }
}
