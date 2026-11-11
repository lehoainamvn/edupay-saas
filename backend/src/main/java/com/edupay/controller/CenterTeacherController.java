package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.CreateCenterTeacherRequest;
import com.edupay.dto.request.UpdateCenterTeacherRequest;
import com.edupay.dto.response.CenterTeacherResponse;
import com.edupay.service.CenterTeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/center/teachers", "/api/center/teachers"})
@RequiredArgsConstructor
@Tag(name = "Center Teacher Management", description = "Quản lý đội ngũ giáo viên trực thuộc trung tâm (Chỉ dành cho Quản lý Trung tâm)")
public class CenterTeacherController {

    private final CenterTeacherService centerTeacherService;

    @GetMapping
    @Operation(summary = "Lấy danh sách giáo viên thuộc trung tâm",
            description = "Chỉ trả về các giáo viên trực thuộc Tenant hiện tại của Quản lý trung tâm")
    public ResponseEntity<ApiResponse<List<CenterTeacherResponse>>> getTeachers() {
        List<CenterTeacherResponse> data = centerTeacherService.getTeachers();
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách giáo viên trung tâm thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết giáo viên trong trung tâm")
    public ResponseEntity<ApiResponse<CenterTeacherResponse>> getTeacherById(@PathVariable UUID id) {
        CenterTeacherResponse data = centerTeacherService.getTeacherById(id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin giáo viên thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm giáo viên mới vào trung tâm")
    public ResponseEntity<ApiResponse<CenterTeacherResponse>> createTeacher(
            @Valid @RequestBody CreateCenterTeacherRequest request) {
        CenterTeacherResponse data = centerTeacherService.createTeacher(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Thêm giáo viên mới vào trung tâm thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin giáo viên")
    public ResponseEntity<ApiResponse<CenterTeacherResponse>> updateTeacher(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCenterTeacherRequest request) {
        CenterTeacherResponse data = centerTeacherService.updateTeacher(id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật giáo viên thành công"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Khóa hoặc mở khóa giáo viên")
    public ResponseEntity<ApiResponse<CenterTeacherResponse>> updateTeacherStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "ACTIVE");
        CenterTeacherResponse data = centerTeacherService.updateTeacherStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật trạng thái giáo viên thành công"));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Đặt lại mật khẩu cho giáo viên")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String newPassword = body.getOrDefault("newPassword", "Admin@123");
        centerTeacherService.resetPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đặt lại mật khẩu cho giáo viên thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa giáo viên khỏi trung tâm")
    public ResponseEntity<ApiResponse<Void>> deleteTeacher(@PathVariable UUID id) {
        centerTeacherService.deleteTeacher(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa giáo viên thành công"));
    }
}
