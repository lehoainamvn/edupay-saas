package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.StudentRequest;
import com.edupay.dto.response.StudentImportResultResponse;
import com.edupay.dto.response.StudentResponse;
import com.edupay.entity.StudentStatus;
import com.edupay.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller cung cấp CRUD API Quản lý Học sinh & Import hàng loạt từ file Excel/CSV.
 * Tuân thủ nghiêm ngặt kiến trúc phân lớp: chỉ xử lý HTTP & Validation (@Valid).
 */
@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@Tag(name = "Quản lý Học sinh (Students)", description = "API thêm, sửa, xóa mềm, tìm kiếm học sinh và import hàng loạt từ Excel/CSV (Apache POI)")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "Lấy danh sách học sinh", description = "Tự động lọc theo tenant_id. Hỗ trợ lọc theo classId, status và tìm kiếm nhanh theo họ tên, biệt danh, SĐT phụ huynh.")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudents(
            @RequestParam(name = "classId", required = false) UUID classId,
            @RequestParam(name = "status", required = false) StudentStatus status,
            @RequestParam(name = "search", required = false) String search) {
        List<StudentResponse> data = studentService.getStudents(classId, status, search);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách học sinh thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết 1 học sinh theo ID", description = "Chỉ trả về nếu học sinh thuộc quyền quản lý của giáo viên hiện tại (tenant_id)")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentById(@PathVariable UUID id) {
        StudentResponse data = studentService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin học sinh thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm học sinh mới vào lớp", description = "Validate định dạng SĐT phụ huynh 10 số và kiểm tra chống trùng trong lớp")
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody StudentRequest request) {
        StudentResponse data = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Thêm học sinh mới thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin học sinh", description = "Cập nhật họ tên, biệt danh ở nhà, SĐT phụ huynh, học phí riêng và lớp học")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable UUID id,
            @Valid @RequestBody StudentRequest request) {
        StudentResponse data = studentService.updateStudent(id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật thông tin học sinh thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mềm (Chuyển nghỉ học) học sinh", description = "Chuyển trạng thái sang DROPPED để giữ nguyên lịch sử điểm danh và hóa đơn học phí")
    public ResponseEntity<ApiResponse<Void>> softDeleteStudent(@PathVariable UUID id) {
        studentService.softDeleteStudent(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã chuyển học sinh sang trạng thái nghỉ học (bảo toàn lịch sử điểm danh & học phí)"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Đổi trạng thái học tập của học sinh", description = "Chuyển đổi trạng thái giữa STUDYING (Đang học), PAUSED (Tạm nghỉ), DROPPED (Đã nghỉ)")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudentStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        StudentStatus newStatus = StudentStatus.valueOf(body.getOrDefault("status", "STUDYING"));
        StudentResponse data = studentService.updateStudentStatus(id, newStatus);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật trạng thái học sinh thành công"));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import danh sách học sinh hàng loạt từ file Excel / CSV", description = "Sử dụng Apache POI đọc file .xlsx/.xls hoặc .csv, validate SĐT phụ huynh và kiểm tra trùng trong lớp")
    public ResponseEntity<ApiResponse<StudentImportResultResponse>> importStudents(
            @RequestParam("classId") UUID classId,
            @RequestParam("file") MultipartFile file) {
        StudentImportResultResponse result = studentService.importStudentsFromFile(classId, file);
        String msg = String.format("Import hoàn tất: thêm thành công %d/%d học sinh", result.getImportedCount(), result.getTotalRows());
        return ResponseEntity.ok(ApiResponse.ok(result, msg));
    }

    @GetMapping("/import/template")
    @Operation(summary = "Tải file Excel mẫu (.xlsx) để import học sinh", description = "Trả về file Excel mẫu tạo bởi Apache POI có sẵn các cột chuẩn và dữ liệu ví dụ")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] bytes = studentService.generateImportTemplateExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Mau_Import_Hoc_Sinh_EduPay.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
