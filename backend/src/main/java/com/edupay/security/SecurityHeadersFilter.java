package com.edupay.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter bổ sung các Security Headers chuẩn OWASP Top 10 không được Spring Security DSL bao gồm sẵn.
 *
 * Headers được thêm vào mọi response:
 * - Content-Security-Policy: Chống XSS và data injection
 * - Permissions-Policy: Vô hiệu hóa camera, microphone, geolocation
 * - X-Permitted-Cross-Domain-Policies: Chống Adobe Flash/PDF cross-domain abuse
 * - Cross-Origin-Opener-Policy: Cô lập tab/window khỏi cross-origin pages
 * - Cross-Origin-Resource-Policy: Chặn cross-origin resource loading
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CSP_VALUE =
            "default-src 'self'; " +
            "script-src 'self'; " +
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
            "img-src 'self' data: https://img.vietqr.io https://api.vietqr.io https://*.vietqr.io; " +
            "connect-src 'self' https://api.vietqr.io; " +
            "font-src 'self' https://fonts.gstatic.com data:; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ─── Content Security Policy ─────────────────────────────────────────
        response.setHeader("Content-Security-Policy", CSP_VALUE);

        // ─── Permissions Policy (giới hạn các API trình duyệt nhạy cảm) ─────
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=(), usb=()");

        // ─── Chống Flash/PDF cross-domain access ─────────────────────────────
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");

        // ─── Cross-Origin isolation headers ──────────────────────────────────
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "cross-origin");

        filterChain.doFilter(request, response);
    }
}

