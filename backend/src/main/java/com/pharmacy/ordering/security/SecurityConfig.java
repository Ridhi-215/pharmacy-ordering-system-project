package com.pharmacy.ordering.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${pharmacy.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(auth -> auth

            // Swagger endpoints
            .requestMatchers(
                "/v3/api-docs/**",
                "/api-docs/**",
                "/api-docs",
                "/swagger-ui/**",
                "/swagger-ui.html"
            ).permitAll()

            // Public auth endpoints
            .requestMatchers(
                "/api/auth/register",
                "/api/auth/login",
                "/api/auth/verify",
                "/api/auth/resend-verification"
            ).permitAll()

            // Public medicine/category endpoints
            .requestMatchers(
                HttpMethod.GET,
                "/api/medicines/**",
                "/api/categories"
            ).permitAll()

            // Public uploads
            .requestMatchers("/uploads/**").permitAll()

            // Admin endpoints
            .requestMatchers("/api/admin/**").hasRole("ADMIN")

            .requestMatchers(HttpMethod.POST, "/api/medicines")
            .hasRole("ADMIN")

            .requestMatchers(HttpMethod.PUT, "/api/medicines/**")
            .hasRole("ADMIN")

            .requestMatchers(HttpMethod.DELETE, "/api/medicines/**")
            .hasRole("ADMIN")

            .requestMatchers(HttpMethod.POST, "/api/categories")
            .hasRole("ADMIN")

            .requestMatchers(
                "/api/prescriptions/*/approve",
                "/api/prescriptions/*/reject"
            ).hasRole("ADMIN")

            // Authenticated user endpoints
            .requestMatchers("/api/auth/profile").authenticated()

            .requestMatchers("/api/cart/**").authenticated()

            .requestMatchers(
                "/api/orders/place",
                "/api/orders/history",
                "/api/orders/*"
            ).authenticated()

            .requestMatchers(
                "/api/prescriptions/upload",
                "/api/prescriptions"
            ).authenticated()

            .requestMatchers("/api/payments/**").authenticated()

            // Everything else
            .anyRequest().permitAll()
        )

        // Enable HTTP Basic for Swagger testing
        .httpBasic(Customizer.withDefaults());

    // Add JWT filter
    http.addFilterBefore(
        jwtAuthenticationFilter,
        UsernamePasswordAuthenticationFilter.class
    );

    return http.build();
}

    @Bean
    public org.springframework.web.filter.CorsFilter corsFilter() {
        log.info("CORS allowed-origins configured: {}", allowedOrigins);
        CorsConfiguration configuration = new CorsConfiguration();
        java.util.List<String> allowedPatterns = new java.util.ArrayList<>();
        
        // Add defaults
        allowedPatterns.add("https://*.onrender.com");
        allowedPatterns.add("http://localhost:[*]");
        allowedPatterns.add("http://127.0.0.1:[*]");
        
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            for (String origin : allowedOrigins.split(",")) {
                String clean = origin.trim();
                if (!clean.isEmpty()) {
                    allowedPatterns.add(clean);
                    if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
                        allowedPatterns.add("http://" + clean);
                        allowedPatterns.add("https://" + clean);
                    }
                }
            }
        }
        log.info("CORS final resolved allowed origin patterns: {}", allowedPatterns);
        
        configuration.setAllowedOriginPatterns(allowedPatterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(Collections.singletonList("Authorization"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new org.springframework.web.filter.CorsFilter(source);
    }
}
