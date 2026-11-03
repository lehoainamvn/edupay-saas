package com.edupay.service;

import com.edupay.dto.request.AttendanceBatchUpsertRequest;
import com.edupay.dto.request.AttendanceUpdateRequest;
import com.edupay.dto.response.AttendanceResponse;
import com.edupay.dto.response.DailyAttendanceResponse;
import com.edupay.dto.response.MonthlyAttendanceMatrixResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service quản lý nghiệp vụ Điểm danh và Lịch sử buổi học (UC-13, UC-14, UC-15).
 */
public interface AttendanceService {

    /**
     * Lấy dữ liệu điểm danh theo ngày của một lớp học (UC-13).
     *
     * @param classId ID lớp học
     * @param date    Ngày học (YYYY-MM-DD), nếu null thì mặc định hôm nay
     * @return Dữ liệu điểm danh của cả lớp kèm cờ khóa
     */
    DailyAttendanceResponse getDailyAttendance(UUID classId, LocalDate date);

    /**
     * Lưu hoặc cập nhật hàng loạt (Batch Upsert) điểm danh cho cả lớp trong 1 buổi học (UC-13).
     *
     * @param request Payload chứa classId, lessonDate và danh sách điểm danh từng học sinh
     * @return Dữ liệu điểm danh sau khi cập nhật
     */
    DailyAttendanceResponse batchUpsertAttendance(AttendanceBatchUpsertRequest request);

    /**
     * Lấy ma trận lịch sử điểm danh và tổng hợp số buổi học theo tháng của một lớp học (UC-14, UC-15).
     *
     * @param classId ID lớp học
     * @param month   Tháng khảo sát theo định dạng YYYY-MM (VD: 2026-10)
     * @return Ma trận lịch sử và thống kê số buổi từng học sinh
     */
    MonthlyAttendanceMatrixResponse getMonthlyAttendanceMatrix(UUID classId, String month);

    /**
     * Chỉnh sửa lẻ một bản ghi điểm danh cụ thể (UC-14).
     *
     * @param id      ID bản ghi điểm danh
     * @param request Thông tin cập nhật (status, note)
     * @return Bản ghi sau khi cập nhật
     */
    AttendanceResponse updateAttendance(UUID id, AttendanceUpdateRequest request);

    /**
     * Xóa toàn bộ buổi điểm danh của lớp trong 1 ngày (dùng khi hủy/xóa buổi học điểm danh nhầm).
     *
     * @param classId ID lớp học
     * @param date    Ngày học cần hủy điểm danh
     */
    void deleteDailyAttendance(UUID classId, LocalDate date);
}

