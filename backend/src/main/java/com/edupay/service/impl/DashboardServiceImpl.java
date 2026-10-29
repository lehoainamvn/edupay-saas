package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.config.TenantContextFilter;
import com.edupay.dto.response.DashboardSummaryResponse;
import com.edupay.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = TenantContextFilter.DEV_DEFAULT_TENANT_ID;
        }

        String currentMonth = YearMonth.now().toString();

        // 1. KPI Metrics
        Long totalClasses = queryForLong("SELECT COUNT(*) FROM classes WHERE tenant_id = ? AND is_active = true", tenantId);
        Long totalStudents = queryForLong("SELECT COUNT(*) FROM students WHERE tenant_id = ? AND status = 'STUDYING'", tenantId);

        Double invoiceExpected = queryForDouble("SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE tenant_id = ? AND billing_month = ?", tenantId, currentMonth);
        Double classExpected = queryForDouble("SELECT COALESCE(SUM(c.default_fee * (SELECT COUNT(*) FROM students s WHERE s.class_id = c.id AND s.status = 'STUDYING')), 0) FROM classes c WHERE c.tenant_id = ? AND c.is_active = true", tenantId);
        Double expectedTuition = (invoiceExpected != null && invoiceExpected > 0) ? invoiceExpected : (classExpected != null ? classExpected : 0.0);

        Double actualTuition = queryForDouble("SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE tenant_id = ? AND billing_month = ? AND status = 'PAID'", tenantId, currentMonth);
        Double unpaidTuition = queryForDouble("SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE tenant_id = ? AND billing_month = ? AND status = 'PENDING'", tenantId, currentMonth);

        Long unpaidCount = queryForLong("SELECT COUNT(DISTINCT student_id) FROM invoices WHERE tenant_id = ? AND billing_month = ? AND status = 'PENDING'", tenantId, currentMonth);
        Long paidCount = queryForLong("SELECT COUNT(*) FROM invoices WHERE tenant_id = ? AND billing_month = ? AND status = 'PAID'", tenantId, currentMonth);

        Long attendedCount = queryForLong("SELECT COUNT(*) FROM attendance WHERE tenant_id = ? AND status = 'PRESENT' AND TO_CHAR(lesson_date, 'YYYY-MM') = ?", tenantId, currentMonth);
        Long permittedLeaveCount = queryForLong("SELECT COUNT(*) FROM attendance WHERE tenant_id = ? AND status = 'ABSENT_PERMITTED' AND TO_CHAR(lesson_date, 'YYYY-MM') = ?", tenantId, currentMonth);
        Long unpermittedLeaveCount = queryForLong("SELECT COUNT(*) FROM attendance WHERE tenant_id = ? AND status = 'ABSENT_UNPERMITTED' AND TO_CHAR(lesson_date, 'YYYY-MM') = ?", tenantId, currentMonth);

        long totalAttendance = attendedCount + permittedLeaveCount + unpermittedLeaveCount;
        double attendanceRate = totalAttendance > 0 ? Math.round((attendedCount * 100.0 / totalAttendance) * 10.0) / 10.0 : 0.0;
        double progressPercent = (expectedTuition != null && expectedTuition > 0) ? Math.min(100.0, Math.round((actualTuition / expectedTuition * 100.0) * 10.0) / 10.0) : 0.0;

        DashboardSummaryResponse.KpiMetrics kpis = DashboardSummaryResponse.KpiMetrics.builder()
                .expectedTuition(expectedTuition)
                .actualTuition(actualTuition)
                .unpaidTuition(unpaidTuition)
                .attendanceRate(attendanceRate)
                .totalStudents(totalStudents)
                .totalClasses(totalClasses)
                .unpaidCount(unpaidCount)
                .paidCount(paidCount)
                .progressPercent(progressPercent)
                .attendedCount(attendedCount)
                .permittedLeaveCount(permittedLeaveCount)
                .unpermittedLeaveCount(unpermittedLeaveCount)
                .build();

        // 2. Today Classes
        List<DashboardSummaryResponse.TodayClassDto> todayClasses = loadTodayClasses(tenantId);

        // 3. Recent Transactions
        List<DashboardSummaryResponse.RecentTransactionDto> recentTransactions = loadRecentTransactions(tenantId);

        // 4. Debtors
        List<DashboardSummaryResponse.DebtorDto> debtors = loadDebtors(tenantId);

        // 5. Bank Settings
        DashboardSummaryResponse.BankSettingDto bankSettings = loadBankSettings(tenantId);

        return DashboardSummaryResponse.builder()
                .kpis(kpis)
                .todayClasses(todayClasses)
                .recentTransactions(recentTransactions)
                .debtors(debtors)
                .bankSettings(bankSettings)
                .build();
    }

    private List<DashboardSummaryResponse.TodayClassDto> loadTodayClasses(UUID tenantId) {
        String sql = "SELECT c.id, c.name, c.description, c.fee_type, c.default_fee, " +
                "(SELECT COUNT(*) FROM students s WHERE s.class_id = c.id AND s.status = 'STUDYING') as student_count " +
                "FROM classes c WHERE c.tenant_id = ? AND c.is_active = true " +
                "ORDER BY c.created_at ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID id = (UUID) rs.getObject("id");
            String name = rs.getString("name");
            String desc = rs.getString("description");
            String feeType = rs.getString("fee_type");
            double fee = rs.getDouble("default_fee");
            long count = rs.getLong("student_count");

            boolean isFirst = (rowNum == 0);
            return DashboardSummaryResponse.TodayClassDto.builder()
                    .id(id)
                    .name(name)
                    .time(isFirst ? "17:30" : "19:30")
                    .end(isFirst ? "19:00" : "21:00")
                    .room(desc != null && !desc.isBlank() ? desc : "Tại nhà · Phòng học 1")
                    .count(count)
                    .fee(fee)
                    .feeType(feeType)
                    .state(isFirst ? "upcoming" : "later")
                    .text(isFirst ? "Sắp diễn ra" : "Ca tiếp theo")
                    .build();
        }, tenantId);
    }

    private List<DashboardSummaryResponse.RecentTransactionDto> loadRecentTransactions(UUID tenantId) {
        String sql = "SELECT i.id, s.full_name as student_name, c.name as class_name, " +
                "i.total_amount, i.transfer_code, i.status, i.paid_at, i.created_at " +
                "FROM invoices i " +
                "JOIN students s ON s.id = i.student_id " +
                "JOIN classes c ON c.id = i.class_id " +
                "WHERE i.tenant_id = ? AND i.status = 'PAID' " +
                "ORDER BY COALESCE(i.paid_at, i.created_at) DESC LIMIT 5";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID id = (UUID) rs.getObject("id");
            String sName = rs.getString("student_name");
            String cName = rs.getString("class_name");
            double amount = rs.getDouble("total_amount");
            String code = rs.getString("transfer_code");
            String status = rs.getString("status");

            return DashboardSummaryResponse.RecentTransactionDto.builder()
                    .id(id)
                    .name(sName)
                    .className(cName)
                    .time("Gần đây")
                    .amount(formatMoney(amount))
                    .rawAmount(amount)
                    .code(code)
                    .initials(extractInitials(sName))
                    .status(status)
                    .build();
        }, tenantId);
    }

    private List<DashboardSummaryResponse.DebtorDto> loadDebtors(UUID tenantId) {
        String sql = "SELECT i.id, s.full_name as student_name, s.parent_name, s.parent_phone, " +
                "c.name as class_name, i.total_amount, i.billing_month, i.created_at " +
                "FROM invoices i " +
                "JOIN students s ON s.id = i.student_id " +
                "JOIN classes c ON c.id = i.class_id " +
                "WHERE i.tenant_id = ? AND i.status = 'PENDING' " +
                "ORDER BY i.created_at DESC LIMIT 5";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID id = (UUID) rs.getObject("id");
            String sName = rs.getString("student_name");
            String pName = rs.getString("parent_name");
            String pPhone = rs.getString("parent_phone");
            double amount = rs.getDouble("total_amount");
            String month = rs.getString("billing_month");

            String parentLabel = (pName != null && !pName.isBlank()) ? "PH: " + pName : "PH: " + (pPhone != null ? pPhone : "Chưa có SĐT");
            return DashboardSummaryResponse.DebtorDto.builder()
                    .id(id)
                    .name(sName)
                    .parent(parentLabel)
                    .amount(formatMoney(amount))
                    .rawAmount(amount)
                    .due("Kỳ học " + (month != null ? month : "tháng này"))
                    .initials(extractInitials(sName))
                    .danger(amount > 1000000)
                    .build();
        }, tenantId);
    }

    private DashboardSummaryResponse.BankSettingDto loadBankSettings(UUID tenantId) {
        String sql = "SELECT bank_name, account_number, account_name, bank_bin FROM payment_settings WHERE tenant_id = ? LIMIT 1";
        List<DashboardSummaryResponse.BankSettingDto> list = jdbcTemplate.query(sql, (rs, rowNum) ->
                DashboardSummaryResponse.BankSettingDto.builder()
                        .bankName(rs.getString("bank_name"))
                        .accountNumber(rs.getString("account_number"))
                        .accountName(rs.getString("account_name"))
                        .bankBin(rs.getString("bank_bin"))
                        .build(), tenantId);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long queryForLong(String sql, Object... args) {
        try {
            Long val = jdbcTemplate.queryForObject(sql, Long.class, args);
            return val != null ? val : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private Double queryForDouble(String sql, Object... args) {
        try {
            Double val = jdbcTemplate.queryForObject(sql, Double.class, args);
            return val != null ? val : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String formatMoney(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,### đ", symbols);
        return formatter.format(amount);
    }

    private String extractInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "HV";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[parts.length - 2].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}

