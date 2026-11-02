package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.AttendanceBatchUpsertRequest;
import com.edupay.dto.request.AttendanceUpdateRequest;
import com.edupay.dto.response.AttendanceResponse;
import com.edupay.dto.response.DailyAttendanceResponse;
import com.edupay.dto.response.MonthlyAttendanceMatrixResponse;
import com.edupay.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST Controller cung cấp các API Quản lý Điểm danh và Lịch sử buổi học (UC-13, UC-14, UC-15).
 * Tuân thủ nghiêm ngặt nguyên tắc kiến trúc: chỉ xử lý HTTP & Validation (@Valid).
 */
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Tag(name = "Quản lý Điểm danh (Attendance)", description = "API Điểm danh 1 chạm hàng ngày, Batch Upsert cả lớp, Ma trận lịch sử và Thống kê số buổi theo tháng")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/classes/{classId}/daily")
    @Operation(summary = "Lấy dữ liệu điểm danh theo ngày của một lớp học (UC-13)",
            description = "Trả về danh sách học sinh đang học trong lớp kèm trạng thái điểm danh và cờ khóa (isLocked). Mặc định lấy ngày hôm nay nếu không truyền param date.")
    public ResponseEntity<ApiResponse<DailyAttendanceResponse>> getDailyAttendance(
            @PathVariable UUID classId,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyAttendanceResponse data = attendanceService.getDailyAttendance(classId, date);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách điểm danh ngày thành công"));
    }

    @PostMapping("/batch")
    @Operation(summary = "Lưu hoặc cập nhật điểm danh hàng loạt cho cả lớp trong 1 request (UC-13)",
            description = "Hỗ trợ Batch Upsert toàn bộ học sinh trong lớp. Tự động kiểm tra cờ khóa đối soát (chặn sửa nếu học sinh đã xuất phiếu thu trong kỳ).")
    public ResponseEntity<ApiResponse<DailyAttendanceResponse>> batchUpsertAttendance(
            @Valid @RequestBody AttendanceBatchUpsertRequest request) {
        DailyAttendanceResponse data = attendanceService.batchUpsertAttendance(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(data, "Lưu điểm danh buổi học thành công"));
    }

    @GetMapping("/classes/{classId}/monthly")
    @Operation(summary = "Lấy ma trận lịch sử điểm danh và tổng hợp số buổi học theo tháng (UC-14, UC-15)",
            description = "Dữ liệu dạng ma trận: Danh sách các ngày học trong tháng và bảng chi tiết từng học sinh kèm tổng số buổi có mặt, nghỉ có phép, không phép.")
    public ResponseEntity<ApiResponse<MonthlyAttendanceMatrixResponse>> getMonthlyAttendanceMatrix(
            @PathVariable UUID classId,
            @RequestParam(name = "month") String month) {
        MonthlyAttendanceMatrixResponse data = attendanceService.getMonthlyAttendanceMatrix(classId, month);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy ma trận lịch sử điểm danh tháng thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Chỉnh sửa lẻ 1 bản ghi điểm danh cụ thể (UC-14)",
            description = "Cho phép cập nhật trạng thái hoặc ghi chú của 1 buổi học. Chặn chỉnh sửa nếu kỳ học đã xuất phiếu thu.")
    public ResponseEntity<ApiResponse<AttendanceResponse>> updateAttendance(
            @PathVariable UUID id,
            @Valid @RequestBody AttendanceUpdateRequest request) {
        AttendanceResponse data = attendanceService.updateAttendance(id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật điểm danh thành công"));
    }

    @DeleteMapping("/classes/{classId}/daily")
    @Operation(summary = "Hủy toàn bộ lượt điểm danh của 1 buổi học (dành cho trường hợp điểm danh nhầm ngày)",
            description = "Xóa toàn bộ bản ghi điểm danh của lớp trong ngày được chọn. Chặn xóa nếu kỳ học đã có phiếu thu.")
    public ResponseEntity<ApiResponse<Void>> deleteDailyAttendance(
            @PathVariable UUID classId,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        attendanceService.deleteDailyAttendance(classId, date);
        return ResponseEntity.ok(ApiResponse.ok(null, "Hủy buổi điểm danh thành công"));
    }
}

