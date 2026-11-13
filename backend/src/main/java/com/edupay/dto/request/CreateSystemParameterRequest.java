package com.edupay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Yêu cầu tạo tham số cấu hình hệ thống mới")
public class CreateSystemParameterRequest {

    @NotBlank(message = "Mã tham số không được để trống")
    @Pattern(regexp = "^[A-Z0-9_]{3,100}$", message = "Mã tham số chỉ gồm chữ cái IN HOA, chữ số và gạch dưới (VD: SYSTEM_AUTO_LOGOUT_HOURS)")
    @Schema(description = "Mã định danh tham số (IN HOA không dấu)", example = "SECURITY_MAX_LOGIN_ATTEMPTS")
    private String paramKey;

    @NotBlank(message = "Giá trị tham số không được để trống")
    @Schema(description = "Giá trị tham số", example = "5")
    private String paramValue;

    @NotBlank(message = "Nhóm tham số không được để trống")
    @Size(max = 50, message = "Nhóm tham số tối đa 50 ký tự")
    @Schema(description = "Nhóm tham số (SYSTEM, TUITION, NOTIFICATION, SECURITY...)", example = "SECURITY")
    private String paramGroup;

    @NotBlank(message = "Kiểu dữ liệu không được để trống")
    @Pattern(regexp = "^(STRING|NUMBER|BOOLEAN|JSON)$", message = "Kiểu dữ liệu chỉ nhận STRING, NUMBER, BOOLEAN hoặc JSON")
    @Schema(description = "Kiểu dữ liệu (STRING, NUMBER, BOOLEAN, JSON)", example = "NUMBER")
    private String dataType;

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 200, message = "Tên hiển thị tối đa 200 ký tự")
    @Schema(description = "Tên hiển thị tiếng Việt", example = "Số lần đăng nhập sai tối đa")
    private String displayName;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    @Schema(description = "Mô tả chi tiết và phạm vi áp dụng của tham số", example = "Số lần đăng nhập sai tối đa trước khi tài khoản bị tạm khóa")
    private String description;
}

