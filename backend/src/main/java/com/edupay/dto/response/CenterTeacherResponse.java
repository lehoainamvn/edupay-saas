package com.edupay.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO thông tin giáo viên trực thuộc trung tâm (Center Teacher).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin giáo viên trực thuộc trung tâm")
public class CenterTeacherResponse {

    @Schema(description = "ID giáo viên")
    private UUID id;

    @Schema(description = "Họ và tên giáo viên", example = "Nguyễn Thu Hà")
    private String fullName;

    @Schema(description = "Số điện thoại đăng nhập", example = "0901112223")
    private String phone;

    @Schema(description = "Email liên hệ", example = "ha.nguyen@anhduong.edu.vn")
    private String email;

    @Schema(description = "Trạng thái tài khoản (ACTIVE / SUSPENDED)", example = "ACTIVE")
    private String status;

    @Schema(description = "Số lớp học đang được phân công phụ trách", example = "2")
    private long assignedClassesCount;

    @Schema(description = "Ngày tham gia trung tâm")
    private Instant createdAt;

    @Schema(description = "Thời điểm đăng nhập gần nhất")
    private Instant lastLoginAt;
}

