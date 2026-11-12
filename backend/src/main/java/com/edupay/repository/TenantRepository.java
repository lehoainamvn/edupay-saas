package com.edupay.repository;

import com.edupay.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByPhone(String phone);

    List<Tenant> findAllByOrderByCreatedAtDesc();

    List<Tenant> findAllByTenantTypeOrderByCreatedAtDesc(String tenantType);

    long countBySubscriptionStatus(String subscriptionStatus);

    long countByTenantType(String tenantType);

    long countByTenantTypeAndSubscriptionStatus(String tenantType, String subscriptionStatus);

    long countByCreatedAtAfter(Instant afterTime);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.subscriptionPlan = 'MONTHLY' AND t.subscriptionStatus = 'ACTIVE'")
    long countActiveMonthlyTenants();

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.subscriptionPlan = 'ANNUAL' AND t.subscriptionStatus = 'ACTIVE'")
    long countActiveAnnualTenants();
}
