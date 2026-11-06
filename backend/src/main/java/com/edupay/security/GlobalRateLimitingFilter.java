package com.edupay.security;

import com.edupay.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter rate limiting toàn cục sử dụng thuật toán Token Bucket (Bucket4j).
 * - Global: 200 requests/phút mỗi IP
 * - Auth endpoints (/auth/**): 10 requests/phút mỗi IP — chống brute-force
 * - Exempt: /actuator/health, /webhooks/**
 *
 * Chạy TRƯỚC JwtAuthFilter để bảo vệ cả các endpoint public.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalRateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    // ─── Giới hạn tối ưu cho trải nghiệm người dùng trên mạng dùng chung (Wi-Fi trường/trung tâm) ─
    private static final int GLOBAL_LIMIT_PER_MIN = 300; // 300 req/phút cho traffic chung
    private static final int AUTH_LIMIT_PER_MIN    = 20;  // 20 req/phút cho đăng nhập

    // ─── Bucket maps theo IP ────────────────────────────────────────────────
    private final ConcurrentHashMap<String, Bucket> globalBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> authBuckets   = new ConcurrentHashMap<>();

    // ─── Paths exempt khỏi rate limit ──────────────────────────────────────
    private static final String[] EXEMPT_PREFIXES = {
            "/actuator/health",
            "/health",
            "/error",
            "/webhooks"  // Webhook ngân hàng tự động đối soát: KHÔNG bao giờ được chặn
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String servletPath = request.getServletPath();

        // Bỏ qua health-check và error endpoints (hỗ trợ cả có và không có context-path /api)
        for (String exempt : EXEMPT_PREFIXES) {
            if (uri.startsWith(exempt) || (servletPath != null && servletPath.startsWith(exempt)) || uri.contains(exempt)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String clientIp = resolveClientIp(request);

        // ─── Auth endpoints: giới hạn chặt hơn ────────────────────────────
        if (uri.contains("/auth/")) {
            Bucket authBucket = authBuckets.computeIfAbsent(clientIp, ip -> buildBucket(AUTH_LIMIT_PER_MIN));
            if (!authBucket.tryConsume(1)) {
                log.warn("Auth rate limit vượt ngưỡng: ip={} uri={}", clientIp, uri);
                rejectWithTooManyRequests(response,
                        "Quá nhiều lượt thử đăng nhập từ mạng của bạn. Vui lòng đợi 1 phút trước khi thử lại.", 60);
                return;
            }
        }

        // ─── Global rate limit ─────────────────────────────────────────────
        Bucket globalBucket = globalBuckets.computeIfAbsent(clientIp, ip -> buildBucket(GLOBAL_LIMIT_PER_MIN));
        if (!globalBucket.tryConsume(1)) {
            log.warn("Global rate limit vượt ngưỡng: ip={} uri={}", clientIp, uri);
            rejectWithTooManyRequests(response,
                    "Hệ thống đang tiếp nhận quá nhiều thao tác từ mạng của bạn. Vui lòng chờ vài giây rồi thử lại.", 60);
            return;
        }

        filterChain.doFilter(request, response);
    }

    // ─── Builders ──────────────────────────────────────────────────────────

    // ─── Regex kiểm tra tính hợp lệ của địa chỉ IPv4 / IPv6 chống IP spoofing injection ─
    private static final java.util.regex.Pattern IP_V4_PATTERN =
            java.util.regex.Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$");
    private static final java.util.regex.Pattern IP_V6_PATTERN =
            java.util.regex.Pattern.compile("^[0-9a-fA-F:]+$");

    private Bucket buildBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String candidate = forwardedFor.split(",")[0].trim();
            if (isValidIp(candidate)) {
                return "ip:" + candidate;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            String candidate = realIp.trim();
            if (isValidIp(candidate)) {
                return "ip:" + candidate;
            }
        }
        return "ip:" + request.getRemoteAddr();
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || ip.length() > 45) return false;
        return IP_V4_PATTERN.matcher(ip).matches() || IP_V6_PATTERN.matcher(ip).matches();
    }

    private void rejectWithTooManyRequests(HttpServletResponse response,
                                           String message,
                                           int retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        ApiResponse<Void> body = ApiResponse.error("RATE_LIMIT_EXCEEDED", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Dọn dẹp định kỳ các bucket đã đầy (client không gửi request trong thời gian dài).
     * Ngăn ngừa rò rỉ bộ nhớ (memory leak) khi số lượng IP truy cập tăng theo thời gian.
     */
    @Scheduled(fixedRate = 300_000L) // Mỗi 5 phút
    public void cleanupIdleBuckets() {
        int beforeTotal = globalBuckets.size() + authBuckets.size();
        globalBuckets.entrySet().removeIf(entry -> entry.getValue().getAvailableTokens() >= GLOBAL_LIMIT_PER_MIN);
        authBuckets.entrySet().removeIf(entry -> entry.getValue().getAvailableTokens() >= AUTH_LIMIT_PER_MIN);
        int afterTotal = globalBuckets.size() + authBuckets.size();
        if (beforeTotal > afterTotal) {
            log.debug("RateLimiting cleanup: xóa {} idle buckets, còn lại: global={}, auth={}",
                    beforeTotal - afterTotal, globalBuckets.size(), authBuckets.size());
        }
    }
}

