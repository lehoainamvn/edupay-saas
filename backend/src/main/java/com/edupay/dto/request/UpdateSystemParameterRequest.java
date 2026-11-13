package com.edupay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSystemParameterRequest {

    @NotNull(message = "Giá trị tham số không được để trống")
    @Schema(description = "Giá trị mới của tham số cấu hình", example = "7")
    private String paramValue;

    @Schema(description = "Tên hiển thị mới (tùy chọn)", example = "Hạn thanh toán học phí")
    private String displayName;

    @Schema(description = "Mô tả mới (tùy chọn)", example = "Số ngày phụ huynh được phép thanh toán sau khi phát hành phiếu thu")
    private String description;
}

