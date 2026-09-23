package com.trustabac.iot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline security configuration for the TrustABAC-IoT gateway application.
 * Configures stateless REST security with unauthenticated access to health, device, ABAC, trust, and risk endpoints
 * for research exploration while maintaining a clean baseline for subsequent JWT security.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health",
                                "/api/devices/**",
                                "/api/policies/**",
                                "/api/bookings/**",
                                "/api/access/**",
                                "/api/trust/**",
                                "/api/risk/**",
                                "/api/authorization/**",
                                "/api/blockchain/**",
                                "/api/resource-operations/**",
                                "/api/simulator/**",
                                "/api/messaging/**",
                                "/api/websocket/**",
                                "/api/batch/**",
                                "/api/experiments/**",
                                "/ws/**",
                                "/ws",
                                "/",
                                "/dashboard/**",
                                "/dashboard",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
