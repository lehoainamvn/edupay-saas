package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "menus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // e.g. "DASHBOARD", "CLASSES", "SYSTEM_CONFIG"

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String icon;

    @Column(length = 200)
    private String route; // e.g. "/dashboard", "/system-config"

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "feature_id", length = 100)
    private String featureId; // NULL = Always available if permitted

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}

