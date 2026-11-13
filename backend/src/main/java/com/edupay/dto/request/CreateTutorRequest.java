package com.edupay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTutorRequest {

    @NotBlank(message = "Họ và tên gia sư không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại đăng nhập không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ (10 chữ số)")
    private String phone;

    private String email;

    private String password; // Nếu null mặc định Admin@123

    @Builder.Default
    private String subscriptionPlan = "MONTHLY";

    @Builder.Default
    private Integer durationMonths = 1;
}

