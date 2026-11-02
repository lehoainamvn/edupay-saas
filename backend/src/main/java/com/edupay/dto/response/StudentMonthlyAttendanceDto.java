package com.edupay.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thống kê tổng hợp số buổi học và chi tiết điểm danh theo tháng của một học sinh")
public class StudentMonthlyAttendanceDto {

    @Schema(description = "ID học sinh")
    private UUID studentId;

    @Schema(description = "Họ và tên học sinh")
    private String fullName;

    @Schema(description = "Tên ở nhà / Biệt danh")
    private String nickname;

    @Schema(description = "Số buổi có mặt (dùng trực tiếp cho công thức tính học phí)")
    private int attendedCount;

    @Schema(description = "Số buổi nghỉ có phép")
    private int excusedCount;

    @Schema(description = "Số buổi nghỉ không phép")
    private int absentCount;

    @Schema(description = "Tổng số buổi đã ghi nhận điểm danh trong tháng của học sinh")
    private int totalLessons;

    @Schema(description = "Cờ khóa (true nếu kỳ học này đã xuất phiếu thu)")
    private boolean isLocked;

    @Schema(description = "Bản đồ ngày học -> chi tiết buổi học (Key: YYYY-MM-DD)")
    private Map<LocalDate, AttendanceRecordDto> dailyRecords;
}

