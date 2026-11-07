package com.edupay.service;

import java.util.UUID;

/**
 * Service ghi lại vết kiểm toán (Audit Trail) cho các hành động quan trọng trong hệ thống.
 * Tất cả các method là bất đồng bộ (@Async) để không làm chậm main business flow.
 */
public interface AuditLogService {

    /**
     * Ghi log một hành động với đầy đủ thông tin (chấp nhận String IP/UserAgent để an toàn với đa luồng @Async).
     *
     * @param tenantId     UUID của tenant (nullable cho system events)
     * @param actorUserId  UUID của user thực hiện (nullable cho anonymous events)
     * @param action       Tên hành động: INVOICE_PAID, LOGIN_FAILED, PAYMENT_SETTING_UPDATED, ...
     * @param resourceType Loại tài nguyên: INVOICE, PAYMENT_SETTING, USER, ...
     * @param resourceId   ID của tài nguyên bị ảnh hưởng
     * @param beforeJson   JSON string trạng thái trước (nullable)
     * @param afterJson    JSON string trạng thái sau (nullable)
     * @param severity     Mức độ: INFO, WARN, CRITICAL
     * @param ipAddress    Địa chỉ IP của người dùng (nullable)
     * @param userAgent    User-Agent của trình duyệt/ứng dụng (nullable)
     */
    void logAction(UUID tenantId,
                   UUID actorUserId,
                   String action,
                   String resourceType,
                   String resourceId,
                   String beforeJson,
                   String afterJson,
                   String severity,
                   String ipAddress,
                   String userAgent);

    /**
     * Tiện ích: ghi log hành động INFO đơn giản.
     */
    void logInfo(UUID tenantId, UUID actorUserId, String action,
                 String resourceType, String resourceId,
                 String ipAddress, String userAgent);

    /**
     * Tiện ích: ghi log hành động WARN (ví dụ: failed login).
     */
    void logWarn(UUID tenantId, UUID actorUserId, String action,
                 String resourceType, String resourceId,
                 String ipAddress, String userAgent);
}
