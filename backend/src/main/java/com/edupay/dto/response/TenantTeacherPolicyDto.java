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
public class TenantTeacherPolicyDto {
    private UUID id;
    private UUID tenantId;
    private Boolean canEditStudentFee;
    private Boolean canCreateClass;
    private Boolean canCreateStudent;
    private Boolean canViewBilling;
    private Instant updatedAt;
}

