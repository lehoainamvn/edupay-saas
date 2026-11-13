package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.UpdateSystemParameterRequest;
import com.edupay.dto.response.SystemParameterResponse;
import com.edupay.service.SystemParameterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/settings/parameters")
@RequiredArgsConstructor
@Tag(name = "Cấu hình Tham số Động (System Parameters)", description = "Quản lý các tham số hệ thống, học phí và thông báo với cơ chế In-memory Cache & Hot-reload")
public class SystemParameterController {

    private final SystemParameterService systemParameterService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tham số cấu hình", description = "Lọc theo nhóm (SYSTEM, TUITION, NOTIFICATION) và tìm kiếm từ khóa")
    public ResponseEntity<ApiResponse<List<SystemParameterResponse>>> getAllParameters(
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String keyword) {
        List<SystemParameterResponse> result = systemParameterService.getAllParameters(group, keyword);
        return ResponseEntity.ok(ApiResponse.ok(result, "Lấy danh sách tham số thành công"));
    }

    @GetMapping("/{key}")
    @Operation(summary = "Lấy chi tiết tham số theo mã", description = "Truy vấn thông tin và giá trị của một tham số cụ thể")
    public ResponseEntity<ApiResponse<SystemParameterResponse>> getParameterByKey(@PathVariable String key) {
        SystemParameterResponse result = systemParameterService.getParameterByKey(key);
        return ResponseEntity.ok(ApiResponse.ok(result, "Lấy chi tiết tham số thành công"));
    }

    @PostMapping
    @Operation(summary = "Tạo tham số cấu hình mới", description = "Thêm tham số tùy biến vào hệ thống và nạp ngay vào In-memory Cache")
    public ResponseEntity<ApiResponse<SystemParameterResponse>> createParameter(
            @Valid @RequestBody com.edupay.dto.request.CreateSystemParameterRequest request) {
        SystemParameterResponse result = systemParameterService.createParameter(request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Tạo tham số cấu hình thành công"));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Cập nhật giá trị tham số cấu hình", description = "Cập nhật và tự động Hot-reload In-memory Cache tức thì (không cần restart server)")
    public ResponseEntity<ApiResponse<SystemParameterResponse>> updateParameter(
            @PathVariable String key,
            @Valid @RequestBody UpdateSystemParameterRequest request) {
        SystemParameterResponse result = systemParameterService.updateParameter(key, request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Cập nhật tham số thành công"));
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Xóa tham số cấu hình tùy biến", description = "Chỉ cho phép xóa tham số do người dùng tạo thêm (không thể xóa tham số hệ thống isSystem)")
    public ResponseEntity<ApiResponse<Void>> deleteParameter(@PathVariable String key) {
        systemParameterService.deleteParameter(key);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa tham số cấu hình thành công"));
    }

    @PostMapping("/reload-cache")
    @Operation(summary = "Làm mới In-memory Cache", description = "Đồng bộ lại toàn bộ tham số từ cơ sở dữ liệu vào bộ nhớ đệm RAM")
    public ResponseEntity<ApiResponse<Void>> reloadCache() {
        systemParameterService.reloadCache();
        return ResponseEntity.ok(ApiResponse.ok(null, "Làm mới In-memory Cache thành công"));
    }

    @PostMapping("/reset-defaults")
    @Operation(summary = "Khôi phục tham số mặc định", description = "Đưa toàn bộ tham số về giá trị ban đầu được hệ thống đề xuất")
    public ResponseEntity<ApiResponse<Void>> resetDefaults() {
        systemParameterService.resetToDefaults();
        return ResponseEntity.ok(ApiResponse.ok(null, "Khôi phục tham số mặc định thành công"));
    }
}

