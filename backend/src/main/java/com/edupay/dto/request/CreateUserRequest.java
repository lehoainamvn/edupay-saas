package com.edupay.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreateUserRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(min = 9, max = 20, message = "Số điện thoại từ 9 đến 20 ký tự")
    private String phone;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;

    private String roleCode; // SUPER_ADMIN, OWNER, TEACHER, VIEWER

    private UUID tenantId; // If null and not SUPER_ADMIN, will link to current or default tenant
}
