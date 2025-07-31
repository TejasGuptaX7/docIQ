package com.vectormind.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.vectormind.api")
public class ApiApplication {

    public static void main(String[] args) {
        // Force load environment variables as system properties
        loadEnvironmentVariables();
        
        SpringApplication.run(ApiApplication.class, args);
    }
    
    private static void loadEnvironmentVariables() {
        System.out.println("=== Loading Environment Variables ===");
        
        // Map environment variables to system properties
        setSystemPropertyFromEnv("OPENAI_API_KEY", "openai.api.key");
        setSystemPropertyFromEnv("GOOGLE_CLIENT_ID", "google.client.id");
        setSystemPropertyFromEnv("GOOGLE_CLIENT_SECRET", "google.client.secret");
        setSystemPropertyFromEnv("GOOGLE_REDIRECT_URI", "google.redirect.uri");
        setSystemPropertyFromEnv("WEAVIATE_URL", "weaviate.url");
        setSystemPropertyFromEnv("WEAVIATE_API_KEY", "weaviate.api-key");
        setSystemPropertyFromEnv("UPLOAD_EXTERNAL_ENDPOINT", "upload.external.endpoint");
        setSystemPropertyFromEnv("FRONTEND_REDIRECT_URI", "frontend.redirect.uri");
        
        System.out.println("=====================================");
    }
    
    private static void setSystemPropertyFromEnv(String envVar, String property) {
        String value = System.getenv(envVar);
        if (value != null && !value.isEmpty()) {
            System.setProperty(property, value);
            String displayValue = value;
            if (envVar.contains("KEY") || envVar.contains("SECRET")) {
                displayValue = value.length() > 10 ? value.substring(0, 10) + "..." : "HIDDEN";
            }
            System.out.println("Set " + property + " from " + envVar + " = " + displayValue);
        } else {
            System.out.println("WARNING: " + envVar + " not found in environment");
        }
    }
}