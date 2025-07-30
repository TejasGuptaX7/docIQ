package com.vectormind.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class WeaviateConfig {
    
    @Value("${weaviate.url:http://localhost:8080}")
    private String weaviateUrl;
    
    private String cleanedUrl;
    
    @PostConstruct
    public void init() {
        // Clean up the URL
        cleanedUrl = weaviateUrl;
        
        // Remove any quotes
        cleanedUrl = cleanedUrl.replace("\"", "").replace("'", "");
        
        // Ensure protocol is present
        if (!cleanedUrl.startsWith("http://") && !cleanedUrl.startsWith("https://")) {
            cleanedUrl = "https://" + cleanedUrl;
        }
        
        // Remove trailing slashes
        if (cleanedUrl.endsWith("/")) {
            cleanedUrl = cleanedUrl.substring(0, cleanedUrl.length() - 1);
        }
        
        System.out.println("[WeaviateConfig] Original URL: " + weaviateUrl);
        System.out.println("[WeaviateConfig] Cleaned URL: " + cleanedUrl);
    }
    
    public String getWeaviateUrl() {
        return cleanedUrl;
    }
    
    public String getGraphQLEndpoint() {
        return cleanedUrl + "/v1/graphql";
    }
    
    public String getObjectsEndpoint() {
        return cleanedUrl + "/v1/objects";
    }
}