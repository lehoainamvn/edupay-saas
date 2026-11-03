package com.edupay.dto.response;

import com.edupay.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chi tiết một buổi điểm danh trong ma trận tháng")
public class AttendanceRecordDto {

    @Schema(description = "ID bản ghi điểm danh")
    private UUID attendanceId;

    @Schema(description = "Trạng thái điểm danh (PRESENT, EXCUSED, ABSENT)")
    private AttendanceStatus status;

    @Schema(description = "Ghi chú buổi học")
    private String note;
}

