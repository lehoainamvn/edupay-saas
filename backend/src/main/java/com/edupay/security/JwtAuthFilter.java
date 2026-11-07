package com.edupay.security;

import com.edupay.config.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * Đọc Bearer token → validate → kiểm tra TokenBlacklist → set SecurityContext + TenantContext.
 */
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final String activeProfile;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider,
                         @Autowired(required = false) TokenBlacklist tokenBlacklist,
                         @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklist = tokenBlacklist != null ? tokenBlacklist : new TokenBlacklist();
        this.activeProfile = activeProfile != null ? activeProfile : "dev";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token) && token.startsWith("dev_mock_jwt_token")) {
                // CHẶN BẢO MẬT: Cấm tuyệt đối dev mock token trên môi trường production
                if ("prod".equalsIgnoreCase(activeProfile)) {
                    log.error("CẢNH BÁO BẢO MẬT: Chặn truy cập trái phép bằng dev_mock_jwt_token trên môi trường PRODUCTION! URI={}",
                            request.getRequestURI());
                    return;
                }

                UUID userId   = UUID.fromString("c0000000-0000-0000-0000-000000000001");
                UUID tenantId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
                TenantContext.setTenantId(tenantId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId.toString(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_OWNER"), new SimpleGrantedAuthority("ROLE_TEACHER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated via dev mock token userId={}, tenantId={}", userId, tenantId);
            } else if (StringUtils.hasText(token) && jwtTokenProvider.isTokenValid(token)) {
                Claims claims = jwtTokenProvider.validateAndGetClaims(token);

                // ─── Kiểm tra JWT Blacklist (token đã bị thu hồi khi logout) ───
                String jti = jwtTokenProvider.getJti(claims);
                if (jti != null && tokenBlacklist.isBlacklisted(jti)) {
                    log.warn("JWT bị từ chối — token đã bị thu hồi (blacklisted): jti={}", jti);
                    return;
                }

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

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}

