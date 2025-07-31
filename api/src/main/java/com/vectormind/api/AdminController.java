package com.vectormind.api;

import com.vectormind.api.config.WeaviateConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.file.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final Path uploadDir = Paths.get("uploads");
    private final RestTemplate restTemplate;
    private final WeaviateConfig weaviateConfig;
    
    @Value("${weaviate.api-key:}")
    private String weaviateApiKey;

    public AdminController(RestTemplate restTemplate, WeaviateConfig weaviateConfig) {
        this.restTemplate = restTemplate;
        this.weaviateConfig = weaviateConfig;
    }

    @DeleteMapping("/purge")
    public ResponseEntity<String> purge() {
        try {
            /* delete files */
            if (Files.exists(uploadDir))
                Files.walk(uploadDir)
                     .filter(Files::isRegularFile)
                     .forEach(p -> p.toFile().delete());

            /* delete objects from Weaviate */
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (weaviateApiKey != null && !weaviateApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + weaviateApiKey);
            }
            
            String weaviateUrl = weaviateConfig.getWeaviateUrl();
            
            // Delete all objects from Chunk class
            restTemplate.exchange(
                weaviateUrl + "/v1/objects?class=Chunk",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                String.class
            );
            
            // Delete all objects from Document class  
            restTemplate.exchange(
                weaviateUrl + "/v1/objects?class=Document",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                String.class
            );

            return ResponseEntity.ok("purged");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("purge failed: "+e.getMessage());
        }
    }
}