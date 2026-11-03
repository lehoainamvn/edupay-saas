package com.edupay.dto.response;

import com.edupay.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết một bản ghi điểm danh")
public class AttendanceResponse {

    @Schema(description = "ID bản ghi điểm danh")
    private UUID id;

    @Schema(description = "ID lớp học")
    private UUID classId;

    @Schema(description = "ID học sinh")
    private UUID studentId;

    @Schema(description = "Ngày học (YYYY-MM-DD)")
    private LocalDate lessonDate;

    @Schema(description = "Trạng thái điểm danh (PRESENT, EXCUSED, ABSENT)")
    private AttendanceStatus status;

    @Schema(description = "Ghi chú buổi học")
    private String note;
}

