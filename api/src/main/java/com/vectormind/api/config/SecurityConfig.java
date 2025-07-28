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
              // allow ALL OPTIONS (CORS preflight)
              .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

              // public UI/static
              .requestMatchers("/", "/dashboard", "/static/**", "/h2-console/**").permitAll()

              // drive flow (no JWT yet)
              .requestMatchers(HttpMethod.GET, "/api/drive/connect").permitAll()
              .requestMatchers(HttpMethod.GET, "/api/drive/oauth2callback").permitAll()
              .requestMatchers(HttpMethod.GET, "/api/drive/status").permitAll()

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
        cfg.setAllowedOrigins(List.of("https://dociq.tech"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        // if your front end needs to read back any headers (e.g. Location on a 302)
        cfg.setExposedHeaders(List.of("Location"));

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
