package com.vectormind.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import java.util.Properties;

public class EnvironmentLoader implements EnvironmentPostProcessor {
    
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Properties props = new Properties();
        
        // Map Railway environment variables to Spring properties
        mapEnvToProperty(props, "OPENAI_API_KEY", "openai.api.key");
        mapEnvToProperty(props, "GOOGLE_CLIENT_ID", "google.client.id");
        mapEnvToProperty(props, "GOOGLE_CLIENT_SECRET", "google.client.secret");
        mapEnvToProperty(props, "GOOGLE_REDIRECT_URI", "google.redirect.uri");
        mapEnvToProperty(props, "WEAVIATE_URL", "weaviate.url");
        mapEnvToProperty(props, "WEAVIATE_API_KEY", "weaviate.api-key");
        mapEnvToProperty(props, "UPLOAD_EXTERNAL_ENDPOINT", "upload.external.endpoint");
        mapEnvToProperty(props, "FRONTEND_REDIRECT_URI", "frontend.redirect.uri");
        
        // Add the properties with high priority
        environment.getPropertySources().addFirst(new PropertiesPropertySource("railwayEnv", props));
        
        // Debug logging
        System.out.println("=== EnvironmentLoader: Loaded environment variables ===");
        props.forEach((k, v) -> {
            String value = v.toString();
            if (k.toString().contains("key") || k.toString().contains("secret")) {
                value = value.length() > 10 ? value.substring(0, 10) + "..." : "HIDDEN";
            }
            System.out.println(k + " = " + value);
        });
    }
    
    private void mapEnvToProperty(Properties props, String envVar, String property) {
        String value = System.getenv(envVar);
        if (value != null && !value.isEmpty()) {
            props.put(property, value);
        }
    }
}