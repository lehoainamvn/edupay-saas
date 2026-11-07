package com.edupay.repository;

import com.edupay.entity.UserFeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFeatureFlagRepository extends JpaRepository<UserFeatureFlag, UUID> {

    List<UserFeatureFlag> findAllByTenantIdAndUserId(UUID tenantId, UUID userId);

    Optional<UserFeatureFlag> findByTenantIdAndUserIdAndFeatureId(UUID tenantId, UUID userId, String featureId);
}

