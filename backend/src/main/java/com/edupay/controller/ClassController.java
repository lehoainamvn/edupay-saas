package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.ClassRequest;
import com.edupay.dto.response.ClassResponse;
import com.edupay.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cung cấp CRUD API Quản lý Lớp học.
 * Chỉ xử lý HTTP & Validation (@Valid), tuyệt đối không chứa logic nghiệp vụ.
 */
@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
@Tag(name = "Quản lý Lớp học (Classes)", description = "API tạo mới, chỉnh sửa, truy vấn và xóa mềm (soft-delete) lớp học theo từng giáo viên (Multi-Tenant)")
public class ClassController {

    private final ClassService classService;

    @GetMapping
    @Operation(summary = "Lấy danh sách lớp học của giáo viên", description = "Tự động lọc theo tenant_id của giáo viên đang đăng nhập. Có thể truyền includeArchived=true để xem cả lớp đã lưu trữ.")
    public ResponseEntity<ApiResponse<List<ClassResponse>>> getClasses(
            @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived) {
        List<ClassResponse> data = classService.getClasses(includeArchived);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách lớp học thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết 1 lớp học theo ID", description = "Chỉ trả về lớp học nếu thuộc sở hữu của giáo viên hiện tại (tenant_id)")
    public ResponseEntity<ApiResponse<ClassResponse>> getClassById(@PathVariable UUID id) {
        ClassResponse data = classService.getClassById(id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin lớp học thành công"));
    }

    @PostMapping
    @Operation(summary = "Tạo lớp học mới", description = "Tạo mới lớp học với tên lớp, hình thức thu phí (PER_LESSON / MONTHLY_PACKAGE) và đơn giá mặc định")
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(
            @Valid @RequestBody ClassRequest request) {
        ClassResponse data = classService.createClass(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Tạo lớp học mới thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Chỉnh sửa thông tin lớp học", description = "Cập nhật tên lớp, lịch học/mô tả, hình thức thu học phí và đơn giá mặc định")
    public ResponseEntity<ApiResponse<ClassResponse>> updateClass(
            @PathVariable UUID id,
            @Valid @RequestBody ClassRequest request) {
        ClassResponse data = classService.updateClass(id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật thông tin lớp học thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mềm (Lưu trữ) lớp học", description = "Thực hiện Soft Delete (is_active = false) để bảo toàn lịch sử điểm danh và hóa đơn học phí của lớp")
    public ResponseEntity<ApiResponse<Void>> softDeleteClass(@PathVariable UUID id) {
        classService.softDeleteClass(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã lưu trữ lớp học thành công (giữ nguyên lịch sử điểm danh & học phí)"));
    }

    @PatchMapping("/{id}/restore")
    @Operation(summary = "Khôi phục lớp học đã lưu trữ", description = "Mở lại lớp học đã xóa mềm (is_active = true)")
    public ResponseEntity<ApiResponse<ClassResponse>> restoreClass(@PathVariable UUID id) {
        ClassResponse data = classService.restoreClass(id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Khôi phục lớp học thành công"));
    }
}
