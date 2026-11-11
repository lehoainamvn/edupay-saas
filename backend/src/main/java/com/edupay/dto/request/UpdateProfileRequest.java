package com.edupay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cập nhật thông tin hồ sơ giáo viên")
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên giáo viên không được để trống")
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    @Schema(description = "Họ và tên giáo viên", example = "Lê Hoài Nam")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    @Schema(description = "Số điện thoại liên hệ & đăng nhập", example = "0912345678")
    private String phone;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    @Schema(description = "Địa chỉ email (không bắt buộc)", example = "nam@gmail.com")
    private String email;

    @Size(max = 500, message = "Đường dẫn ảnh đại diện không quá 500 ký tự")
    @Schema(description = "URL ảnh đại diện (avatar)", example = "https://example.com/avatar.jpg")
    private String avatarUrl;
}

