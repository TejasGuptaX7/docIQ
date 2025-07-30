package com.vectormind.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${openai.api.key:}")
    private String openaiKey;

    @Value("${google.client.id:}")
    private String googleId;

    @Value("${google.client.secret:}")
    private String googleSecret;

    @Value("${weaviate.url:}")
    private String weaviateUrl;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        
        Map<String, Boolean> services = new HashMap<>();
        services.put("openai_configured", !openaiKey.isBlank());
        services.put("google_oauth_configured", !googleId.isBlank() && !googleSecret.isBlank());
        services.put("weaviate_configured", !weaviateUrl.isBlank());
        
        status.put("services", services);
        
        // Add environment info
        Map<String, String> env = new HashMap<>();
        env.put("port", System.getenv("PORT"));
        env.put("railway_environment", System.getenv("RAILWAY_ENVIRONMENT"));
        status.put("environment", env);
        
        return status;
    }


}