package com.edupay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsResponse {
    private BigDecimal mrr;                  // Doanh thu định kỳ hàng tháng (Monthly Recurring Revenue)
    private long newTeachersThisMonth;       // Số giáo viên mới đăng ký trong tháng
    private double renewalRate;              // Tỷ lệ gia hạn (%)
    private long totalTenants;               // Tổng số giáo viên thuê
    private long activeTenants;              // Số giáo viên đang hoạt động
    private long suspendedTenants;           // Số giáo viên bị tạm khóa
    private long trialTenants;               // Số giáo viên đang dùng thử
}
