package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity ánh xạ bảng `users`.
 * Mỗi user thuộc về 1 Tenant, có 1 hoặc nhiều Role.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_tenant", columnList = "tenant_id"),
        @Index(name = "idx_users_phone",  columnList = "phone")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, PENDING_VERIFY

    @Column(name = "is_owner", nullable = false)
    @Builder.Default
    private Boolean isOwner = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
