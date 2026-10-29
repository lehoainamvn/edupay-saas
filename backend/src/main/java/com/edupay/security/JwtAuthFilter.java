package com.edupay.security;

import com.edupay.config.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Filter chạy 1 lần mỗi request.
 * Đọc Bearer token → validate → set SecurityContext + TenantContext.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.isTokenValid(token)) {
                Claims claims = jwtTokenProvider.validateAndGetClaims(token);

                UUID userId   = jwtTokenProvider.getUserId(claims);
                UUID tenantId = jwtTokenProvider.getTenantId(claims);
                List<String> roles = jwtTokenProvider.getRoles(claims);

                // Set TenantContext cho ThreadLocal — các Service dùng TenantContext.getTenantId()
                TenantContext.setTenantId(tenantId);

                // Set SecurityContext để Spring Security nhận diện user
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId.toString(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authenticated userId={}, tenantId={}", userId, tenantId);
            }

        } catch (ExpiredJwtException e) {
            log.warn("Access token đã hết hạn — request: {}", request.getRequestURI());
        } catch (Exception e) {
            log.error("Lỗi xử lý JWT filter: {}", e.getMessage());
        } finally {
            filterChain.doFilter(request, response);
            // QUAN TRỌNG: Xóa TenantContext sau mỗi request để tránh rò rỉ ThreadLocal
            TenantContext.clear();
        }
    }

    /** Trích xuất Bearer token từ Authorization header */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
