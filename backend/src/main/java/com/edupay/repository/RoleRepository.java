package com.edupay.repository;

import com.edupay.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCodeAndTenantIdIsNull(String code);

    Optional<Role> findByTenantIdAndCode(UUID tenantId, String code);

    @Query(value = """
            SELECT DISTINCT r.code
            FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = :userId AND ur.tenant_id = :tenantId
            """, nativeQuery = true)
    List<String> findRoleCodesByUserIdAndTenantId(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId
    );
}

