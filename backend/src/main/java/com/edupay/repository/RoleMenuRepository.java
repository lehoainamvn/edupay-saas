package com.edupay.repository;

import com.edupay.entity.Menu;
import com.edupay.entity.RoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleMenuRepository extends JpaRepository<RoleMenu, RoleMenu.RoleMenuId> {

    List<RoleMenu> findByIdRoleId(UUID roleId);

    List<RoleMenu> findByIdRoleIdIn(List<UUID> roleIds);

    @Query("""
            SELECT DISTINCT m
            FROM Menu m
            JOIN RoleMenu rm ON rm.id.menuId = m.id
            WHERE rm.id.roleId IN :roleIds
              AND m.isActive = true
            ORDER BY m.sortOrder ASC
            """)
    List<Menu> findAccessibleMenusByRoleIds(@Param("roleIds") List<UUID> roleIds);

    @Query(value = """
            SELECT DISTINCT m.id
            FROM menus m
            JOIN role_menus rm ON rm.menu_id = m.id
            JOIN roles r ON r.id = rm.role_id
            WHERE r.code IN :roleCodes
              AND m.is_active = true
            """, nativeQuery = true)
    List<UUID> findMenuIdsByRoleCodes(@Param("roleCodes") List<String> roleCodes);

    @Modifying
    @Query("DELETE FROM RoleMenu rm WHERE rm.id.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") UUID roleId);
}

