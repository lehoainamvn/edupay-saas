package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.CreateUserRequest;
import com.edupay.dto.request.ResetPasswordRequest;
import com.edupay.dto.request.UpdateUserRequest;
import com.edupay.dto.response.AdminUserResponse;
import com.edupay.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/admin/users", "/api/admin/users"})
@RequiredArgsConstructor
@Tag(name = "SuperAdmin Users", description = "Quản lý toàn diện tài khoản người dùng trên toàn hệ thống SaaS")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Lấy danh sách người dùng toàn hệ thống",
            description = "Hỗ trợ phân trang, tìm kiếm theo tên/SĐT/email, lọc theo role, status, tenant")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "tenantId", required = false) UUID tenantId) {
        Page<AdminUserResponse> data = adminUserService.getAllUsers(page, size, search, role, status, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách người dùng thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết thông tin người dùng")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserById(@PathVariable UUID id) {
        AdminUserResponse data = adminUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin người dùng thành công"));
    }

    @PostMapping
    @Operation(summary = "Tạo tài khoản người dùng mới")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        AdminUserResponse data = adminUserService.createUser(request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Tạo tài khoản người dùng mới thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin và vai trò người dùng")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        AdminUserResponse data = adminUserService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật người dùng thành công"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Khóa hoặc Mở khóa tài khoản người dùng")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String newStatus = body.getOrDefault("status", "ACTIVE");
        AdminUserResponse data = adminUserService.updateUserStatus(id, newStatus);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật trạng thái tài khoản thành công"));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Đặt lại mật khẩu cho người dùng")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetPasswordRequest request) {
        adminUserService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đặt lại mật khẩu thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa tài khoản người dùng")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa tài khoản người dùng thành công"));
    }
}
