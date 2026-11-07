package com.edupay.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalRateLimitingFilter - Giới hạn tần suất toàn cục và Auth")
class GlobalRateLimitingFilterTest {

    private GlobalRateLimitingFilter filter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        filter = new GlobalRateLimitingFilter(objectMapper);
    }

    @Test
    @DisplayName("Request thông thường trong ngưỡng cho phép được tiếp tục (proceed)")
    void doFilter_WithinLimit_Proceeds() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    @DisplayName("Endpoint health check và webhook ngân hàng được miễn trừ rate limit")
    void doFilter_ExemptPath_BypassesRateLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/webhooks/vietqr");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Gọi 400 lần liên tiếp (vượt cả global limit 300)
        for (int i = 0; i < 400; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(400)).doFilter(request, response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    @DisplayName("Auth endpoint bị chặn với 429 sau khi vượt 20 requests/phút")
    void doFilter_AuthEndpoint_ExceedingLimit_Returns429() throws ServletException, IOException {
        String clientIp = "172.16.0.50";

        // 20 requests đầu tiên thành công
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilterInternal(req, resp, filterChain);
            assertEquals(HttpStatus.OK.value(), resp.getStatus());
        }

        // Request thứ 21 bị chặn với 429
        MockHttpServletRequest req21 = new MockHttpServletRequest("POST", "/api/auth/login");
        req21.setRemoteAddr(clientIp);
        MockHttpServletResponse resp21 = new MockHttpServletResponse();

        filter.doFilterInternal(req21, resp21, filterChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), resp21.getStatus());
        assertNotNull(resp21.getHeader("Retry-After"));
        assertTrue(resp21.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));
    }
}

