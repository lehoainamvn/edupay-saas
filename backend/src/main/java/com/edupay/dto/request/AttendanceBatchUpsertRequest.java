package com.edupay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Yêu cầu lưu/cập nhật điểm danh hàng loạt (Batch Upsert) cho cả lớp trong 1 buổi học")
public class AttendanceBatchUpsertRequest {

    @NotNull(message = "ID lớp học không được để trống")
    @Schema(description = "ID lớp học", example = "d0000000-0000-0000-0000-000000000001")
    private UUID classId;

    @NotNull(message = "Ngày học không được để trống")
    @Schema(description = "Ngày học (YYYY-MM-DD)", example = "2026-10-31")
    private LocalDate lessonDate;

    @NotEmpty(message = "Danh sách điểm danh không được để trống")
    @Size(max = 200, message = "Mỗi lượt điểm danh chỉ hỗ trợ tối đa 200 học sinh để đảm bảo an toàn hệ thống")
    @Valid
    @Schema(description = "Danh sách điểm danh từng học sinh (tối đa 200 học sinh)")
    private List<AttendanceItemRequest> items;
}
