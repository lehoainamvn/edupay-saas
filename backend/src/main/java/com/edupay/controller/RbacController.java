package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.OverrideFeatureRequest;
import com.edupay.dto.response.FeatureFlagResponse;
import com.edupay.dto.response.UserNavigationResponse;
import com.edupay.service.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Phân quyền RBAC & Menu động", description = "Quản lý quyền hạn, Feature Flags và cây Menu động theo vai trò người dùng")
public class RbacController {

    private final RbacService rbacService;

    @GetMapping("/me/navigation")
    @Operation(summary = "Lấy dữ liệu điều hướng và phân quyền của User",
            description = "Trả về danh sách vai trò, danh mục quyền, feature flags và cây menu động tương ứng")
    public ResponseEntity<ApiResponse<UserNavigationResponse>> getCurrentUserNavigation() {
        UserNavigationResponse response = rbacService.getCurrentUserNavigation();
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy dữ liệu điều hướng thành công"));
    }

    @GetMapping("/features")
    @Operation(summary = "Lấy danh sách Feature Flags của trung tâm",
            description = "Xem trạng thái các tính năng theo gói cước hoặc được cấp riêng cho Tenant")
    public ResponseEntity<ApiResponse<List<FeatureFlagResponse>>> getTenantFeatureFlags() {
        List<FeatureFlagResponse> response = rbacService.getTenantFeatureFlags();
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy danh sách Feature Flags thành công"));
    }

    @PutMapping("/features/{featureId}/override")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Cấp hoặc thu hồi tính năng riêng cho trung tâm (Override)",
            description = "Chỉ dành cho Chủ tài khoản (Owner) để kích hoạt tính năng addon hoặc thử nghiệm beta")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> overrideTenantFeature(
            @PathVariable String featureId,
            @Valid @RequestBody OverrideFeatureRequest request) {
        FeatureFlagResponse response = rbacService.overrideTenantFeature(featureId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật Feature Flag thành công"));
    }
}

