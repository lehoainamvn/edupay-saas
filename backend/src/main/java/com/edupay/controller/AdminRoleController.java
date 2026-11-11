package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.UpdateRolePermissionsRequest;
import com.edupay.dto.response.PermissionGroupResponse;
import com.edupay.dto.response.RoleResponse;
import com.edupay.service.AdminRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/admin/roles", "/api/admin/roles"})
@RequiredArgsConstructor
@Tag(name = "SuperAdmin RBAC", description = "Quản lý vai trò và ma trận phân quyền trên toàn hệ thống")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các vai trò (Roles) trong hệ thống",
            description = "Trả về danh sách Roles kèm số lượng người dùng và danh sách mã quyền đã cấp")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        List<RoleResponse> data = adminRoleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách vai trò thành công"));
    }

    @GetMapping("/permissions")
    @Operation(summary = "Lấy toàn bộ quyền hạn theo nhóm nghiệp vụ (Permission Matrix)",
            description = "Gom nhóm theo Lớp học, Học sinh, Điểm danh, Học phí, Cài đặt, v.v.")
    public ResponseEntity<ApiResponse<List<PermissionGroupResponse>>> getAllPermissionsGrouped() {
        List<PermissionGroupResponse> data = adminRoleService.getAllPermissionsGrouped();
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh mục quyền hạn thành công"));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Cập nhật ma trận quyền hạn cho một vai trò")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRolePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        RoleResponse data = adminRoleService.updateRolePermissions(id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật phân quyền vai trò thành công"));
    }
}

