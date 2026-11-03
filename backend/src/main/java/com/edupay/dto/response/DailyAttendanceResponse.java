package com.edupay.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu điểm danh của cả lớp theo ngày")
public class DailyAttendanceResponse {

    @Schema(description = "ID lớp học")
    private UUID classId;

    @Schema(description = "Tên lớp học")
    private String className;

    @Schema(description = "Ngày học (YYYY-MM-DD)")
    private LocalDate lessonDate;

    @Schema(description = "Tổng số học sinh đang học trong lớp")
    private int totalStudents;

    @Schema(description = "Số học sinh đã được điểm danh")
    private int markedCount;

    @Schema(description = "Số học sinh có mặt")
    private int presentCount;

    @Schema(description = "Số học sinh nghỉ có phép")
    private int excusedCount;

    @Schema(description = "Số học sinh nghỉ không phép")
    private int absentCount;

    @Schema(description = "Cờ khóa toàn bộ buổi học (true nếu toàn bộ học sinh đều đã xuất phiếu thu)")
    private boolean isClassLocked;

    @Schema(description = "Danh sách chi tiết điểm danh từng học sinh")
    private List<DailyAttendanceItemResponse> students;
}

