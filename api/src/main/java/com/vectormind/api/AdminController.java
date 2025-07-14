package com.vectormind.api;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.file.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final Path uploadDir = Paths.get("uploads");
    private final RestTemplate restTemplate = new RestTemplate();

    @DeleteMapping("/purge")
    public ResponseEntity<String> purge() {
        try {
            /* delete files */
            if (Files.exists(uploadDir))
                Files.walk(uploadDir)
                     .filter(Files::isRegularFile)
                     .forEach(p -> p.toFile().delete());

            /* delete objects from classes using REST API */
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Delete all objects from Chunk class
            restTemplate.delete("http://localhost:8080/v1/objects?class=Chunk");
            
            // Delete all objects from Document class  
            restTemplate.delete("http://localhost:8080/v1/objects?class=Document");

            return ResponseEntity.ok("purged");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("purge failed: "+e.getMessage());
        }
    }
}
