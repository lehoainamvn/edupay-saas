package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.TenantExtendRequest;
import com.edupay.dto.response.PlatformStatsResponse;
import com.edupay.dto.response.TenantAdminResponse;
import com.edupay.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/admin", "/api/admin"})
@RequiredArgsConstructor
@Tag(name = "SuperAdmin Platform", description = "Quản lý danh sách giáo viên thuê nền tảng, gia hạn gói dịch vụ, khóa/mở khóa tenant và thống kê MRR")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/tenants")
    @Operation(summary = "Lấy danh sách giáo viên thuê hệ thống (Tenants)",
            description = "Hỗ trợ lọc theo trạng thái (ACTIVE, SUSPENDED, EXPIRED, ALL) và tìm kiếm theo tên, SĐT")
    public ResponseEntity<ApiResponse<List<TenantAdminResponse>>> getTenants(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search) {
        List<TenantAdminResponse> data = superAdminService.getAllTenants(status, search);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách giáo viên thành công"));
    }

    @PatchMapping("/tenants/{id}/status")
    @Operation(summary = "Khóa hoặc mở khóa tài khoản giáo viên",
            description = "Cập nhật subscription_status (ACTIVE: Mở khóa, SUSPENDED: Tạm khóa)")
    public ResponseEntity<ApiResponse<TenantAdminResponse>> updateTenantStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String newStatus = body.getOrDefault("status", "ACTIVE");
        TenantAdminResponse data = superAdminService.updateTenantStatus(id, newStatus);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật trạng thái tài khoản thành công"));
    }

    @PostMapping("/tenants/{id}/extend")
    @Operation(summary = "Gia hạn gói dịch vụ cho giáo viên",
            description = "Cộng dồn thời hạn sử dụng (+1 tháng: 30 ngày hoặc +1 năm: 365 ngày)")
    public ResponseEntity<ApiResponse<TenantAdminResponse>> extendSubscription(
            @PathVariable UUID id,
            @Valid @RequestBody TenantExtendRequest request) {
        TenantAdminResponse data = superAdminService.extendTenantSubscription(id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Gia hạn gói dịch vụ thành công"));
    }

    @GetMapping("/platform/stats")
    @Operation(summary = "Thống kê chỉ số kinh doanh nền tảng (MRR, Tăng trưởng)",
            description = "Tính toán Monthly Recurring Revenue, số giáo viên mới trong tháng, tỷ lệ gia hạn")
    public ResponseEntity<ApiResponse<PlatformStatsResponse>> getPlatformStats() {
        PlatformStatsResponse data = superAdminService.getPlatformStats();
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thống kê nền tảng thành công"));
    }
}
