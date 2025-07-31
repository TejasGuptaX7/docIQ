package com.vectormind.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Map /api/pdf/** to the uploads/ directory
    registry.addResourceHandler("/api/pdf/**")
            .addResourceLocations("file:uploads/")
            .setCachePeriod(3600); // Cache for 1 hour
  }
  
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    // Add CORS support for PDF endpoints
    registry.addMapping("/api/pdf/**")
            .allowedOrigins("https://dociq.tech", "http://localhost:5173", "http://localhost:3000")
            .allowedMethods("GET", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
  }
}