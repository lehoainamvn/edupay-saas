package com.edupay.repository;

import com.edupay.entity.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemParameterRepository extends JpaRepository<SystemParameter, UUID> {

    List<SystemParameter> findAllByTenantIdOrderByParamGroupAscParamKeyAsc(UUID tenantId);

    List<SystemParameter> findAllByTenantIdAndParamGroupOrderByParamKeyAsc(UUID tenantId, String paramGroup);

    Optional<SystemParameter> findByTenantIdAndParamKey(UUID tenantId, String paramKey);

    boolean existsByTenantIdAndParamKey(UUID tenantId, String paramKey);

    long countByTenantId(UUID tenantId);
}

