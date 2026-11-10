package com.edupay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantExtendRequest {

    /**
     * Thời hạn gia hạn: "1_MONTH" (+30 ngày) hoặc "1_YEAR" (+365 ngày)
     */
    @NotBlank(message = "Thời gian gia hạn không được để trống")
    private String duration; // "1_MONTH", "1_YEAR"

    private String plan; // "MONTHLY", "ANNUAL" (nếu muốn nâng cấp gói)

    private String note;
}
