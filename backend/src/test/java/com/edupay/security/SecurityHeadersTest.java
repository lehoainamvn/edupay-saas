package com.edupay.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityHeadersFilter - Thiết lập OWASP Security Headers")
class SecurityHeadersTest {

    private SecurityHeadersFilter filter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
    }

    @Test
    @DisplayName("Response phải chứa đầy đủ các OWASP security headers")
    void doFilter_SetsAllRequiredHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/invoices");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        // Content Security Policy
        String csp = response.getHeader("Content-Security-Policy");
        assertNotNull(csp);
        assertTrue(csp.contains("default-src 'self'"));
        assertTrue(csp.contains("frame-ancestors 'none'"));
        assertTrue(csp.contains("https://img.vietqr.io"));

        // Permissions Policy
        String permissions = response.getHeader("Permissions-Policy");
        assertNotNull(permissions);
        assertTrue(permissions.contains("camera=()"));
        assertTrue(permissions.contains("microphone=()"));

        // Anti Flash/PDF cross-domain
        assertEquals("none", response.getHeader("X-Permitted-Cross-Domain-Policies"));

        // Cross-Origin headers
        assertEquals("same-origin", response.getHeader("Cross-Origin-Opener-Policy"));
        assertEquals("same-origin", response.getHeader("Cross-Origin-Resource-Policy"));

        // Verify filter chain called
        verify(filterChain).doFilter(request, response);
    }
}

