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
@Schema(description = "Ma trận lịch sử điểm danh và tổng hợp buổi học theo tháng của lớp học")
public class MonthlyAttendanceMatrixResponse {

    @Schema(description = "ID lớp học")
    private UUID classId;

    @Schema(description = "Tên lớp học")
    private String className;

    @Schema(description = "Tháng khảo sát (YYYY-MM)", example = "2026-10")
    private String month;

    @Schema(description = "Danh sách các ngày đã tổ chức học trong tháng (sắp xếp tăng dần)")
    private List<LocalDate> lessonDates;

    @Schema(description = "Tổng số buổi lớp đã học trong tháng")
    private int totalClassLessons;

    @Schema(description = "Danh sách học sinh kèm số liệu tổng hợp và ma trận điểm danh")
    private List<StudentMonthlyAttendanceDto> students;
}

