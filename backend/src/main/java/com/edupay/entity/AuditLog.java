package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity ánh xạ bảng audit_logs.
 * Ghi lại mọi hành động quan trọng: thanh toán, thay đổi cấu hình ngân hàng, đăng nhập thất bại.
 *
 * KHÔNG kế thừa BaseEntity để tránh yêu cầu tenant_id bắt buộc
 * (audit_logs có thể ghi sự kiện hệ thống không gắn với tenant, ví dụ: failed login với SĐT không tồn tại).
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_logs_tenant_created", columnList = "tenant_id, created_at DESC"),
        @Index(name = "idx_audit_logs_actor", columnList = "actor_user_id, created_at DESC"),
        @Index(name = "idx_audit_logs_action", columnList = "action, created_at DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** ID của Tenant liên quan đến hành động (nullable cho system events) */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /** ID của User thực hiện hành động (nullable cho anonymous/system events) */
    @Column(name = "actor_user_id")
    private UUID actorUserId;

    /** Tên hành động: INVOICE_PAID, PAYMENT_SETTING_UPDATED, LOGIN_FAILED, ... */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /** Loại tài nguyên bị ảnh hưởng: INVOICE, PAYMENT_SETTING, USER, ... */
    @Column(name = "resource_type", length = 100)
    private String resourceType;

    /** ID của tài nguyên bị ảnh hưởng */
    @Column(name = "resource_id", length = 255)
    private String resourceId;

    /** Trạng thái trước khi thay đổi (JSON) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_value", columnDefinition = "jsonb")
    private String beforeValue;

    /** Trạng thái sau khi thay đổi (JSON) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_value", columnDefinition = "jsonb")
    private String afterValue;

    /** Địa chỉ IP của người thực hiện (hỗ trợ IPv4 và IPv6) */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** User-Agent của trình duyệt/ứng dụng */
    @Column(name = "user_agent")
    private String userAgent;

    /** Mức độ nghiêm trọng: INFO, WARN, CRITICAL */
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private String severity = "INFO";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

