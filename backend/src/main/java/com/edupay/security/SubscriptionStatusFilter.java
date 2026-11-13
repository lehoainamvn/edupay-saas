package com.edupay.security;

import com.edupay.config.TenantContext;
import com.edupay.dto.ApiResponse;
import com.edupay.entity.Tenant;
import com.edupay.repository.TenantRepository;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Filter kiểm tra trạng thái thuê gói (subscription_status) trước mọi API nghiệp vụ của Tenant.
 * Nếu tài khoản bị SUSPENDED hoặc EXPIRED thì chặn lại và phản hồi HTTP 403 Forbidden.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionStatusFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Bỏ qua các route công khai, auth, webhooks và trang quản trị SuperAdmin
        if (isExemptPath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("SUPER_ADMIN"))) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isPresent()) {
            Tenant tenant = tenantOpt.get();

            // 1. Kiểm tra tài khoản bị tạm khóa
            if ("SUSPENDED".equalsIgnoreCase(tenant.getSubscriptionStatus())) {
                log.warn("Chặn truy cập do tài khoản Tenant [{}] đang bị khóa (SUSPENDED)", tenantId);
                handleForbidden(response, "SUBSCRIPTION_SUSPENDED",
                        "Tài khoản giáo viên của bạn đang bị tạm khóa. Vui lòng liên hệ ban quản trị EduPay.");
                return;
            }

            // 2. Kiểm tra tài khoản hết hạn sử dụng
            if (tenant.getSubscriptionEndsAt() != null && tenant.getSubscriptionEndsAt().isBefore(Instant.now())) {
                log.warn("Chặn truy cập do gói dịch vụ của Tenant [{}] đã hết hạn", tenantId);
                handleForbidden(response, "SUBSCRIPTION_EXPIRED",
                        "Gói dịch vụ EduPay của bạn đã hết hạn. Vui lòng gia hạn để tiếp tục sử dụng.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExemptPath(String uri) {
        return uri.startsWith("/auth")
                || uri.startsWith("/api/auth")
                || uri.startsWith("/public")
                || uri.startsWith("/api/public")
                || uri.startsWith("/p/")
                || uri.startsWith("/webhooks")
                || uri.startsWith("/api/webhooks")
                || uri.startsWith("/admin")
                || uri.startsWith("/api/admin")
                || uri.startsWith("/me")
                || uri.startsWith("/api/me")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/actuator")
                || uri.startsWith("/health");
    }

    private void handleForbidden(HttpServletResponse response, String errorCode, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode, message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
