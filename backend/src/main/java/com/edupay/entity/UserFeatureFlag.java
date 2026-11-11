package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
    name = "user_feature_flags",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_feature", columnNames = {"user_id", "feature_id"})
    },
    indexes = {
        @Index(name = "idx_user_feature_flags_user", columnList = "user_id"),
        @Index(name = "idx_user_feature_flags_tenant", columnList = "tenant_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFeatureFlag extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "feature_id", nullable = false, length = 100)
    private String featureId;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(length = 255)
    private String reason;
}

