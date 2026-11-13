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
    private BigDecimal arr;                  // Doanh thu quy năm ước tính (Annual Run Rate)
    private double renewalRate;              // Tỷ lệ gia hạn (%)
    private long totalTenants;               // Tổng số cơ sở/khách hàng thuê
    private long totalCenters;               // Tổng số Trung tâm đào tạo
    private long totalTutors;                // Tổng số Gia sư tự do
    private long totalTeachers;              // Tổng số Giáo viên toàn hệ thống (thuộc các trung tâm + gia sư)
    private long totalStudents;              // Tổng số Học sinh toàn hệ thống
    private long totalClasses;               // Tổng số Lớp học toàn hệ thống
    private long activeTenants;              // Số cơ sở đang hoạt động
    private long suspendedTenants;           // Số cơ sở bị tạm khóa
    private long trialTenants;               // Số cơ sở đang dùng thử
    private long newTeachersThisMonth;       // Số tài khoản mới trong tháng
}
