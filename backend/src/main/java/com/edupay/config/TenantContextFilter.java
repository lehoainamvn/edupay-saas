package com.edupay.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter quản lý vòng đời TenantContext trên mỗi HTTP Request.
 * - Khi chưa tích hợp đầy đủ JWT Filter ở môi trường dev, tự động nạp Tenant mẫu của Cô Mai
 *   (a0000000-0000-0000-0000-000000000001 từ V3__seed_dev_data.sql) để phục vụ phát triển & kiểm thử.
 * - Luôn gọi TenantContext.clear() trong khối finally để chống rò rỉ ThreadLocal.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantContextFilter extends OncePerRequestFilter {

    public static final UUID DEV_DEFAULT_TENANT_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            if (TenantContext.getTenantId() == null && isDevProfile()) {
                TenantContext.setTenantId(DEV_DEFAULT_TENANT_ID);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isDevProfile() {
        return activeProfile != null && activeProfile.contains("dev");
    }
}
