package com.edupay.service;

import com.edupay.dto.request.OverrideFeatureRequest;
import com.edupay.dto.response.FeatureFlagResponse;
import com.edupay.dto.response.UserNavigationResponse;

import java.util.List;

public interface RbacService {

    /**
     * Lấy toàn bộ cây Menu động, Roles, Permissions và Feature Flags của User đang đăng nhập.
     */
    UserNavigationResponse getCurrentUserNavigation();

    /**
     * Lấy danh sách toàn bộ tính năng và trạng thái Feature Flag hiện tại của Tenant.
     */
    List<FeatureFlagResponse> getTenantFeatureFlags();

    /**
     * Cấp hoặc thu hồi tính năng riêng biệt cho Tenant (Tenant Feature Flag Override).
     */
    FeatureFlagResponse overrideTenantFeature(String featureId, OverrideFeatureRequest request);
}

