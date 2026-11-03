package com.edupay.dto.request;

import com.edupay.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu chỉnh sửa 1 bản ghi điểm danh cụ thể")
public class AttendanceUpdateRequest {

    @NotNull(message = "Trạng thái điểm danh không được để trống")
    @Schema(description = "Trạng thái điểm danh (PRESENT, EXCUSED, ABSENT)", example = "EXCUSED")
    private AttendanceStatus status;

    @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
    @Schema(description = "Ghi chú điểm danh", example = "Xin phép nghỉ đi khám bệnh")
    private String note;
}

