package com.edupay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu gạch nợ thủ công phiếu thu")
public class InvoicePaymentManualRequest {

    @NotBlank(message = "Hình thức thanh toán không được để trống")
    @Schema(description = "Phương thức thanh toán (CASH, MANUAL, VIETQR)", example = "CASH", requiredMode = Schema.RequiredMode.REQUIRED)
    private String paidMethod;

    @Schema(description = "Ghi chú khi gạch nợ", example = "Phụ huynh nộp tiền mặt trực tiếp tại lớp")
    private String note;
}

