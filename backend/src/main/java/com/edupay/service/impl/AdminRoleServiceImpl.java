package com.edupay.service.impl;

import com.edupay.dto.request.UpdateRolePermissionsRequest;
import com.edupay.dto.response.PermissionGroupResponse;
import com.edupay.dto.response.RoleResponse;
import com.edupay.entity.Permission;
import com.edupay.entity.Role;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.PermissionRepository;
import com.edupay.repository.RoleRepository;
import com.edupay.service.AdminRoleService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    private static final Map<String, String> RESOURCE_LABELS = Map.of(
            "class", "Quản lý Lớp học",
            "student", "Quản lý Học sinh",
            "attendance", "Quản lý Điểm danh",
            "invoice", "Học phí & VietQR",
            "report", "Báo cáo Doanh thu",
            "settings", "Cài đặt & Cấu hình",
            "user", "Quản lý Tài khoản & Nhân sự",
            "system", "Quản trị Hệ thống"
    );

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        roles.sort(Comparator.comparing(Role::getIsSystem).reversed().thenComparing(Role::getCode));

        return roles.stream().map(role -> {
            long userCount = roleRepository.countUsersByRoleId(role.getId());
            List<String> permissionCodes = roleRepository.findPermissionCodesByRoleId(role.getId());

            return RoleResponse.builder()
                    .id(role.getId())
                    .code(role.getCode())
                    .name(role.getName())
                    .description(role.getDescription())
                    .isSystem(role.getIsSystem())
                    .isActive(role.getIsActive())
                    .userCount(userCount)
                    .permissions(permissionCodes)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionGroupResponse> getAllPermissionsGrouped() {
        List<Permission> allPermissions = permissionRepository.findAll();

        Map<String, List<Permission>> byResource = allPermissions.stream()
                .collect(Collectors.groupingBy(Permission::getResource));

        List<PermissionGroupResponse> groups = new ArrayList<>();
        List<String> orderedResources = List.of("student", "class", "attendance", "invoice", "report", "settings", "user", "system");

        // Gom các resource đã định nghĩa theo thứ tự ưu tiên
        for (String res : orderedResources) {
            if (byResource.containsKey(res)) {
                groups.add(buildGroup(res, byResource.get(res)));
            }
        }

        // Bổ sung các resource khác nếu có
        for (Map.Entry<String, List<Permission>> entry : byResource.entrySet()) {
            if (!orderedResources.contains(entry.getKey())) {
                groups.add(buildGroup(entry.getKey(), entry.getValue()));
            }
        }

        return groups;
    }

    @Override
    @Transactional
    public RoleResponse updateRolePermissions(UUID roleId, UpdateRolePermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("ROLE_NOT_FOUND", "Không tìm thấy vai trò với ID: " + roleId));

        // Xóa liên kết quyền cũ
        entityManager.createNativeQuery("DELETE FROM role_permissions WHERE role_id = :roleId")
                .setParameter("roleId", roleId)
                .executeUpdate();

        // Gán liên kết quyền mới
        if (request.getPermissionCodes() != null && !request.getPermissionCodes().isEmpty()) {
            for (String code : request.getPermissionCodes()) {
                permissionRepository.findByCode(code.trim()).ifPresent(perm -> {
                    entityManager.createNativeQuery("""
                            INSERT INTO role_permissions (role_id, permission_id)
                            VALUES (:roleId, :permissionId)
                            ON CONFLICT DO NOTHING
                            """)
                            .setParameter("roleId", roleId)
                            .setParameter("permissionId", perm.getId())
                            .executeUpdate();
                });
            }
        }

        log.info("SuperAdmin cập nhật ma trận quyền cho Role [{}] ({} quyền)",
                role.getCode(), request.getPermissionCodes() != null ? request.getPermissionCodes().size() : 0);

        long userCount = roleRepository.countUsersByRoleId(role.getId());
        List<String> updatedPermissions = roleRepository.findPermissionCodesByRoleId(role.getId());

        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .isSystem(role.getIsSystem())
                .isActive(role.getIsActive())
                .userCount(userCount)
                .permissions(updatedPermissions)
                .build();
    }

    private PermissionGroupResponse buildGroup(String resource, List<Permission> permissions) {
        String label = RESOURCE_LABELS.getOrDefault(resource, "Quản lý " + resource);
        List<PermissionGroupResponse.PermissionItemResponse> items = permissions.stream()
                .sorted(Comparator.comparing(Permission::getCode))
                .map(p -> PermissionGroupResponse.PermissionItemResponse.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .action(p.getAction())
                        .build())
                .collect(Collectors.toList());

        return PermissionGroupResponse.builder()
                .resource(resource)
                .resourceName(label)
                .permissions(items)
                .build();
    }
}

