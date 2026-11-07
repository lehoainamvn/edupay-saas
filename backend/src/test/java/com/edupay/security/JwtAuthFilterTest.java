package com.edupay.security;

import com.edupay.config.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter - Xác thực JWT & Kiểm tra lỗ hổng Backdoor / Blacklist")
class JwtAuthFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("LỖ HỔNG BACKDOOR: dev_mock_jwt_token bị chặn hoàn toàn trên môi trường PRODUCTION")
    void doFilter_DevMockTokenInProduction_Blocked() throws ServletException, IOException {
        JwtAuthFilter prodFilter = new JwtAuthFilter(jwtTokenProvider, tokenBlacklist, "prod");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students");
        request.addHeader("Authorization", "Bearer dev_mock_jwt_token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        prodFilter.doFilterInternal(request, response, filterChain);

        // SecurityContext không được gán quyền
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(TenantContext.getTenantId());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Môi trường DEV: dev_mock_jwt_token được phép sử dụng để test nhanh")
    void doFilter_DevMockTokenInDev_Allowed() throws ServletException, IOException {
        JwtAuthFilter devFilter = new JwtAuthFilter(jwtTokenProvider, tokenBlacklist, "dev");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students");
        request.addHeader("Authorization", "Bearer dev_mock_jwt_token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        devFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("ROLE_OWNER", SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Token đã bị Blacklist (sau khi logout) bị từ chối ngay lập tức")
    void doFilter_BlacklistedToken_Rejected() throws ServletException, IOException {
        JwtAuthFilter filter = new JwtAuthFilter(jwtTokenProvider, tokenBlacklist, "dev");

        String token = "valid-looking-jwt-token";
        Claims claims = mock(Claims.class);

        when(jwtTokenProvider.isTokenValid(token)).thenReturn(true);
        when(jwtTokenProvider.validateAndGetClaims(token)).thenReturn(claims);
        when(jwtTokenProvider.getJti(claims)).thenReturn("revoked-jti-123");
        when(tokenBlacklist.isBlacklisted("revoked-jti-123")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/classes");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        // Không được gán quyền vào SecurityContext
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Token hợp lệ và chưa bị blacklist: xác thực thành công")
    void doFilter_ValidToken_AuthenticatesSuccessfully() throws ServletException, IOException {
        JwtAuthFilter filter = new JwtAuthFilter(jwtTokenProvider, tokenBlacklist, "dev");

        String token = "valid-active-jwt-token";
        Claims claims = mock(Claims.class);
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(jwtTokenProvider.isTokenValid(token)).thenReturn(true);
        when(jwtTokenProvider.validateAndGetClaims(token)).thenReturn(claims);
        when(jwtTokenProvider.getJti(claims)).thenReturn("active-jti-456");
        when(tokenBlacklist.isBlacklisted("active-jti-456")).thenReturn(false);
        when(jwtTokenProvider.getUserId(claims)).thenReturn(userId);
        when(jwtTokenProvider.getTenantId(claims)).thenReturn(tenantId);
        when(jwtTokenProvider.getRoles(claims)).thenReturn(List.of("OWNER"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/classes");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userId.toString(), SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }
}

