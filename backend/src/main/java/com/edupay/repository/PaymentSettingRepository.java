package com.edupay.repository;

import com.edupay.entity.PaymentSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository quản lý Cấu hình thanh toán VietQR của giáo viên.
 * Lọc theo tenantId để đảm bảo cô lập Multi-Tenant (P0).
 */
@Repository
public interface PaymentSettingRepository extends JpaRepository<PaymentSetting, UUID> {

    Optional<PaymentSetting> findByTenantId(UUID tenantId);

    boolean existsByTenantId(UUID tenantId);
}
