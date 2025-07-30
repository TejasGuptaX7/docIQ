package com.vectormind.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StartupVerifier implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${openai.api.key:}")
    private String openaiKey;

    @Value("${google.client.id:}")
    private String googleId;

    @Value("${google.client.secret:}")
    private String googleSecret;

    @Value("${weaviate.url:}")
    private String weaviateUrl;

    @Value("${server.port:8080}")
    private String serverPort;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("\n========================================");
        System.out.println("=== DocIQ API Started Successfully! ===");
        System.out.println("========================================");
        System.out.println("Server running on port: " + serverPort);
        System.out.println("Environment: " + System.getenv("RAILWAY_ENVIRONMENT"));
        
        System.out.println("\n=== Service Configuration Status ===");
        System.out.println("OpenAI API: " + (openaiKey.isBlank() ? "❌ NOT CONFIGURED" : "✅ Configured"));
        System.out.println("Google OAuth: " + (!googleId.isBlank() && !googleSecret.isBlank() ? "✅ Configured" : "❌ NOT CONFIGURED"));
        System.out.println("Weaviate: " + (weaviateUrl.isBlank() ? "❌ NOT CONFIGURED" : "✅ Configured at " + weaviateUrl));
        
        if (openaiKey.isBlank()) {
            System.out.println("\n⚠️  WARNING: OpenAI API key not set - search functionality will be limited");
        }
        if (googleId.isBlank() || googleSecret.isBlank()) {
            System.out.println("⚠️  WARNING: Google OAuth not configured - Drive sync will not work");
        }
        
        System.out.println("\n=== Available Endpoints ===");
        System.out.println("Health Check: GET /health");
        System.out.println("API Test: GET /api/hello");
        System.out.println("Drive Status: GET /api/drive/status");
        System.out.println("========================================\n");
    }
}