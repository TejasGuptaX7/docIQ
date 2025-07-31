package com.vectormind.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@RestController
public class DebugController {
    
    @GetMapping("/debug/all")
    public Map<String, Object> debugAll() {
        Map<String, Object> result = new HashMap<>();
        
        // Get all environment variables
        Map<String, String> env = new TreeMap<>();
        System.getenv().forEach((k, v) -> {
            if (k.contains("OPENAI") || k.contains("GOOGLE") || k.contains("WEAVIATE") || 
                k.contains("PORT") || k.contains("RAILWAY")) {
                // Mask sensitive values
                String masked = v;
                if (k.contains("KEY") || k.contains("SECRET")) {
                    masked = v.length() > 10 ? v.substring(0, 10) + "..." : "TOO_SHORT";
                }
                env.put(k, masked);
            }
        });
        
        result.put("environment_variables", env);
        
        // Get system properties
        Map<String, String> props = new TreeMap<>();
        System.getProperties().forEach((k, v) -> {
            String key = k.toString();
            if (key.contains("openai") || key.contains("google") || key.contains("weaviate") || 
                key.contains("port") || key.contains("server")) {
                props.put(key, v.toString());
            }
        });
        
        result.put("system_properties", props);
        
        return result;
    }
}
