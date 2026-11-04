package com.edupay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu xuất phiếu thu cho 1 học sinh")
public class InvoiceItemGenerateRequest {

    @NotNull(message = "ID học sinh không được để trống")
    @Schema(description = "ID học sinh", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID studentId;

    @NotNull(message = "Số buổi có mặt không được để trống")
    @Min(value = 0, message = "Số buổi có mặt phải lớn hơn hoặc bằng 0")
    @Schema(description = "Số buổi có mặt thực tế", example = "8")
    private Integer attendedCount;

    @NotNull(message = "Đơn giá không được để trống")
    @DecimalMin(value = "0.0", message = "Đơn giá không được âm")
    @Schema(description = "Đơn giá / buổi hoặc trọn gói tháng", example = "150000.00")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.0", message = "Phụ phí không được âm")
    @Schema(description = "Phụ phí (sách vở, tài liệu)", example = "50000.00")
    private BigDecimal extraFee;

    @DecimalMin(value = "0.0", message = "Khoản giảm trừ không được âm")
    @Schema(description = "Khoản giảm trừ (học bổng, nghỉ bù)", example = "0.00")
    private BigDecimal discount;

    @Schema(description = "Ghi chú riêng cho phiếu thu", example = "Tiền sách Toán 9 tập 1")
    private String note;
}

