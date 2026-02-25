package com.codex.identity_verifier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/**").permitAll()  // Allow all API endpoints
                .anyRequest().permitAll()               // Allow all other requests
            )
            .csrf(csrf -> csrf.disable());              // Disable CSRF for API
        
        return http.build();
    }
}