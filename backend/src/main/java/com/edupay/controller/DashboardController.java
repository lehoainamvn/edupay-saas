package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.response.DashboardSummaryResponse;
import com.edupay.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Bảng điều hành (Dashboard)", description = "API tổng hợp dữ liệu KPI, lịch dạy hôm nay, đối soát VietQR và danh sách nợ học phí cho giáo viên")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Lấy dữ liệu tổng quan Dashboard", description = "Truy vấn dữ liệu thực tế từ cơ sở dữ liệu theo tenant_id của giáo viên đang đăng nhập")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        DashboardSummaryResponse data = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy dữ liệu Dashboard thành công"));
    }
}

