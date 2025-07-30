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

    @GetMapping("/debug/env")
    public Map<String, Object> debugEnv() {
        Map<String, Object> debug = new HashMap<>();
        
        // Show actual values (partially masked for security)
        Map<String, String> configValues = new HashMap<>();
        configValues.put("openai_key_length", String.valueOf(openaiKey.length()));
        configValues.put("openai_key_preview", openaiKey.length() > 10 ? 
            openaiKey.substring(0, 10) + "..." : "NOT_SET");
        configValues.put("google_id_length", String.valueOf(googleId.length()));
        configValues.put("google_secret_length", String.valueOf(googleSecret.length()));
        configValues.put("weaviate_url", weaviateUrl);
        
        debug.put("spring_config_values", configValues);
        
        // Show raw environment variables
        Map<String, String> rawEnv = new HashMap<>();
        rawEnv.put("PORT", System.getenv("PORT"));
        rawEnv.put("OPENAI_API_KEY_exists", System.getenv("OPENAI_API_KEY") != null ? "YES" : "NO");
        rawEnv.put("GOOGLE_CLIENT_ID_exists", System.getenv("GOOGLE_CLIENT_ID") != null ? "YES" : "NO");
        rawEnv.put("GOOGLE_CLIENT_SECRET_exists", System.getenv("GOOGLE_CLIENT_SECRET") != null ? "YES" : "NO");
        rawEnv.put("WEAVIATE_URL", System.getenv("WEAVIATE_URL"));
        
        debug.put("raw_env_variables", rawEnv);
        
        return debug;
    }


}