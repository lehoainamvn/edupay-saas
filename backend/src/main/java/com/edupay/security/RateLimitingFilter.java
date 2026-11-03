package com.edupay.security;

import com.edupay.config.TenantContext;
import com.edupay.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter giới hạn tần suất yêu cầu (Rate Limiting) chuyên biệt cho chức năng Điểm danh (/attendance/**).
 * Bảo vệ chống tấn công Brute-force, DoS và Spam gửi batch điểm danh liên tục.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    // Giới hạn an toàn: tối đa 60 requests / 1 phút cho mỗi client (Tenant hoặc IP)
    private static final int MAX_REQUESTS_PER_WINDOW = 60;
    private static final long WINDOW_DURATION_MS = 60_000L; // 1 phút
    private static final String ATTENDANCE_PATH = "/attendance";

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // CHỈ áp dụng riêng cho các endpoint của chức năng Điểm danh (/attendance/**)
        if (!requestUri.contains(ATTENDANCE_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);

        if (isRateLimitExceeded(clientKey)) {
            log.warn("Rate limit vượt ngưỡng cho chức năng điểm danh: client={} uri={}", clientKey, requestUri);
            handleRateLimitExceeded(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return "tenant:" + tenantId;
        }

        // Fallback theo địa chỉ IP nếu chưa có context tenant
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private boolean isRateLimitExceeded(String clientKey) {
        long now = System.currentTimeMillis();

        RequestCounter counter = requestCounts.compute(clientKey, (key, existing) -> {
            if (existing == null || now - existing.windowStartTime >= WINDOW_DURATION_MS) {
                return new RequestCounter(now, 1);
            }
            existing.count++;
            return existing;
        });

        // Tự động dọn dẹp các bộ đếm hết hạn nếu map lớn hơn 5,000 mục
        if (requestCounts.size() > 5_000) {
            cleanupExpiredCounters(now);
        }

        return counter.count > MAX_REQUESTS_PER_WINDOW;
    }

    private void cleanupExpiredCounters(long now) {
        requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStartTime >= WINDOW_DURATION_MS);
    }

    private void handleRateLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Retry-After", "60");

        ApiResponse<Void> apiResponse = ApiResponse.error(
                "RATE_LIMIT_EXCEEDED",
                "Bạn đã thực hiện quá nhiều yêu cầu điểm danh trong thời gian ngắn. Vui lòng thử lại sau 1 phút."
        );

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private static class RequestCounter {
        final long windowStartTime;
        int count;

        RequestCounter(long windowStartTime, int count) {
            this.windowStartTime = windowStartTime;
            this.count = count;
        }
    }
}

