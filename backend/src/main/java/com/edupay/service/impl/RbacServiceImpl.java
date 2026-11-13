package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.OverrideFeatureRequest;
import com.edupay.dto.response.FeatureFlagResponse;
import com.edupay.dto.response.MenuItemResponse;
import com.edupay.dto.response.UserNavigationResponse;
import com.edupay.entity.Menu;
import com.edupay.entity.TenantFeatureFlag;
import com.edupay.entity.User;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.*;
import com.edupay.service.AuditLogService;
import com.edupay.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RbacServiceImpl implements RbacService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final MenuRepository menuRepository;
    private final TenantFeatureFlagRepository tenantFeatureFlagRepository;
    private final UserFeatureFlagRepository userFeatureFlagRepository;
    private final TenantRepository tenantRepository;
    private final AuditLogService auditLogService;
    private final com.edupay.repository.RoleMenuRepository roleMenuRepository;
    private final com.edupay.repository.TenantTeacherPolicyRepository policyRepository;

    @Override
    @Transactional(readOnly = true)
    public UserNavigationResponse getCurrentUserNavigation() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("TENANT_REQUIRED", "Không tìm thấy thông tin Tenant trong phiên làm việc");
        }

        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        String tenantType = tenantRepository.findById(tenantId)
                .map(com.edupay.entity.Tenant::getTenantType)
                .orElse("SOLO_TUTOR");

        // 1. Lấy danh sách Roles
        List<String> roles = roleRepository.findRoleCodesByUserIdAndTenantId(userId, tenantId);
        if (roles.isEmpty() && Boolean.TRUE.equals(user.getIsOwner())) {
            roles = List.of("OWNER");
        }

        // 2. Lấy danh sách Permissions thực tế (Role + User Overrides)
        Set<String> permissions = permissionRepository.findEffectivePermissionsByUserIdAndTenantId(userId, tenantId);
        if (Boolean.TRUE.equals(user.getIsOwner()) || roles.contains("OWNER")) {
            // Owner có toàn quyền: nạp toàn bộ danh mục permissions
            permissions = permissionRepository.findAll().stream()
                    .map(com.edupay.entity.Permission::getCode)
                    .collect(Collectors.toSet());
        }

        // 3. Tính toán Feature Flags đa cấp (Plan -> Tenant Override -> User Override)
        Map<String, Boolean> features = computeEffectiveFeatures(tenantId, userId);

        // 4. Lọc danh sách Menu động theo Quyền và Feature Flags
        List<MenuItemResponse> menus = computeAccessibleMenus(tenantId, roles, permissions, features);

        return UserNavigationResponse.builder()
                .userId(userId)
                .tenantId(tenantId)
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .isOwner(Boolean.TRUE.equals(user.getIsOwner()))
                .tenantType(tenantType)
                .roles(roles)
                .permissions(permissions)
                .features(features)
                .menus(menus)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> getTenantFeatureFlags() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("TENANT_REQUIRED", "Không tìm thấy thông tin Tenant");
        }

        List<Object[]> allFeatures = tenantFeatureFlagRepository.findAllActiveFeatures();
        Map<String, Boolean> planFeatures = getPlanFeatures(tenantId);
        Map<String, TenantFeatureFlag> tenantOverrides = tenantFeatureFlagRepository.findAllByTenantId(tenantId)
                .stream()
                .collect(Collectors.toMap(TenantFeatureFlag::getFeatureId, tf -> tf, (a, b) -> a));

        List<FeatureFlagResponse> responseList = new ArrayList<>();
        Instant now = Instant.now();

        for (Object[] row : allFeatures) {
            String featureId = (String) row[0];
            String name = (String) row[1];
            String description = (String) row[2];
            String category = (String) row[3];

            boolean isEnabled;
            String source;
            String reason = null;
            Instant expiresAt = null;

            TenantFeatureFlag tf = tenantOverrides.get(featureId);
            if (tf != null && (tf.getExpiresAt() == null || tf.getExpiresAt().isAfter(now))) {
                isEnabled = tf.getIsEnabled();
                source = "TENANT_OVERRIDE";
                reason = tf.getReason();
                expiresAt = tf.getExpiresAt();
            } else if (planFeatures.containsKey(featureId)) {
                isEnabled = planFeatures.get(featureId);
                source = "PLAN";
            } else {
                isEnabled = false;
                source = "DEFAULT";
            }

            responseList.add(FeatureFlagResponse.builder()
                    .featureId(featureId)
                    .name(name)
                    .description(description)
                    .category(category)
                    .enabled(isEnabled)
                    .source(source)
                    .reason(reason)
                    .expiresAt(expiresAt)
                    .build());
        }

        return responseList;
    }

    @Override
    @Transactional
    public FeatureFlagResponse overrideTenantFeature(String featureId, OverrideFeatureRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = getCurrentUserId();

        TenantFeatureFlag flag = tenantFeatureFlagRepository.findByTenantIdAndFeatureId(tenantId, featureId)
                .orElse(TenantFeatureFlag.builder()
                        .featureId(featureId)
                        .build());

        flag.setTenantId(tenantId);
        flag.setIsEnabled(request.getIsEnabled());
        flag.setReason(request.getReason());
        flag.setExpiresAt(request.getExpiresAt());

        TenantFeatureFlag saved = tenantFeatureFlagRepository.save(flag);

        auditLogService.logAction(tenantId, userId, "OVERRIDE_FEATURE_FLAG", "FEATURE",
                featureId, null, "enabled=" + request.getIsEnabled(), "INFO", null, null);

        log.info("Tenant {} đã override feature {}: enabled={}", tenantId, featureId, request.getIsEnabled());

        return FeatureFlagResponse.builder()
                .featureId(saved.getFeatureId())
                .enabled(saved.getIsEnabled())
                .source("TENANT_OVERRIDE")
                .reason(saved.getReason())
                .expiresAt(saved.getExpiresAt())
                .build();
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private Map<String, Boolean> computeEffectiveFeatures(UUID tenantId, UUID userId) {
        // Cấp 1: Features từ Subscription Plan
        Map<String, Boolean> features = new LinkedHashMap<>(getPlanFeatures(tenantId));

        // Cấp 2: Tenant Overrides (Addons / Beta)
        Instant now = Instant.now();
        List<TenantFeatureFlag> tenantFlags = tenantFeatureFlagRepository.findAllByTenantId(tenantId);
        for (TenantFeatureFlag tf : tenantFlags) {
            if (tf.getExpiresAt() == null || tf.getExpiresAt().isAfter(now)) {
                features.put(tf.getFeatureId(), tf.getIsEnabled());
            }
        }

        // Cấp 3: User Overrides
        if (userId != null) {
            List<com.edupay.entity.UserFeatureFlag> userFlags = userFeatureFlagRepository.findAllByTenantIdAndUserId(tenantId, userId);
            for (com.edupay.entity.UserFeatureFlag uf : userFlags) {
                features.put(uf.getFeatureId(), uf.getIsEnabled());
            }
        }

        return features;
    }

    private Map<String, Boolean> getPlanFeatures(UUID tenantId) {
        Map<String, Boolean> planFeatures = new LinkedHashMap<>();
        List<Object[]> rows = tenantFeatureFlagRepository.findPlanFeaturesByTenantId(tenantId);
        for (Object[] row : rows) {
            String fId = (String) row[0];
            Boolean enabled = (Boolean) row[1];
            planFeatures.put(fId, Boolean.TRUE.equals(enabled));
        }
        return planFeatures;
    }

    private List<MenuItemResponse> computeAccessibleMenus(UUID tenantId,
                                                          List<String> roles,
                                                          Set<String> permissions,
                                                          Map<String, Boolean> features) {
        List<Menu> allMenus = menuRepository.findAllByIsActiveTrueOrderBySortOrderAsc();
        List<Object[]> mappings = menuRepository.findAllMenuPermissionMappings();

        // Gom nhóm permissions yêu cầu theo menuId
        Map<UUID, Set<String>> menuPermissionsMap = new HashMap<>();
        for (Object[] row : mappings) {
            UUID menuId = UUID.fromString((String) row[0]);
            String permCode = (String) row[1];
            menuPermissionsMap.computeIfAbsent(menuId, k -> new HashSet<>()).add(permCode);
        }

        boolean isSuperAdmin = roles.contains("SUPER_ADMIN");
        boolean isOwner = roles.contains("OWNER") || roles.contains("CENTER_ADMIN");

        // Lấy danh sách menuIds được gán trực tiếp qua role_menus
        List<UUID> assignedMenuIds = roleMenuRepository.findMenuIdsByRoleCodes(roles);
        boolean hasRoleMenuConfig = assignedMenuIds != null && !assignedMenuIds.isEmpty();

        // Kiểm tra loại tenant (CENTER vs SOLO_TUTOR)
        String tenantType = "SOLO_TUTOR";
        var tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isPresent() && tenantOpt.get().getTenantType() != null) {
            tenantType = tenantOpt.get().getTenantType();
        }

        // Lấy chính sách giáo viên nội bộ nếu là GV trực thuộc
        boolean isSubTeacher = !isOwner && !isSuperAdmin;
        boolean canViewBilling = false;
        if (isSubTeacher) {
            var policyOpt = policyRepository.findByTenantId(tenantId);
            canViewBilling = policyOpt.map(p -> Boolean.TRUE.equals(p.getCanViewBilling())).orElse(false);
        }

        List<MenuItemResponse> result = new ArrayList<>();

        for (Menu m : allMenus) {
            // A. Đối với người dùng không phải SUPER_ADMIN: ẩn tất cả menu quản trị Admin
            boolean isAdminMenu = m.getCode().startsWith("ADMIN_") || "SYSTEM_CONFIG".equalsIgnoreCase(m.getCode());
            if (isAdminMenu && !isSuperAdmin) {
                continue;
            }

            // B. Nếu là gia sư tự do (SOLO_TUTOR): không hiển thị menu quản lý giáo viên trung tâm
            if ("CENTER_TEACHERS".equalsIgnoreCase(m.getCode()) && "SOLO_TUTOR".equalsIgnoreCase(tenantType)) {
                continue;
            }

            // C. Nếu là giáo viên trực thuộc (TEACHER): chặn menu quản lý giáo viên & kiểm tra quyền xem học phí
            if (isSubTeacher) {
                if ("CENTER_TEACHERS".equalsIgnoreCase(m.getCode())) {
                    continue;
                }
                if ("BILLING".equalsIgnoreCase(m.getCode()) && !canViewBilling) {
                    continue;
                }
            }

            // D. Kiểm tra bảng phân quyền Menu động (role_menus)
            if (hasRoleMenuConfig && !isSuperAdmin) {
                if (!assignedMenuIds.contains(m.getId())) {
                    continue; // Menu này không được cấp quyền cho bất kỳ role nào của user
                }
            }

            // E. Kiểm tra Feature Flag (nếu menu liên kết với feature)
            if (m.getFeatureId() != null && !m.getFeatureId().isBlank()) {
                boolean featureEnabled = Boolean.TRUE.equals(features.get(m.getFeatureId()));
                if (!featureEnabled) {
                    continue; // Feature chưa bật -> không hiển thị menu
                }
            }

            // F. Kiểm tra Quyền (Permissions) nếu có ràng buộc
            Set<String> requiredPerms = menuPermissionsMap.get(m.getId());
            if (requiredPerms != null && !requiredPerms.isEmpty() && !isOwner && !isSuperAdmin) {
                boolean hasAnyPerm = requiredPerms.stream().anyMatch(permissions::contains);
                if (!hasAnyPerm) {
                    continue;
                }
            }

            result.add(MenuItemResponse.builder()
                    .code(m.getCode())
                    .name(m.getName())
                    .icon(m.getIcon())
                    .route(m.getRoute())
                    .sortOrder(m.getSortOrder())
                    .build());
        }

        return result;
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new BusinessException("UNAUTHORIZED", "Người dùng chưa được xác thực");
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_PRINCIPAL", "Định dạng danh tính người dùng không hợp lệ");
        }
    }
}

