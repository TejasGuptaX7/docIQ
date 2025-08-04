package com.vectormind.api.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Disable CSRF for stateless REST API
            .csrf(csrf -> csrf.disable())
            
            // Stateless session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow OPTIONS requests for CORS pre-flight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Public endpoints
                .requestMatchers(
                    "/health",
                    "/debug/env",
                    "/test",
                    "/debug/all"
                ).permitAll()
                
                // Static resources and demo endpoints
                .requestMatchers(
                    "/",
                    "/dashboard",
                    "/static/**",
                    "/h2-console/**"
                ).permitAll()
                
                // Google Drive integration endpoints
                .requestMatchers(
                    "/api/drive/**",
                    "/api/fallback/drive/**"
                ).permitAll()
                
                // Test endpoint
                .requestMatchers("/api/hello").permitAll()
                
                // Protected API endpoints (require authentication)
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/api/pdf/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwkSetUri("https://divine-duckling-17.clerk.accounts.dev/.well-known/jwks.json")
                )
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins
        configuration.setAllowedOrigins(Arrays.asList(
            "https://dociq.tech",
            "https://api.dociq.tech",
            "http://localhost:3000"
        ));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
            "Location",
            "Authorization"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Cache duration for CORS pre-flight requests (1 hour)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}