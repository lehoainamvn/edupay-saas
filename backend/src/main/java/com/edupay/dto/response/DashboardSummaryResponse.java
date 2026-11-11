package com.edupay.dto.response;

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
public class DashboardSummaryResponse {

    private KpiMetrics kpis;
    private List<TodayClassDto> todayClasses;
    private List<TodayClassDto> allClasses;
    private List<RecentTransactionDto> recentTransactions;
    private List<DebtorDto> debtors;
    private BankSettingDto bankSettings;
    private List<MonthlyRevenueChartDto> monthlyChart;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiMetrics {
        private Double expectedTuition;
        private Double actualTuition;
        private Double unpaidTuition;
        private Double attendanceRate;
        private Long totalStudents;
        private Long totalClasses;
        private Long unpaidCount;
        private Long paidCount;
        private Double progressPercent;
        private Long attendedCount;
        private Long permittedLeaveCount;
        private Long unpermittedLeaveCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenueChartDto {
        private String month;          // VD: "2026-10"
        private String label;          // VD: "Thg 10"
        private Double expectedAmount; // Doanh thu dự kiến
        private Double actualAmount;   // Thực thu đã nộp
        private Long paidCount;        // Số phiếu đã nộp
        private Long unpaidCount;      // Số phiếu chưa nộp
        private Double collectionRate; // Tỷ lệ thu hồi %
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayClassDto {
        private UUID id;
        private String name;
        private String time;
        private String end;
        private String room;
        private String scheduleDays;
        private Long count;
        private Double fee;
        private String feeType;
        private String state; // upcoming, later
        private String text;  // Sắp diễn ra, Ca tiếp theo
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTransactionDto {
        private UUID id;
        private String name;
        private String className;
        private String time;
        private String amount;
        private Double rawAmount;
        private String code;
        private String initials;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DebtorDto {
        private UUID id;
        private String name;
        private String parent;
        private String amount;
        private Double rawAmount;
        private String due;
        private String initials;
        private Boolean danger;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankSettingDto {
        private String bankName;
        private String accountNumber;
        private String accountName;
        private String bankBin;
    }
}
