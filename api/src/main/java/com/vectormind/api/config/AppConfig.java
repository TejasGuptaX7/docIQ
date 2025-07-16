package com.vectormind.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /** Single, shared RestTemplate for all HTTP calls. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
