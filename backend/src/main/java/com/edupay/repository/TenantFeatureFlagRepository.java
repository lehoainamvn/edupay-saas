package com.edupay.repository;

import com.edupay.entity.TenantFeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantFeatureFlagRepository extends JpaRepository<TenantFeatureFlag, UUID> {

    List<TenantFeatureFlag> findAllByTenantId(UUID tenantId);

    Optional<TenantFeatureFlag> findByTenantIdAndFeatureId(UUID tenantId, String featureId);

    /**
     * Lấy các tính năng mặc định từ gói cước của Tenant (plan_features).
     */
    @Query(value = """
            SELECT pf.feature_id, pf.is_enabled
            FROM plan_features pf
            JOIN tenants t ON t.subscription_plan = pf.plan_id
            WHERE t.id = :tenantId
            """, nativeQuery = true)
    List<Object[]> findPlanFeaturesByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Lấy danh sách toàn bộ tính năng trong bảng features kèm thông tin mô tả.
     */
    @Query(value = """
            SELECT f.id, f.name, f.description, f.category
            FROM features f
            WHERE f.is_active = TRUE
            ORDER BY f.category, f.id
            """, nativeQuery = true)
    List<Object[]> findAllActiveFeatures();
}

