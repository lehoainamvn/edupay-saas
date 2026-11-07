package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // e.g. "class:read", "attendance:create"

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "feature_id", length = 100)
    private String featureId;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}

