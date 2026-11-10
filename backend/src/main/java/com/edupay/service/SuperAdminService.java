package com.edupay.service;

import com.edupay.dto.request.TenantExtendRequest;
import com.edupay.dto.response.PlatformStatsResponse;
import com.edupay.dto.response.TenantAdminResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý nền tảng SuperAdmin: Quản lý danh sách Tenant, Gia hạn gói, Thống kê MRR.
 */
public interface SuperAdminService {

    List<TenantAdminResponse> getAllTenants(String status, String search);

    TenantAdminResponse updateTenantStatus(UUID tenantId, String newStatus);

    TenantAdminResponse extendTenantSubscription(UUID tenantId, TenantExtendRequest request);

    PlatformStatsResponse getPlatformStats();
}
