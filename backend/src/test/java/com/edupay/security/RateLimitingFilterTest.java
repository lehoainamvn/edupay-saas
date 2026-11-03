package com.edupay.security;

import com.edupay.config.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;
    private ObjectMapper objectMapper;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        rateLimitingFilter = new RateLimitingFilter(objectMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Bỏ qua kiểm tra giới hạn cho các endpoint không thuộc /attendance/**")
    void doFilter_NonAttendancePath_ProceedsDirectly() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/classes");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Cho phép yêu cầu hợp lệ dưới ngưỡng 60 req/phút cho /attendance/**")
    void doFilter_WithinRateLimit_Proceeds() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/attendance/classes/123/daily");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Trả về 429 Too Many Requests khi gửi vượt ngưỡng 60 req/phút cho /attendance/**")
    void doFilter_ExceedsRateLimit_Returns429() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        // Gửi 60 requests đầu tiên
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/attendance/batch");
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilterInternal(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        // Request thứ 61 vượt ngưỡng
        MockHttpServletRequest req61 = new MockHttpServletRequest("POST", "/api/attendance/batch");
        MockHttpServletResponse res61 = new MockHttpServletResponse();

        rateLimitingFilter.doFilterInternal(req61, res61, filterChain);

        assertEquals(429, res61.getStatus());
        assertEquals("60", res61.getHeader("Retry-After"));
        assertTrue(res61.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));

        // Chỉ được gọi 60 lần trong filterChain, lần thứ 61 bị ngắt sớm
        verify(filterChain, times(60)).doFilter(any(), any());
    }
}
