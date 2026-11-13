package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.CreateCenterRequest;
import com.edupay.dto.request.UpdateCenterRequest;
import com.edupay.dto.response.CenterTeacherResponse;
import com.edupay.dto.response.TenantAdminResponse;
import com.edupay.service.AdminCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/admin/centers", "/api/admin/centers"})
@RequiredArgsConstructor
@Tag(name = "SuperAdmin Quản lý Trung tâm", description = "CRUD các Trung tâm đào tạo, quản lý thông tin chủ cơ sở và xem danh sách giáo viên")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminCenterController {

    private final AdminCenterService adminCenterService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các Trung tâm đào tạo", description = "Hỗ trợ lọc theo trạng thái và tìm kiếm tên, SĐT")
    public ResponseEntity<ApiResponse<List<TenantAdminResponse>>> getAllCenters(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search) {
        List<TenantAdminResponse> data = adminCenterService.getAllCenters(status, search);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách Trung tâm thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết thông tin một Trung tâm")
    public ResponseEntity<ApiResponse<TenantAdminResponse>> getCenterById(@PathVariable UUID id) {
        TenantAdminResponse data = adminCenterService.getCenterById(id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin Trung tâm thành công"));
    }

    @PostMapping
    @Operation(summary = "Tạo mới Trung tâm đào tạo", description = "Tự động sinh tài khoản Chủ trung tâm và thiết lập chính sách mặc định")
    public ResponseEntity<ApiResponse<TenantAdminResponse>> createCenter(@Valid @RequestBody CreateCenterRequest request) {
        TenantAdminResponse data = adminCenterService.createCenter(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Tạo Trung tâm mới thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin Trung tâm")
    public ResponseEntity<ApiResponse<TenantAdminResponse>> updateCenter(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCenterRequest request) {
        TenantAdminResponse data = adminCenterService.updateCenter(id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật thông tin Trung tâm thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa Trung tâm")
    public ResponseEntity<ApiResponse<Void>> deleteCenter(@PathVariable UUID id) {
        adminCenterService.deleteCenter(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã xóa Trung tâm thành công"));
    }

    @GetMapping("/{id}/teachers")
    @Operation(summary = "Xem danh sách giáo viên thuộc Trung tâm")
    public ResponseEntity<ApiResponse<List<CenterTeacherResponse>>> getCenterTeachers(@PathVariable UUID id) {
        List<CenterTeacherResponse> data = adminCenterService.getCenterTeachers(id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách giáo viên của Trung tâm thành công"));
    }
}

