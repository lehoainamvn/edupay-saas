package com.edupay.repository;

import com.edupay.entity.TenantTeacherPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantTeacherPolicyRepository extends JpaRepository<TenantTeacherPolicy, UUID> {

    Optional<TenantTeacherPolicy> findByTenantId(UUID tenantId);
}

