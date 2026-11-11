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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
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

        // 2. Classes (All active classes & today's classes)
        List<DashboardSummaryResponse.TodayClassDto> allClasses = loadAllClasses(tenantId);
        List<DashboardSummaryResponse.TodayClassDto> todayClasses = filterTodayClasses(allClasses);

        // 3. Recent Transactions
        List<DashboardSummaryResponse.RecentTransactionDto> recentTransactions = loadRecentTransactions(tenantId);

        // 4. Debtors
        List<DashboardSummaryResponse.DebtorDto> debtors = loadDebtors(tenantId);

        // 5. Bank Settings
        DashboardSummaryResponse.BankSettingDto bankSettings = loadBankSettings(tenantId);

        // 6. Monthly Revenue Chart (6 months)
        List<DashboardSummaryResponse.MonthlyRevenueChartDto> monthlyChart = loadMonthlyRevenueChart(tenantId);

        return DashboardSummaryResponse.builder()
                .kpis(kpis)
                .todayClasses(todayClasses)
                .allClasses(allClasses)
                .recentTransactions(recentTransactions)
                .debtors(debtors)
                .bankSettings(bankSettings)
                .monthlyChart(monthlyChart)
                .build();
    }

    private List<DashboardSummaryResponse.TodayClassDto> loadAllClasses(UUID tenantId) {
        String sql = "SELECT c.id, c.name, c.description, c.fee_type, c.default_fee, " +
                "c.schedule_days, c.start_time, c.end_time, " +
                "(SELECT COUNT(*) FROM students s WHERE s.class_id = c.id AND s.status = 'STUDYING') as student_count " +
                "FROM classes c WHERE c.tenant_id = ? AND c.is_active = true " +
                "ORDER BY c.start_time ASC NULLS LAST, c.created_at ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID id = (UUID) rs.getObject("id");
            String name = rs.getString("name");
            String desc = rs.getString("description");
            String feeType = rs.getString("fee_type");
            double fee = rs.getDouble("default_fee");
            long count = rs.getLong("student_count");
            String scheduleDays = rs.getString("schedule_days");
            String startTime = rs.getString("start_time");
            String endTime = rs.getString("end_time");

            String timeStr = (startTime != null && !startTime.isBlank()) ? startTime : "17:30";
            String endStr = (endTime != null && !endTime.isBlank()) ? endTime : "19:00";
            String roomName = "Phòng trực tiếp";
            if (desc != null && !desc.isBlank()) {
                if (desc.contains("·")) {
                    String[] parts = desc.split("·");
                    String last = parts[parts.length - 1].trim();
                    if (!last.toLowerCase().contains("thứ") && !last.contains(":")) {
                        roomName = last;
                    }
                } else if (!desc.toLowerCase().contains("thứ") && !desc.contains(":")) {
                    roomName = desc.trim();
                }
            }

            return DashboardSummaryResponse.TodayClassDto.builder()
                    .id(id)
                    .name(name)
                    .time(timeStr)
                    .end(endStr)
                    .room(roomName)
                    .scheduleDays(scheduleDays != null && !scheduleDays.isBlank() ? scheduleDays : desc)
                    .count(count)
                    .fee(fee)
                    .feeType(feeType)
                    .build();
        }, tenantId);
    }

    private List<DashboardSummaryResponse.TodayClassDto> filterTodayClasses(List<DashboardSummaryResponse.TodayClassDto> allClasses) {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        DayOfWeek todayDow = LocalDate.now(zone).getDayOfWeek();
        String todayName = todayDow.name(); // e.g. MONDAY, TUESDAY...

        List<DashboardSummaryResponse.TodayClassDto> allMatching = new ArrayList<>();

        for (DashboardSummaryResponse.TodayClassDto c : allClasses) {
            String scheduleDays = c.getScheduleDays();
            boolean matchesToday = false;

            if (scheduleDays != null && !scheduleDays.isBlank()) {
                matchesToday = Arrays.stream(scheduleDays.split(","))
                        .map(String::trim)
                        .anyMatch(day -> day.equalsIgnoreCase(todayName) || day.equals(String.valueOf(todayDow.getValue() + 1)));

                if (!matchesToday) {
                    String lower = scheduleDays.toLowerCase();
                    switch (todayDow) {
                        case MONDAY -> matchesToday = lower.contains("thứ 2") || lower.contains("t2");
                        case TUESDAY -> matchesToday = lower.contains("thứ 3") || lower.contains("t3");
                        case WEDNESDAY -> matchesToday = lower.contains("thứ 4") || lower.contains("t4");
                        case THURSDAY -> matchesToday = lower.contains("thứ 5") || lower.contains("t5");
                        case FRIDAY -> matchesToday = lower.contains("thứ 6") || lower.contains("t6");
                        case SATURDAY -> matchesToday = lower.contains("thứ 7") || lower.contains("t7");
                        case SUNDAY -> matchesToday = lower.contains("chủ nhật") || lower.contains("cn");
                    }
                }
            }

            if (matchesToday) {
                // Clone item for today view
                allMatching.add(DashboardSummaryResponse.TodayClassDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .time(c.getTime())
                        .end(c.getEnd())
                        .room(c.getRoom())
                        .scheduleDays(c.getScheduleDays())
                        .count(c.getCount())
                        .fee(c.getFee())
                        .feeType(c.getFeeType())
                        .build());
            }
        }

        // Gán trạng thái: Ca đầu tiên là upcoming, các ca sau là later
        for (int i = 0; i < allMatching.size(); i++) {
            DashboardSummaryResponse.TodayClassDto item = allMatching.get(i);
            boolean isFirst = (i == 0);
            item.setState(isFirst ? "upcoming" : "later");
            item.setText(isFirst ? "Sắp diễn ra" : "Ca tiếp theo");
        }

        return allMatching;
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

    private List<DashboardSummaryResponse.MonthlyRevenueChartDto> loadMonthlyRevenueChart(UUID tenantId) {
        List<DashboardSummaryResponse.MonthlyRevenueChartDto> chart = new ArrayList<>();
        YearMonth current = YearMonth.now();
        List<YearMonth> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            months.add(current.minusMonths(i));
        }

        Map<String, Map<String, Object>> monthDataMap = new HashMap<>();
        String sql = "SELECT billing_month, " +
                "COALESCE(SUM(total_amount), 0) as expected_amount, " +
                "COALESCE(SUM(CASE WHEN status = 'PAID' THEN total_amount ELSE 0 END), 0) as actual_amount, " +
                "COUNT(CASE WHEN status = 'PAID' THEN 1 END) as paid_count, " +
                "COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as unpaid_count " +
                "FROM invoices " +
                "WHERE tenant_id = ? AND billing_month >= ? " +
                "GROUP BY billing_month";

        String earliestMonth = months.get(0).toString();
        jdbcTemplate.query(sql, rs -> {
            String bm = rs.getString("billing_month");
            Map<String, Object> row = new HashMap<>();
            row.put("expected", rs.getDouble("expected_amount"));
            row.put("actual", rs.getDouble("actual_amount"));
            row.put("paid", rs.getLong("paid_count"));
            row.put("unpaid", rs.getLong("unpaid_count"));
            monthDataMap.put(bm, row);
        }, tenantId, earliestMonth);

        for (YearMonth ym : months) {
            String monthKey = ym.toString();
            String label = "Thg " + ym.getMonthValue();
            Map<String, Object> data = monthDataMap.get(monthKey);
            double expected = data != null ? (Double) data.getOrDefault("expected", 0.0) : 0.0;
            double actual = data != null ? (Double) data.getOrDefault("actual", 0.0) : 0.0;
            long paid = data != null ? (Long) data.getOrDefault("paid", 0L) : 0L;
            long unpaid = data != null ? (Long) data.getOrDefault("unpaid", 0L) : 0L;
            double rate = expected > 0 ? Math.round((actual / expected * 100.0) * 10.0) / 10.0 : 0.0;

            chart.add(DashboardSummaryResponse.MonthlyRevenueChartDto.builder()
                    .month(monthKey)
                    .label(label)
                    .expectedAmount(expected)
                    .actualAmount(actual)
                    .paidCount(paid)
                    .unpaidCount(unpaid)
                    .collectionRate(rate)
                    .build());
        }

        return chart;
    }
}

