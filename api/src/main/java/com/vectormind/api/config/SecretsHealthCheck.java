// src/main/java/com/vectormind/api/config/SecretsHealthCheck.java
package com.vectormind.api.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretsHealthCheck {

    @Value("${openai.api.key:}")
    private String openaiKey;

    @Value("${google.client.id:}")
    private String googleId;

    @Value("${google.client.secret:}")
    private String googleSecret;

    @PostConstruct
    public void verify() {
        if (openaiKey.isBlank())
            warn("OPENAI_API_KEY");
        if (googleId.isBlank() || googleSecret.isBlank())
            warn("GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET");
    }

    private void warn(String missing) {
        System.err.println(
            "\n************************************************************\n" +
            "  WARNING: Environment variable " + missing + " is not set.\n" +
            "  Related features will be disabled until it is provided.\n" +
            "************************************************************\n"
        );
    }
}
