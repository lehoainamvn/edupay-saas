package com.edupay.config;

import com.edupay.security.GlobalRateLimitingFilter;
import com.edupay.security.JwtAuthFilter;
import com.edupay.security.RateLimitingFilter;
import com.edupay.security.SecurityHeadersFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final GlobalRateLimitingFilter globalRateLimitingFilter;
    private final SecurityHeadersFilter securityHeadersFilter;

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String corsAllowedOrigins;

    private static final String[] PUBLIC_WHITE_LIST = {
            // Swagger / OpenAPI documentation
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",

            // Auth endpoints (không cần token)
            "/auth/register",
            "/auth/login",
            "/auth/refresh-token",
            "/auth/logout",      // logout chỉ cần refreshToken trong body

            // Public routes
            "/public/**",
            "/webhooks/**",
            "/actuator/health",
            "/health",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ─── OWASP Security Headers ──────────────────────────────────
                .headers(headers -> headers
                        // X-Frame-Options: DENY — chống Clickjacking
                        .frameOptions(frame -> frame.deny())
                        // X-Content-Type-Options: nosniff — chống MIME sniffing
                        .contentTypeOptions(contentType -> {})
                        // HSTS: max-age 1 năm, bao gồm sub-domains
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31_536_000)
                                .includeSubDomains(true))
                        // Referrer-Policy: strict-origin-when-cross-origin
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_WHITE_LIST).permitAll()
                        .requestMatchers("/auth/logout-all").authenticated() // Cần JWT để biết revoke ai
                        .anyRequest().authenticated()
                )

                // ─── Filter Chain Order ───────────────────────────────────────
                // 1. SecurityHeadersFilter: thiết lập OWASP headers cho MỌI response (kể cả khi 429 hoặc 401)
                .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
                // 2. GlobalRateLimitingFilter: chạy ngay sau header filter (bảo vệ cả public endpoints)
                .addFilterAfter(globalRateLimitingFilter, SecurityHeadersFilter.class)
                // 3. JwtAuthFilter: validate token, set SecurityContext
                .addFilterAfter(jwtAuthFilter, GlobalRateLimitingFilter.class)
                // 4. RateLimitingFilter: giới hạn riêng cho /attendance/**
                .addFilterAfter(rateLimitingFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Thay thế allowedOriginPatterns("*") bằng danh sách origins được cấu hình
        List<String> allowedOrigins = Arrays.asList(corsAllowedOrigins.split(","));
        config.setAllowedOrigins(allowedOrigins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Cache-Control"
        ));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

