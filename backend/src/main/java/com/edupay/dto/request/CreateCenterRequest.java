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
public class CreateCenterRequest {

    @NotBlank(message = "Tên trung tâm không được để trống")
    private String name;

    @NotBlank(message = "Số điện thoại đăng nhập chủ trung tâm không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ (10 chữ số)")
    private String phone;

    private String email;

    @NotBlank(message = "Tên người đại diện không được để trống")
    private String representativeName;

    private String password; // Nếu null mặc định Admin@123

    @Builder.Default
    private String subscriptionPlan = "MONTHLY"; // MONTHLY, ANNUAL, ENTERPRISE

    @Builder.Default
    private Integer durationMonths = 1; // Số tháng đăng ký ban đầu
}

