package com.edupay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private UUID id;
    private UUID tenantId;
    private String tenantName;
    private String phone;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String status; // ACTIVE, SUSPENDED
    private Boolean isOwner;
    private List<String> roles;
    private Instant lastLoginAt;
    private Instant createdAt;
}
