package com.edupay.service.impl;

import com.edupay.dto.request.TenantExtendRequest;
import com.edupay.dto.response.PlatformStatsResponse;
import com.edupay.dto.response.TenantAdminResponse;
import com.edupay.entity.Tenant;
import com.edupay.entity.User;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.ClassRepository;
import com.edupay.repository.StudentRepository;
import com.edupay.repository.TenantRepository;
import com.edupay.repository.UserRepository;
import com.edupay.service.SuperAdminService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final TenantRepository tenantRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    private static final BigDecimal MONTHLY_PRICE = new BigDecimal("59000");
    private static final BigDecimal ANNUAL_MONTHLY_EQUIVALENT = new BigDecimal("499000").divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);

    private boolean isSuperAdmin() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("SUPER_ADMIN"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantAdminResponse> getAllTenants(String status, String search) {
        UUID currentTenantId = com.edupay.config.TenantContext.getTenantId();
        Instant now = Instant.now();

        if (!isSuperAdmin() && currentTenantId != null) {
            // Khi gọi bởi CenterAdmin: chỉ trả về cơ sở trung tâm của chính họ
            return tenantRepository.findById(currentTenantId)
                    .map(t -> List.of(toTenantAdminResponse(t, now)))
                    .orElse(List.of());
        }

        List<Tenant> tenants = tenantRepository.findAllByOrderByCreatedAtDesc();

        return tenants.stream()
                .filter(t -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) || t.getSubscriptionStatus().equalsIgnoreCase(status))
                .filter(t -> search == null || search.isBlank() || matchesTenantSearch(t, search.trim().toLowerCase()))
                .map(t -> toTenantAdminResponse(t, now))
                .toList();
    }

    @Override
    @Transactional
    public TenantAdminResponse updateTenantStatus(UUID tenantId, String newStatus) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("TENANT_NOT_FOUND", "Không tìm thấy thông tin cơ sở với ID: " + tenantId));

        String upperStatus = newStatus.trim().toUpperCase();
        if (!List.of("ACTIVE", "SUSPENDED", "EXPIRED").contains(upperStatus)) {
            throw new BusinessException("INVALID_STATUS", "Trạng thái không hợp lệ (Chỉ chấp nhận: ACTIVE, SUSPENDED, EXPIRED)");
        }

        tenant.setSubscriptionStatus(upperStatus);
        Tenant saved = tenantRepository.save(tenant);
        log.info("SuperAdmin cập nhật trạng thái Tenant [{}] thành [{}]", tenant.getName(), upperStatus);

        return toTenantAdminResponse(saved, Instant.now());
    }

    @Override
    @Transactional
    public TenantAdminResponse extendTenantSubscription(UUID tenantId, TenantExtendRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("TENANT_NOT_FOUND", "Không tìm thấy thông tin cơ sở với ID: " + tenantId));

        Instant now = Instant.now();
        Instant baseTime = (tenant.getSubscriptionEndsAt() != null && tenant.getSubscriptionEndsAt().isAfter(now))
                ? tenant.getSubscriptionEndsAt()
                : now;

        long daysToAdd = "1_YEAR".equalsIgnoreCase(request.getDuration()) ? 365 : 30;
        Instant newExpiry = baseTime.plus(daysToAdd, ChronoUnit.DAYS);

        tenant.setSubscriptionEndsAt(newExpiry);
        tenant.setSubscriptionStatus("ACTIVE");

        if (request.getPlan() != null && !request.getPlan().isBlank()) {
            tenant.setSubscriptionPlan(request.getPlan().toUpperCase());
        }

        Tenant saved = tenantRepository.save(tenant);
        log.info("SuperAdmin gia hạn gói cho Tenant [{}] thêm {} ngày. Hạn mới: {}",
                saved.getName(), daysToAdd, newExpiry);

        return toTenantAdminResponse(saved, now);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformStatsResponse getPlatformStats() {
        UUID currentTenantId = com.edupay.config.TenantContext.getTenantId();

        if (!isSuperAdmin() && currentTenantId != null) {
            // Thống kê dành riêng cho Giám đốc / Quản lý Trung tâm
            long centerTeachers = userRepository.countByTenantIdAndIsOwnerFalse(currentTenantId);
            long centerClasses = classRepository.findAllByTenantIdOrderByIsActiveDescCreatedAtDesc(currentTenantId).size();
            long centerStudents = studentRepository.findAllByTenantIdOrderByCreatedAtDesc(currentTenantId).size();

            BigDecimal centerRevenue = BigDecimal.ZERO;
            BigDecimal centerExpected = BigDecimal.ZERO;
            try {
                String currentMonth = YearMonth.now().toString();
                Object paidRes = entityManager.createNativeQuery(
                                "SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE tenant_id = :tenantId AND billing_month = :currentMonth AND status = 'PAID'")
                        .setParameter("tenantId", currentTenantId)
                        .setParameter("currentMonth", currentMonth)
                        .getSingleResult();
                if (paidRes != null) {
                    centerRevenue = new BigDecimal(paidRes.toString());
                }

                Object totalRes = entityManager.createNativeQuery(
                                "SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE tenant_id = :tenantId AND billing_month = :currentMonth")
                        .setParameter("tenantId", currentTenantId)
                        .setParameter("currentMonth", currentMonth)
                        .getSingleResult();
                if (totalRes != null && new BigDecimal(totalRes.toString()).compareTo(BigDecimal.ZERO) > 0) {
                    centerExpected = new BigDecimal(totalRes.toString());
                } else {
                    Object classRes = entityManager.createNativeQuery(
                                    "SELECT COALESCE(SUM(c.default_fee * (SELECT COUNT(*) FROM students s WHERE s.class_id = c.id AND s.status = 'STUDYING')), 0) " +
                                    "FROM classes c WHERE c.tenant_id = :tenantId AND c.is_active = true")
                            .setParameter("tenantId", currentTenantId)
                            .getSingleResult();
                    if (classRes != null) {
                        centerExpected = new BigDecimal(classRes.toString());
                    }
                }
            } catch (Exception e) {
                log.warn("Không thể tính doanh thu học phí cơ sở: {}", e.getMessage());
            }

            return PlatformStatsResponse.builder()
                    .mrr(centerRevenue)
                    .arr(centerExpected)
                    .renewalRate(100.0)
                    .totalTenants(1)
                    .totalCenters(1)
                    .totalTutors(0)
                    .totalTeachers(centerTeachers)
                    .totalStudents(centerStudents)
                    .totalClasses(centerClasses)
                    .activeTenants(1)
                    .suspendedTenants(0)
                    .trialTenants(0)
                    .newTeachersThisMonth(centerTeachers)
                    .build();
        }

        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countBySubscriptionStatus("ACTIVE");
        long suspendedTenants = tenantRepository.countBySubscriptionStatus("SUSPENDED");
        long trialTenants = tenantRepository.countBySubscriptionStatus("TRIAL");

        long totalCenters = tenantRepository.countByTenantType("CENTER");
        long totalTutors = tenantRepository.countByTenantType("SOLO_TUTOR");
        long totalTeachers = userRepository.countByIsOwnerFalse();
        long totalStudents = studentRepository.count();
        long totalClasses = classRepository.count();

        // Tài khoản mới trong 30 ngày qua
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long newTeachersThisMonth = tenantRepository.countByCreatedAtAfter(thirtyDaysAgo);

        // Tính MRR & ARR
        long activeMonthly = tenantRepository.countActiveMonthlyTenants();
        long activeAnnual = tenantRepository.countActiveAnnualTenants();
        BigDecimal mrr = MONTHLY_PRICE.multiply(BigDecimal.valueOf(activeMonthly))
                .add(ANNUAL_MONTHLY_EQUIVALENT.multiply(BigDecimal.valueOf(activeAnnual)));
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12));

        // Tỷ lệ gia hạn ước tính
        double renewalRate = totalTenants > 0 ? ((double) activeTenants / totalTenants) * 100.0 : 85.0;

        return PlatformStatsResponse.builder()
                .mrr(mrr)
                .arr(arr)
                .renewalRate(Math.round(renewalRate * 10.0) / 10.0)
                .totalTenants(totalTenants)
                .totalCenters(totalCenters)
                .totalTutors(totalTutors)
                .totalTeachers(totalTeachers)
                .totalStudents(totalStudents)
                .totalClasses(totalClasses)
                .activeTenants(activeTenants)
                .suspendedTenants(suspendedTenants)
                .trialTenants(trialTenants)
                .newTeachersThisMonth(newTeachersThisMonth)
                .build();
    }

    private TenantAdminResponse toTenantAdminResponse(Tenant t, Instant now) {
        long totalClasses = classRepository.findAllByTenantIdOrderByIsActiveDescCreatedAtDesc(t.getId()).size();
        long totalStudents = studentRepository.findAllByTenantIdOrderByCreatedAtDesc(t.getId()).size();
        long totalTeachers = userRepository.countByTenantIdAndIsOwnerFalse(t.getId());
        boolean isExpired = t.getSubscriptionEndsAt() != null && t.getSubscriptionEndsAt().isBefore(now);

        String repName = t.getName();
        String repPhone = t.getPhone();
        User owner = userRepository.findFirstByTenantIdAndIsOwnerTrue(t.getId()).orElse(null);
        if (owner != null) {
            repName = owner.getFullName();
            repPhone = owner.getPhone();
        }

        return TenantAdminResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .phone(t.getPhone())
                .email(t.getEmail())
                .tenantType(t.getTenantType() != null ? t.getTenantType() : "SOLO_TUTOR")
                .subscriptionPlan(t.getSubscriptionPlan())
                .subscriptionStatus(t.getSubscriptionStatus())
                .subscriptionEndsAt(t.getSubscriptionEndsAt())
                .createdAt(t.getCreatedAt())
                .totalTeachers(totalTeachers)
                .totalClasses(totalClasses)
                .totalStudents(totalStudents)
                .isExpired(isExpired)
                .representativeName(repName)
                .representativePhone(repPhone)
                .build();
    }

    private boolean matchesTenantSearch(Tenant t, String query) {
        String combined = (t.getName() + " " + t.getPhone() + " " + (t.getEmail() != null ? t.getEmail() : "")).toLowerCase();
        return combined.contains(query);
    }
}
