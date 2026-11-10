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
 * Filter giới hạn tần suất yêu cầu (Rate Limiting) chuyên biệt:
 * 1. Chức năng Điểm danh (/attendance/**): tối đa 60 requests / 1 phút (theo Tenant hoặc IP).
 * 2. Cổng tra cứu Phiếu thu công khai (/public/invoices/**, /api/public/invoices/**): tối đa 30 requests / 1 phút / IP.
 * Bảo vệ chống tấn công Brute-force, DoS và dò mã hash hóa đơn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    private static final int MAX_ATTENDANCE_REQUESTS = 60;
    private static final int MAX_PUBLIC_INVOICE_REQUESTS = 30; // 30 req/phút/IP theo yêu cầu
    private static final long WINDOW_DURATION_MS = 60_000L; // 1 phút

    private static final String ATTENDANCE_PATH = "/attendance";
    private static final String PUBLIC_INVOICE_PATH = "/public/invoices";

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // 1. Kiểm tra Rate Limiting cho Cổng Phụ huynh tra cứu phiếu thu (/public/invoices/**, /api/public/invoices/**)
        if (requestUri.contains(PUBLIC_INVOICE_PATH)) {
            String ipKey = "public_inv:" + resolveClientIp(request);
            if (isRateLimitExceeded(ipKey, MAX_PUBLIC_INVOICE_REQUESTS)) {
                log.warn("Rate limit vượt ngưỡng cho Cổng phụ huynh tra cứu: ip={} uri={}", ipKey, requestUri);
                handleRateLimitExceeded(
                        response,
                        "RATE_LIMIT_EXCEEDED",
                        "Bạn đã gửi quá nhiều yêu cầu tra cứu phiếu thu (tối đa 30 lần/phút). Vui lòng thử lại sau 1 phút."
                );
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Kiểm tra Rate Limiting cho Chức năng Điểm danh (/attendance/**)
        if (requestUri.contains(ATTENDANCE_PATH)) {
            String clientKey = "attendance:" + resolveClientKey(request);
            if (isRateLimitExceeded(clientKey, MAX_ATTENDANCE_REQUESTS)) {
                log.warn("Rate limit vượt ngưỡng cho chức năng điểm danh: client={} uri={}", clientKey, requestUri);
                handleRateLimitExceeded(
                        response,
                        "RATE_LIMIT_EXCEEDED",
                        "Bạn đã thực hiện quá nhiều yêu cầu điểm danh trong thời gian ngắn (tối đa 60 lần/phút). Vui lòng thử lại sau 1 phút."
                );
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Các route khác tiếp tục bình thường
        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveClientKey(HttpServletRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return "tenant:" + tenantId;
        }
        return "ip:" + resolveClientIp(request);
    }

    private boolean isRateLimitExceeded(String clientKey, int maxRequests) {
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

        return counter.count > maxRequests;
    }

    private void cleanupExpiredCounters(long now) {
        requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStartTime >= WINDOW_DURATION_MS);
    }

    private void handleRateLimitExceeded(HttpServletResponse response, String errorCode, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Retry-After", "60");

        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode, message);
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
