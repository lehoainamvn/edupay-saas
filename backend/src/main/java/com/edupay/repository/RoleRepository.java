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

    @Query(value = """
            SELECT DISTINCT r.code
            FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = :userId
            """, nativeQuery = true)
    List<String> findRoleCodesByUserId(@Param("userId") UUID userId);

    @Query(value = """
            SELECT COUNT(DISTINCT ur.user_id)
            FROM user_roles ur
            WHERE ur.role_id = :roleId
            """, nativeQuery = true)
    long countUsersByRoleId(@Param("roleId") UUID roleId);

    @Query(value = """
            SELECT p.code
            FROM permissions p
            JOIN role_permissions rp ON rp.permission_id = p.id
            WHERE rp.role_id = :roleId
            ORDER BY p.code ASC
            """, nativeQuery = true)
    List<String> findPermissionCodesByRoleId(@Param("roleId") UUID roleId);

    @Query(value = """
            SELECT DISTINCT ur.user_id
            FROM user_roles ur
            JOIN roles r ON r.id = ur.role_id
            WHERE UPPER(r.code) = UPPER(:roleCode)
            """, nativeQuery = true)
    List<UUID> findUserIdsByRoleCode(@Param("roleCode") String roleCode);
}

