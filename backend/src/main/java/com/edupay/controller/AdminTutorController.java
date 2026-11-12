package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.CreateTutorRequest;
import com.edupay.dto.request.UpdateTutorRequest;
import com.edupay.dto.response.TenantAdminResponse;
import com.edupay.service.AdminTutorService;
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
@RequestMapping({"/admin/tutors", "/api/admin/tutors"})
@RequiredArgsConstructor
@Tag(name = "SuperAdmin Quản lý Gia sư Tự do", description = "CRUD các Gia sư độc lập, gói cước và trạng thái tài khoản")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminTutorController {

    private final AdminTutorService adminTutorService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các Gia sư tự do")
    public ResponseEntity<ApiResponse<List<TenantAdminResponse>>> getAllTutors(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search) {
        List<TenantAdminResponse> data = adminTutorService.getAllTutors(status, search);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách Gia sư tự do thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một Gia sư")
    public ResponseEntity<ApiResponse<TenantAdminResponse>> getTutorById(@PathVariable UUID id) {
        TenantAdminResponse data = adminTutorService.getTutorById(id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin Gia sư thành công"));
    }

    @PostMapping
    @Operation(summary = "Tạo mới tài khoản Gia sư tự do")
    public ResponseEntity<ApiResponse<TenantAdminResponse>> createTutor(@Valid @RequestBody CreateTutorRequest request) {
        TenantAdminResponse data = adminTutorService.createTutor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Tạo tài khoản Gia sư thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin Gia sư tự do")
    public ResponseEntity<ApiResponse<TenantAdminResponse>> updateTutor(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTutorRequest request) {
        TenantAdminResponse data = adminTutorService.updateTutor(id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật thông tin Gia sư thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa tài khoản Gia sư tự do")
    public ResponseEntity<ApiResponse<Void>> deleteTutor(@PathVariable UUID id) {
        adminTutorService.deleteTutor(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã xóa tài khoản Gia sư thành công"));
    }
}

