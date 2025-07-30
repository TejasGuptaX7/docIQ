package com.vectormind.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.application.name:api}")
    private String appName;

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "application", appName,
            "port", serverPort,
            "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/api/hello")
    public String hello() {
        return "Hello from " + appName + " on port " + serverPort;
    }
}
