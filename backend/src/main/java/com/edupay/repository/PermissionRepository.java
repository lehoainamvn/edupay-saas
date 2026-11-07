package com.edupay.repository;

import com.edupay.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    /**
     * Truy vấn toàn bộ mã quyền hạn thực tế của User trong Tenant.
     * Tự động tổng hợp từ Role của User + Ghi đè cấp thêm (is_granted = true) - Thu hồi (is_granted = false).
     */
    @Query(value = """
            (
                SELECT DISTINCT p.code
                FROM permissions p
                JOIN role_permissions rp ON rp.permission_id = p.id
                JOIN roles r ON r.id = rp.role_id
                JOIN user_roles ur ON ur.role_id = r.id
                WHERE ur.user_id = :userId AND ur.tenant_id = :tenantId
            )
            UNION
            (
                SELECT p.code
                FROM permissions p
                JOIN user_permission_overrides upo ON upo.permission_id = p.id
                WHERE upo.user_id = :userId AND upo.tenant_id = :tenantId AND upo.is_granted = TRUE
            )
            EXCEPT
            (
                SELECT p.code
                FROM permissions p
                JOIN user_permission_overrides upo ON upo.permission_id = p.id
                WHERE upo.user_id = :userId AND upo.tenant_id = :tenantId AND upo.is_granted = FALSE
            )
            """, nativeQuery = true)
    Set<String> findEffectivePermissionsByUserIdAndTenantId(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId
    );
}

