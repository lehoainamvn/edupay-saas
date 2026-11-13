package com.edupay.service;

import com.edupay.dto.request.UpdateTenantTeacherPolicyRequest;
import com.edupay.dto.response.TenantTeacherPolicyDto;

import java.util.UUID;

public interface CenterPolicyService {

    TenantTeacherPolicyDto getTenantPolicy(UUID tenantId);

    TenantTeacherPolicyDto getMyPolicy();

    TenantTeacherPolicyDto updateTenantPolicy(UUID tenantId, UpdateTenantTeacherPolicyRequest request);
}

