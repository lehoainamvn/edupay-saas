package com.edupay.service.impl;

import com.edupay.dto.request.TenantExtendRequest;
import com.edupay.dto.response.PlatformStatsResponse;
import com.edupay.dto.response.TenantAdminResponse;
import com.edupay.entity.Tenant;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.ClassRepository;
import com.edupay.repository.StudentRepository;
import com.edupay.repository.TenantRepository;
import com.edupay.service.SuperAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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

    private static final BigDecimal MONTHLY_PRICE = new BigDecimal("59000");
    private static final BigDecimal ANNUAL_MONTHLY_EQUIVALENT = new BigDecimal("499000").divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);

    @Override
    @Transactional(readOnly = true)
    public List<TenantAdminResponse> getAllTenants(String status, String search) {
        List<Tenant> tenants = tenantRepository.findAllByOrderByCreatedAtDesc();
        Instant now = Instant.now();

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
                .orElseThrow(() -> new ResourceNotFoundException("TENANT_NOT_FOUND", "Không tìm thấy thông tin Giáo viên với ID: " + tenantId));

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
                .orElseThrow(() -> new ResourceNotFoundException("TENANT_NOT_FOUND", "Không tìm thấy thông tin Giáo viên với ID: " + tenantId));

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
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countBySubscriptionStatus("ACTIVE");
        long suspendedTenants = tenantRepository.countBySubscriptionStatus("SUSPENDED");
        long trialTenants = tenantRepository.countBySubscriptionStatus("TRIAL");

        // Giáo viên mới trong 30 ngày qua
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long newTeachersThisMonth = tenantRepository.countByCreatedAtAfter(thirtyDaysAgo);

        // Tính MRR
        long activeMonthly = tenantRepository.countActiveMonthlyTenants();
        long activeAnnual = tenantRepository.countActiveAnnualTenants();
        BigDecimal mrr = MONTHLY_PRICE.multiply(BigDecimal.valueOf(activeMonthly))
                .add(ANNUAL_MONTHLY_EQUIVALENT.multiply(BigDecimal.valueOf(activeAnnual)));

        // Tỷ lệ gia hạn ước tính
        double renewalRate = totalTenants > 0 ? ((double) activeTenants / totalTenants) * 100.0 : 85.0;

        return PlatformStatsResponse.builder()
                .mrr(mrr)
                .newTeachersThisMonth(newTeachersThisMonth)
                .renewalRate(Math.round(renewalRate * 10.0) / 10.0)
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .suspendedTenants(suspendedTenants)
                .trialTenants(trialTenants)
                .build();
    }

    private TenantAdminResponse toTenantAdminResponse(Tenant t, Instant now) {
        long totalClasses = classRepository.findAllByTenantIdOrderByIsActiveDescCreatedAtDesc(t.getId()).size();
        long totalStudents = studentRepository.findAllByTenantIdOrderByCreatedAtDesc(t.getId()).size();
        boolean isExpired = t.getSubscriptionEndsAt() != null && t.getSubscriptionEndsAt().isBefore(now);

        return TenantAdminResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .phone(t.getPhone())
                .email(t.getEmail())
                .subscriptionPlan(t.getSubscriptionPlan())
                .subscriptionStatus(t.getSubscriptionStatus())
                .subscriptionEndsAt(t.getSubscriptionEndsAt())
                .createdAt(t.getCreatedAt())
                .totalClasses(totalClasses)
                .totalStudents(totalStudents)
                .isExpired(isExpired)
                .build();
    }

    private boolean matchesTenantSearch(Tenant t, String query) {
        String combined = (t.getName() + " " + t.getPhone() + " " + (t.getEmail() != null ? t.getEmail() : "")).toLowerCase();
        return combined.contains(query);
    }
}
