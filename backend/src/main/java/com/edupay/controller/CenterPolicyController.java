package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.UpdateTenantTeacherPolicyRequest;
import com.edupay.dto.response.TenantTeacherPolicyDto;
import com.edupay.config.TenantContext;
import com.edupay.service.CenterPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/center/policies", "/api/center/policies"})
@RequiredArgsConstructor
@Tag(name = "Trung tâm: Cấu hình Phân quyền Giáo viên nội bộ", description = "Cho phép Quản lý Trung tâm bật/tắt quyền sửa học phí, tạo lớp, thêm học sinh cho giáo viên của mình")
public class CenterPolicyController {

    private final CenterPolicyService centerPolicyService;

    @GetMapping("/my-policy")
    @Operation(summary = "Lấy chính sách quyền hạn áp dụng cho tài khoản người dùng hiện tại")
    public ResponseEntity<ApiResponse<TenantTeacherPolicyDto>> getMyPolicy() {
        TenantTeacherPolicyDto data = centerPolicyService.getMyPolicy();
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy chính sách quyền hạn người dùng thành công"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CENTER_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Lấy chính sách phân quyền giáo viên hiện tại của trung tâm")
    public ResponseEntity<ApiResponse<TenantTeacherPolicyDto>> getTenantPolicy(
            @RequestParam(name = "tenantId", required = false) UUID paramTenantId) {
        UUID tenantId = (paramTenantId != null) ? paramTenantId : TenantContext.getTenantId();
        TenantTeacherPolicyDto data = centerPolicyService.getTenantPolicy(tenantId);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy chính sách giáo viên thành công"));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CENTER_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Cập nhật chính sách phân quyền giáo viên nội bộ của trung tâm")
    public ResponseEntity<ApiResponse<TenantTeacherPolicyDto>> updateTenantPolicy(
            @RequestParam(name = "tenantId", required = false) UUID paramTenantId,
            @RequestBody UpdateTenantTeacherPolicyRequest request) {
        UUID tenantId = (paramTenantId != null) ? paramTenantId : TenantContext.getTenantId();
        TenantTeacherPolicyDto data = centerPolicyService.updateTenantPolicy(tenantId, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật chính sách giáo viên thành công"));
    }
}
