package com.edupay.dto.request;

import com.edupay.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin điểm danh của 1 học sinh trong buổi học")
public class AttendanceItemRequest {

    @NotNull(message = "ID học sinh không được để trống")
    @Schema(description = "ID học sinh", example = "e0000000-0000-0000-0000-000000000001")
    private UUID studentId;

    @NotNull(message = "Trạng thái điểm danh không được để trống")
    @Schema(description = "Trạng thái điểm danh (PRESENT: Có mặt, EXCUSED: Nghỉ có phép, ABSENT: Nghỉ không phép)", example = "PRESENT")
    private AttendanceStatus status;

    @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
    @Schema(description = "Ghi chú buổi học", example = "Bị sốt nhẹ")
    private String note;
}

