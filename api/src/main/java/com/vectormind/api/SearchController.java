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
      if (query.isBlank() || docId == null)
        return mock("missing query or docId");

      // 1 — embed query
      HttpHeaders hJson = new HttpHeaders();
      hJson.setContentType(MediaType.APPLICATION_JSON);
      Map<String,Object> eReq = Map.of("texts", List.of(query));

      List<?> vec = (List<?>) ((List<?>)
        rest.postForObject("http://localhost:5001/embed",
          new HttpEntity<>(eReq,hJson), Map.class)
          .get("embeddings")).get(0);

      // 2 — search in Weaviate, scoped to this docId
      String gql = """
        {
          Get {
            Chunk(
              where:{ path:["docId"], operator:Equal, valueText:"%s" }
              nearVector:{vector:%s}
              limit:4
            ){
              text
              page
              _additional { certainty }
            }
          }
        }
      """.formatted(docId, vec.toString());

      Map<?,?> weav = rest.postForObject(
        "http://localhost:8080/v1/graphql",
        new HttpEntity<>(Map.of("query", gql), hJson), Map.class);

      List<Map<String,Object>> chunks = (List<Map<String,Object>>)
        ((Map<?,?>)((Map<?,?>) weav.get("data")).get("Get")).get("Chunk");

      if (chunks==null || chunks.isEmpty()) return mock("no chunks");

      StringBuilder ctx = new StringBuilder();
      for (Map<String,Object> c : chunks)
        ctx.append("Page ").append(c.get("page")).append(": ")
           .append(c.get("text")).append("\n\n");

      // 3 — call OpenAI
      String key = !cfgKey.isBlank() ? cfgKey : System.getenv("OPENAI_API_KEY");
      if (key==null || key.isBlank()) return mock("no key");

      HttpHeaders oh = new HttpHeaders();
      oh.setContentType(MediaType.APPLICATION_JSON);
      oh.setBearerAuth(key);

      Map<String,Object> oaReq = Map.of(
        "model", "gpt-4o-mini",
        "messages", List.of(
          Map.of("role","system", "content", "Answer strictly from the provided context."),
          Map.of("role","user", "content", "Context:\n"+ctx+"\n\nQuestion:\n"+query)
        ),
        "temperature", 0.2,
        "max_tokens", 256
      );

      Map<?,?> oa = rest.postForObject(
        "https://api.openai.com/v1/chat/completions",
        new HttpEntity<>(oaReq, oh), Map.class);

      Map<String,Object> message = (Map<String,Object>)
        ((Map<?,?>)((List<?>) oa.get("choices")).get(0)).get("message");

      String answer = (String) message.get("content");

      List<Map<String,Object>> src = chunks.stream().map(c -> Map.of(
        "page", c.get("page"),
        "excerpt", ((String)c.get("text")).substring(0, 120) + "…",
        "confidence", ((Map<?,?>)c.get("_additional")).get("certainty")
      )).toList();

      return ResponseEntity.ok(Map.of("answer", answer.trim(), "sources", src));

    } catch(Exception e){
      e.printStackTrace();
      return mock("error "+e.getMessage());
    }
  }

  private ResponseEntity<Map<String,String>> mock(String reason){
    System.out.println("mock → "+reason);
    return ResponseEntity.ok(Map.of(
      "answer","Based on your query, here's a mock answer:"
    ));
  }
}
