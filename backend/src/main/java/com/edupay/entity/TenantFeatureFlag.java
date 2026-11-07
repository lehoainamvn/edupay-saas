package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "tenant_feature_flags",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_feature", columnNames = {"tenant_id", "feature_id"})
    },
    indexes = {
        @Index(name = "idx_tenant_feature_flags_tenant", columnList = "tenant_id"),
        @Index(name = "idx_tenant_feature_flags_feature", columnList = "feature_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantFeatureFlag extends BaseEntity {

    @Column(name = "feature_id", nullable = false, length = 100)
    private String featureId;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(length = 255)
    private String reason;

    @Column(name = "expires_at")
    private Instant expiresAt; // NULL = active throughout subscription
}

