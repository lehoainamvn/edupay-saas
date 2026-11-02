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
@Schema(description = "Thông tin điểm danh của học sinh trong buổi học ngày")
public class DailyAttendanceItemResponse {

    @Schema(description = "ID học sinh")
    private UUID studentId;

    @Schema(description = "Họ và tên học sinh")
    private String fullName;

    @Schema(description = "Tên ở nhà / Biệt danh")
    private String nickname;

    @Schema(description = "Số điện thoại phụ huynh")
    private String parentPhone;

    @Schema(description = "ID bản ghi điểm danh (null nếu chưa lưu)")
    private UUID attendanceId;

    @Schema(description = "Trạng thái điểm danh (null nếu chưa điểm danh)")
    private AttendanceStatus status;

    @Schema(description = "Ghi chú buổi học")
    private String note;

    @Schema(description = "Cờ khóa (true nếu kỳ học của ngày này đã xuất phiếu thu)")
    private boolean isLocked;
}

