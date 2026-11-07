package com.edupay.repository;

import com.edupay.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuRepository extends JpaRepository<Menu, UUID> {

    List<Menu> findAllByIsActiveTrueOrderBySortOrderAsc();

    Optional<Menu> findByCode(String code);

    /**
     * Lấy danh sách các mã quyền (permission code) yêu cầu cho một menu cụ thể.
     */
    @Query(value = """
            SELECT p.code
            FROM menu_permissions mp
            JOIN permissions p ON p.id = mp.permission_id
            WHERE mp.menu_id = :menuId
            """, nativeQuery = true)
    List<String> findRequiredPermissionCodesByMenuId(@Param("menuId") UUID menuId);

    /**
     * Lấy toàn bộ mapping: Menu ID -> Permission Code
     */
    @Query(value = """
            SELECT CAST(mp.menu_id AS VARCHAR) as menu_id, p.code as permission_code
            FROM menu_permissions mp
            JOIN permissions p ON p.id = mp.permission_id
            """, nativeQuery = true)
    List<Object[]> findAllMenuPermissionMappings();
}

