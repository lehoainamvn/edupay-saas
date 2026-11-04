package com.edupay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu xuất phiếu thu học phí hàng loạt cho các học sinh được chọn")
public class InvoiceBatchGenerateRequest {

    @NotNull(message = "ID lớp học không được để trống")
    @Schema(description = "ID lớp học", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID classId;

    @NotBlank(message = "Kỳ thu phí không được để trống")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Kỳ thu phí phải đúng định dạng YYYY-MM (VD: 2026-10)")
    @Schema(description = "Kỳ thu phí (YYYY-MM)", example = "2026-10", requiredMode = Schema.RequiredMode.REQUIRED)
    private String billingMonth;

    @NotEmpty(message = "Danh sách học sinh xuất phiếu không được để trống")
    @Valid
    @Schema(description = "Danh sách chi tiết từng học sinh cần xuất phiếu thu", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<InvoiceItemGenerateRequest> items;
}

