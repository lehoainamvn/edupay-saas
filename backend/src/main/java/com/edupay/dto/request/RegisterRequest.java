package com.edupay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[3-9][0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 255, message = "Họ tên phải từ 2 đến 255 ký tự")
    private String fullName;

    /** Tên hiển thị của trung tâm/lớp học (VD: "Lớp Toán Cô Mai") */
    @NotBlank(message = "Tên trung tâm/lớp học không được để trống")
    @Size(min = 2, max = 255, message = "Tên trung tâm phải từ 2 đến 255 ký tự")
    private String centerName;
}
