package com.vectormind.api.config;

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

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
          .cors(cors -> cors.configurationSource(corsConfigurationSource()))
          .csrf(csrf -> csrf.disable())
          .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth -> auth
              // CRITICAL: Allow ALL OPTIONS requests first (CORS preflight)
              .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

              // public UI/static
              .requestMatchers("/", "/dashboard", "/static/**", "/h2-console/**").permitAll()

              // drive flow endpoints (no JWT required)
              .requestMatchers(HttpMethod.GET, "/api/drive/connect").permitAll()
              .requestMatchers(HttpMethod.GET, "/api/drive/oauth2callback").permitAll()
              .requestMatchers(HttpMethod.GET, "/api/drive/status").permitAll()
              .requestMatchers(HttpMethod.GET, "/api/fallback/drive/status").permitAll()

              // everything else /api/** requires a valid Clerk JWT
              .requestMatchers("/api/**").authenticated()
              .anyRequest().authenticated()
          )
          .oauth2ResourceServer(oauth2 -> oauth2
              .jwt(jwt -> jwt
                  .jwkSetUri("https://divine-duckling-17.clerk.accounts.dev/.well-known/jwks.json")
              )
          );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        
        // Allow your frontend domain
        cfg.setAllowedOrigins(Arrays.asList("https://dociq.tech", "http://localhost:3000"));
        
        // Allow all common HTTP methods
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        
        // Allow all headers (including Authorization)
        cfg.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (cookies, authorization headers)
        cfg.setAllowCredentials(true);
        
        // Expose headers that frontend might need
        cfg.setExposedHeaders(Arrays.asList("Location", "Authorization"));
        
        // Set max age for preflight cache (optional, improves performance)
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}