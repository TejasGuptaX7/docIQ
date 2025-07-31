package com.vectormind.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.vectormind.api.config.WeaviateConfig;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

@RestController
@RequestMapping("/api/docs")
public class DocumentController {

  private static final Logger logger = Logger.getLogger(DocumentController.class.getName());
  
  private final RestTemplate rest;
  private final WeaviateConfig weaviateConfig;

  @Autowired
  public DocumentController(RestTemplate rest, WeaviateConfig weaviateConfig) {
    this.rest = rest;
    this.weaviateConfig = weaviateConfig;
  }

  @GetMapping("/documents")
  public ResponseEntity<Map<String, Object>> listDocuments(Authentication auth) {
    try {
      // 1) Extract the current user's ID from the Clerk JWT
      String userId = extractUserIdFromAuth(auth);
      if (userId == null) {
        logger.warning("Unauthorized access attempt - no valid user ID found");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(createErrorResponse("Unauthorized", "Valid authentication required"));
      }

      // 2) Build the GraphQL query with proper escaping
      String gql = buildDocumentQuery(userId);

      // 3) Prepare headers
      HttpHeaders headers = createWeaviateHeaders();

      // 4) Execute the request with error handling
      Map<String, Object> requestBody = Map.of("query", gql);
      List<Map<String, Object>> docs = executeWeaviateQuery(requestBody, headers);

      // 5) Return successful response
      Map<String, Object> response = Map.of(
          "success", true,
          "data", docs,
          "count", docs.size()
      );
      
      logger.info(String.format("Successfully retrieved %d documents for user %s", docs.size(), userId));
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Unexpected error in listDocuments", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse("Internal Server Error", "An unexpected error occurred"));
    }
  }

  @GetMapping("/documents/{documentId}")
  public ResponseEntity<Map<String, Object>> getDocument(
      @PathVariable String documentId, 
      Authentication auth) {
    
    try {
      String userId = extractUserIdFromAuth(auth);
      if (userId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(createErrorResponse("Unauthorized", "Valid authentication required"));
      }

      // Query for specific document with user verification
      String gql = buildSingleDocumentQuery(documentId, userId);
      HttpHeaders headers = createWeaviateHeaders();
      Map<String, Object> requestBody = Map.of("query", gql);
      
      List<Map<String, Object>> docs = executeWeaviateQuery(requestBody, headers);
      
      if (docs.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse("Not Found", "Document not found or access denied"));
      }

      Map<String, Object> response = Map.of(
          "success", true,
          "data", docs.get(0)
      );

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error retrieving document: " + documentId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse("Internal Server Error", "Failed to retrieve document"));
    }
  }

  @DeleteMapping("/documents/{documentId}")
  public ResponseEntity<Map<String, Object>> deleteDocument(
      @PathVariable String documentId,
      Authentication auth) {
    
    try {
      String userId = extractUserIdFromAuth(auth);
      if (userId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(createErrorResponse("Unauthorized", "Valid authentication required"));
      }

      // First verify the document belongs to the user
      ResponseEntity<Map<String, Object>> getResponse = getDocument(documentId, auth);
      if (getResponse.getStatusCode() != HttpStatus.OK) {
        return getResponse; // Return the error response from getDocument
      }

      // Delete the document
      String deleteMutation = buildDeleteMutation(documentId);
      HttpHeaders headers = createWeaviateHeaders();
      Map<String, Object> requestBody = Map.of("query", deleteMutation);
      
      executeWeaviateQuery(requestBody, headers);

      Map<String, Object> response = Map.of(
          "success", true,
          "message", "Document deleted successfully"
      );

      logger.info(String.format("Document %s deleted by user %s", documentId, userId));
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error deleting document: " + documentId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse("Internal Server Error", "Failed to delete document"));
    }
  }

  // Private helper methods

  private String extractUserIdFromAuth(Authentication auth) {
    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      return jwt.getSubject();
    }
    return null;
  }

  private String buildDocumentQuery(String userId) {
    return String.format("""
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
              createdAt
              updatedAt
            }
          }
        }
        """, escapeGraphQLString(userId));
  }

  private String buildSingleDocumentQuery(String documentId, String userId) {
    return String.format("""
        {
          Get {
            Document(
              where: {
                operator: And,
                operands: [
                  {
                    path: ["userId"],
                    operator: Equal,
                    valueText: "%s"
                  },
                  {
                    path: ["_additional", "id"],
                    operator: Equal,
                    valueText: "%s"
                  }
                ]
              }
            ) {
              _additional { id }
              title
              processed
              pages
              workspace
              source
              userId
              createdAt
              updatedAt
            }
          }
        }
        """, escapeGraphQLString(userId), escapeGraphQLString(documentId));
  }

  private String buildDeleteMutation(String documentId) {
    return String.format("""
        mutation {
          Delete(
            class: "Document",
            where: {
              path: ["_additional", "id"],
              operator: Equal,
              valueText: "%s"
            }
          ) {
            successful
            objects {
              id
            }
          }
        }
        """, escapeGraphQLString(documentId));
  }

  private HttpHeaders createWeaviateHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    
    String weavKey = System.getenv("WEAVIATE_API_KEY");
    if (weavKey != null && !weavKey.isBlank()) {
      headers.set("X-API-KEY", weavKey);
    }
    
    return headers;
  }

  private List<Map<String, Object>> executeWeaviateQuery(
      Map<String, Object> requestBody, 
      HttpHeaders headers) throws Exception {
    
    try {
      @SuppressWarnings("unchecked")
      Map<?, ?> response = rest.postForObject(
          weaviateConfig.getGraphQLEndpoint(),
          new HttpEntity<>(requestBody, headers),
          Map.class
      );

      // Check for GraphQL errors
      if (response != null && response.containsKey("errors")) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) response.get("errors");
        String errorMessage = errors.stream()
            .map(error -> error.get("message"))
            .map(Object::toString)
            .reduce("", (a, b) -> a + "; " + b);
        throw new RuntimeException("GraphQL errors: " + errorMessage);
      }

      if (response == null) {
        throw new RuntimeException("Null response from Weaviate");
      }

      @SuppressWarnings("unchecked")
      Map<?, ?> data = (Map<?, ?>) response.get("data");
      if (data == null) {
        return List.of();
      }

      @SuppressWarnings("unchecked")
      Map<?, ?> get = (Map<?, ?>) data.get("Get");
      if (get == null) {
        return List.of();
      }

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> documents = (List<Map<String, Object>>) get.get("Document");
      
      return documents != null ? documents : List.of();

    } catch (HttpClientErrorException e) {
      logger.log(Level.WARNING, "Client error calling Weaviate: " + e.getStatusCode(), e);
      throw new RuntimeException("Client error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
    } catch (HttpServerErrorException e) {
      logger.log(Level.SEVERE, "Server error calling Weaviate: " + e.getStatusCode(), e);
      throw new RuntimeException("Server error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
    } catch (ResourceAccessException e) {
      logger.log(Level.SEVERE, "Network error calling Weaviate", e);
      throw new RuntimeException("Network error: Unable to connect to Weaviate");
    }
  }

  private String escapeGraphQLString(String input) {
    if (input == null) return "";
    return input.replace("\"", "\\\"")
               .replace("\n", "\\n")
               .replace("\r", "\\r")
               .replace("\t", "\\t")
               .replace("\\", "\\\\");
  }

  private Map<String, Object> createErrorResponse(String error, String message) {
    return Map.of(
        "success", false,
        "error", error,
        "message", message
    );
  }
}