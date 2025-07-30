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
        /* CORS first */
        .cors(c -> c.configurationSource(cors()))
        /* stateless REST */
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        /* routes -------------------------------------------------------- */
        .authorizeHttpRequests(auth -> auth
            /* pre-flight for every path */
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            /* health check endpoint */
            .requestMatchers("/health").permitAll()

            /* public assets / demo endpoints */
            .requestMatchers("/", "/dashboard", "/static/**", "/h2-console/**").permitAll()

            /* Google-Drive flow */
            .requestMatchers("/api/drive/**", "/api/fallback/drive/**").permitAll()

            /* Test endpoint */
            .requestMatchers("/api/hello").permitAll()

            /* everything else needs a Clerk JWT */
            .requestMatchers("/api/**").authenticated()
            .anyRequest().authenticated()
        )
        /* Clerk JWT */
        .oauth2ResourceServer(o -> o
            .jwt(j -> j.jwkSetUri(
                "https://divine-duckling-17.clerk.accounts.dev/.well-known/jwks.json"))
        );

    return http.build();
  }

  /* -------- single, global CORS policy ------------------------------- */
  private CorsConfigurationSource cors() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(
        List.of("https://dociq.tech", "https://api.dociq.tech", "http://localhost:3000"));
    cfg.setAllowedMethods(
        List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS","HEAD"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setExposedHeaders(List.of("Location","Authorization"));
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(3600L);                       // 1 hour

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }
}