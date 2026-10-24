package com.edupay.config;

import java.util.UUID;

/**
 * Lưu trữ tenant_id trong ThreadLocal suốt vòng đời của 1 HTTP Request.
 * Được trích xuất từ JWT token qua AuthenticationFilter.
 */
public class TenantContext {
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
