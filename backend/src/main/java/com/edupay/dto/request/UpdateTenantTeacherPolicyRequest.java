package com.edupay.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantTeacherPolicyRequest {
    private Boolean canEditStudentFee;
    private Boolean canCreateClass;
    private Boolean canCreateStudent;
    private Boolean canViewBilling;
}

