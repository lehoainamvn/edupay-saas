package com.edupay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO tạo mới tài khoản giáo viên cho Trung tâm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu thêm giáo viên mới vào trung tâm")
public class CreateCenterTeacherRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    @Schema(description = "Số điện thoại đăng nhập của giáo viên", example = "0901112229")
    private String phone;

    @NotBlank(message = "Họ tên giáo viên không được để trống")
    @Size(min = 2, max = 255, message = "Họ tên phải từ 2 đến 255 ký tự")
    @Schema(description = "Họ và tên giáo viên", example = "Vũ Thị Hương")
    private String fullName;

    @Schema(description = "Email liên hệ", example = "huong.vu@anhduong.edu.vn")
    private String email;

    @NotBlank(message = "Mật khẩu ban đầu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự")
    @Schema(description = "Mật khẩu đăng nhập ban đầu", example = "Admin@123")
    private String password;
}

