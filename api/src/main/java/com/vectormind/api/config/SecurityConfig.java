package com.vectormind.api.config;

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
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      /* ---- CORS -------------------------------------------------------- */
      .cors(c -> c.configurationSource(corsConfiguration()))
      /* ---- REST  (no cookies, no sessions) ----------------------------- */
      .csrf(csrf -> csrf.disable())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      /* ---- AUTHORISATION ---------------------------------------------- */
      .authorizeHttpRequests(auth -> auth
          /* allow every CORS pre-flight without auth */
          .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

          /* public pages / static */
          .requestMatchers("/", "/dashboard", "/static/**", "/h2-console/**").permitAll()

          /* Google-Drive flow is public until user claims the token */
          .requestMatchers("/api/drive/**", "/api/fallback/drive/**").permitAll()

          /* every other API endpoint needs a Clerk JWT */
          .requestMatchers("/api/**").authenticated()

          /* any leftover route */
          .anyRequest().authenticated()
      )
      /* ---- Clerk JWT validation --------------------------------------- */
      .oauth2ResourceServer(oauth -> oauth
          .jwt(jwt -> jwt
              .jwkSetUri("https://divine-duckling-17.clerk.accounts.dev/.well-known/jwks.json")
          )
      );

    return http.build();
  }

  /* ---------- single, global CORS definition --------------------------- */
  private CorsConfigurationSource corsConfiguration() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of("https://dociq.tech", "http://localhost:3000"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setExposedHeaders(List.of("Location", "Authorization"));
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(3600L);                 // 1 h pre-flight cache

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);   // apply to every path
    return src;
  }
}
