package com.edupay.security.annotation;

import java.lang.annotation.*;

/**
 * Annotation đánh dấu method cần ghi vết kiểm toán (Audit Trail).
 * AuditLogAspect sẽ bắt các method được đánh annotation này và tự động ghi log.
 *
 * Ví dụ sử dụng:
 * <pre>
 *   {@literal @}AuditAction(action = "INVOICE_PAID", resourceType = "INVOICE")
 *   public void markAsPaid(UUID invoiceId) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditAction {

    /** Tên hành động sẽ được ghi vào cột audit_logs.action */
    String action();

    /** Loại tài nguyên bị ảnh hưởng */
    String resourceType() default "";

    /** Mức độ nghiêm trọng: INFO (default), WARN, CRITICAL */
    String severity() default "INFO";
}

