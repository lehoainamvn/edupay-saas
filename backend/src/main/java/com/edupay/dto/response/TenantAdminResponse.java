package com.edupay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAdminResponse {
    private UUID id;
    private String name;
    private String phone;
    private String email;
    private String tenantType; // CENTER hoặc SOLO_TUTOR
    private String subscriptionPlan;
    private String subscriptionStatus;
    private Instant subscriptionEndsAt;
    private Instant createdAt;
    private long totalTeachers;
    private long totalClasses;
    private long totalStudents;
    private boolean isExpired;
    private String representativeName;
    private String representativePhone;
}
