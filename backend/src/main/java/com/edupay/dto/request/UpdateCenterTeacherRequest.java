package com.edupay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cập nhật thông tin giáo viên trong Trung tâm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu chỉnh sửa thông tin giáo viên trong trung tâm")
public class UpdateCenterTeacherRequest {

    @NotBlank(message = "Họ tên giáo viên không được để trống")
    @Size(min = 2, max = 255, message = "Họ tên phải từ 2 đến 255 ký tự")
    @Schema(description = "Họ và tên giáo viên", example = "Nguyễn Thu Hà")
    private String fullName;

    @Schema(description = "Email liên hệ", example = "ha.nguyen@anhduong.edu.vn")
    private String email;

    @Schema(description = "Trạng thái tài khoản (ACTIVE / SUSPENDED)", example = "ACTIVE")
    private String status;
}

