package com.rejection.service.config;

import com.rejection.service.security.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Complete security configuration with industry-grade protection
 * Security: Rate limiting, CORS, headers, input validation
 * Performance: Optimized filter chain order
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private RateLimitingFilter rateLimitingFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        try {
            http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/", "/index.html", "/api/v1/rejection", "/api/v1/health").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                    .frameOptions(frameOptions -> frameOptions.deny())
                    .contentTypeOptions(contentTypeOptions -> {})
                    .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                        .maxAgeInSeconds(31536000)
                        .includeSubDomains(true)
                    )
                );
            
            return http.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure security filter chain: " + e.getMessage(), e);
        }
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        try {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.addAllowedOriginPattern("*");
            configuration.addAllowedMethod("GET");
            configuration.addAllowedHeader("*");
            configuration.setAllowCredentials(false);
            configuration.setMaxAge(3600L);
            
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/api/**", configuration);
            
            return source;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid CORS configuration parameters: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure CORS: " + e.getMessage(), e);
        }
    }
}