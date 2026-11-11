package com.edupay.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlagResponse {
    private String featureId;
    private String name;
    private String description;
    private String category; // CORE, ADDON, PREMIUM
    private boolean enabled;
    private String source; // PLAN, TENANT_OVERRIDE, USER_OVERRIDE
    private String reason;
    private Instant expiresAt;
}

