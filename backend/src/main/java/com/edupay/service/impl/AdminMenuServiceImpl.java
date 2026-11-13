package com.edupay.service.impl;

import com.edupay.dto.request.CreateMenuRequest;
import com.edupay.dto.request.UpdateMenuRequest;
import com.edupay.dto.request.UpdateRoleMenusRequest;
import com.edupay.dto.response.MenuDto;
import com.edupay.dto.response.RoleMenuMatrixResponse;
import com.edupay.dto.response.RoleResponse;
import com.edupay.entity.Menu;
import com.edupay.entity.Role;
import com.edupay.entity.RoleMenu;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.MenuRepository;
import com.edupay.repository.RoleMenuRepository;
import com.edupay.repository.RoleRepository;
import com.edupay.service.AdminMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMenuServiceImpl implements AdminMenuService {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MenuDto> getAllMenus() {
        return menuRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"))
                .stream()
                .map(this::toMenuDto)
                .toList();
    }

    @Override
    @Transactional
    public MenuDto createMenu(CreateMenuRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (menuRepository.findByCode(code).isPresent()) {
            throw new BusinessException("MENU_CODE_EXISTS", "Mã menu đã tồn tại: " + code);
        }

        Menu menu = Menu.builder()
                .code(code)
                .name(request.getName().trim())
                .icon(request.getIcon() != null ? request.getIcon().trim() : "folder")
                .route(request.getRoute().trim())
                .parentId(request.getParentId())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .featureId(request.getFeatureId() != null && !request.getFeatureId().isBlank() ? request.getFeatureId().trim() : null)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Menu saved = menuRepository.save(menu);
        log.info("SuperAdmin đã tạo Menu mới [{}] với route [{}]", saved.getName(), saved.getRoute());
        return toMenuDto(saved);
    }

    @Override
    @Transactional
    public MenuDto updateMenu(UUID menuId, UpdateMenuRequest request) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("MENU_NOT_FOUND", "Không tìm thấy Menu với ID: " + menuId));

        menu.setName(request.getName().trim());
        if (request.getIcon() != null) {
            menu.setIcon(request.getIcon().trim());
        }
        menu.setRoute(request.getRoute().trim());
        menu.setParentId(request.getParentId());
        if (request.getSortOrder() != null) {
            menu.setSortOrder(request.getSortOrder());
        }
        menu.setFeatureId(request.getFeatureId() != null && !request.getFeatureId().isBlank() ? request.getFeatureId().trim() : null);
        if (request.getIsActive() != null) {
            menu.setIsActive(request.getIsActive());
        }

        Menu saved = menuRepository.save(menu);
        log.info("SuperAdmin đã cập nhật Menu [{}]", saved.getName());
        return toMenuDto(saved);
    }

    @Override
    @Transactional
    public void deleteMenu(UUID menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("MENU_NOT_FOUND", "Không tìm thấy Menu với ID: " + menuId));

        menuRepository.delete(menu);
        log.info("SuperAdmin đã xóa Menu [{}]", menu.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleMenuMatrixResponse getRoleMenuMatrix() {
        // Chỉ lấy các vai trò hệ thống hoặc vai trò chính
        List<Role> roles = roleRepository.findAll().stream()
                .filter(r -> r.getTenantId() == null)
                .sorted(Comparator.comparing(r -> getRolePriority(r.getCode())))
                .toList();

        List<RoleResponse> roleResponses = roles.stream().map(r -> RoleResponse.builder()
                .id(r.getId())
                .code(r.getCode())
                .name(r.getName())
                .description(r.getDescription())
                .isSystem(r.getIsSystem())
                .isActive(r.getIsActive())
                .build()
        ).toList();

        List<MenuDto> menus = menuRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"))
                .stream()
                .map(this::toMenuDto)
                .toList();

        List<RoleMenu> allRoleMenus = roleMenuRepository.findAll();
        Map<UUID, List<UUID>> roleMenuMap = new HashMap<>();
        for (Role r : roles) {
            roleMenuMap.put(r.getId(), new ArrayList<>());
        }
        for (RoleMenu rm : allRoleMenus) {
            if (roleMenuMap.containsKey(rm.getId().getRoleId())) {
                roleMenuMap.get(rm.getId().getRoleId()).add(rm.getId().getMenuId());
            }
        }

        return RoleMenuMatrixResponse.builder()
                .roles(roleResponses)
                .menus(menus)
                .roleMenuMap(roleMenuMap)
                .build();
    }

    @Override
    @Transactional
    public void updateRoleMenus(UpdateRoleMenusRequest request) {
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("ROLE_NOT_FOUND", "Không tìm thấy Vai trò với ID: " + request.getRoleId()));

        // Xóa các liên kết menu cũ của role này
        roleMenuRepository.deleteByRoleId(role.getId());

        // Thêm danh sách liên kết menu mới
        if (request.getMenuIds() != null && !request.getMenuIds().isEmpty()) {
            List<RoleMenu> newRoleMenus = request.getMenuIds().stream()
                    .distinct()
                    .map(menuId -> RoleMenu.builder()
                            .id(RoleMenu.RoleMenuId.builder()
                                    .roleId(role.getId())
                                    .menuId(menuId)
                                    .build())
                            .build())
                    .toList();
            roleMenuRepository.saveAll(newRoleMenus);
        }

        log.info("SuperAdmin cập nhật danh sách Menu được phép cho Vai trò [{}]: {} menu", role.getCode(), request.getMenuIds() != null ? request.getMenuIds().size() : 0);
    }

    private MenuDto toMenuDto(Menu m) {
        return MenuDto.builder()
                .id(m.getId())
                .code(m.getCode())
                .name(m.getName())
                .icon(m.getIcon())
                .route(m.getRoute())
                .parentId(m.getParentId())
                .sortOrder(m.getSortOrder())
                .featureId(m.getFeatureId())
                .isActive(m.getIsActive())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private int getRolePriority(String code) {
        return switch (code) {
            case "SUPER_ADMIN" -> 1;
            case "CENTER_ADMIN" -> 2;
            case "OWNER" -> 3;
            case "TEACHER" -> 4;
            case "SOLO_TUTOR" -> 5;
            case "VIEWER" -> 6;
            default -> 99;
        };
    }
}
