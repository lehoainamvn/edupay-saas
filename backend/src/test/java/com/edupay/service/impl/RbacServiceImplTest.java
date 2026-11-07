package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.OverrideFeatureRequest;
import com.edupay.dto.response.FeatureFlagResponse;
import com.edupay.dto.response.MenuItemResponse;
import com.edupay.dto.response.UserNavigationResponse;
import com.edupay.entity.Menu;
import com.edupay.entity.Permission;
import com.edupay.entity.TenantFeatureFlag;
import com.edupay.entity.User;
import com.edupay.repository.*;
import com.edupay.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private TenantFeatureFlagRepository tenantFeatureFlagRepository;

    @Mock
    private UserFeatureFlagRepository userFeatureFlagRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RbacServiceImpl rbacService;

    private UUID tenantId;
    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        testUser = User.builder()
                .phone("0912345678")
                .fullName("Cô Mai")
                .isOwner(true)
                .build();
        testUser.setId(userId);
        testUser.setTenantId(tenantId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Lấy navigation cho Owner: Trả về đầy đủ menu và full quyền")
    void getCurrentUserNavigation_whenOwner_returnsAllMenusAndPermissions() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findRoleCodesByUserIdAndTenantId(userId, tenantId)).thenReturn(List.of("OWNER"));
        when(permissionRepository.findAll()).thenReturn(List.of(
                Permission.builder().code("class:read").build(),
                Permission.builder().code("attendance:create").build()
        ));
        when(tenantFeatureFlagRepository.findPlanFeaturesByTenantId(tenantId)).thenReturn(List.of(
                new Object[]{"CLASS_MANAGEMENT", true},
                new Object[]{"ATTENDANCE", true}
        ));
        when(tenantFeatureFlagRepository.findAllByTenantId(tenantId)).thenReturn(Collections.emptyList());
        when(userFeatureFlagRepository.findAllByTenantIdAndUserId(tenantId, userId)).thenReturn(Collections.emptyList());

        Menu m1 = Menu.builder().code("CLASSES").name("Lớp học").route("/classes").sortOrder(1).featureId("CLASS_MANAGEMENT").build();
        m1.setId(UUID.randomUUID());
        when(menuRepository.findAllByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(m1));
        when(menuRepository.findAllMenuPermissionMappings()).thenReturn(Collections.emptyList());

        // When
        UserNavigationResponse response = rbacService.getCurrentUserNavigation();

        // Then
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertTrue(response.isOwner());
        assertTrue(response.getRoles().contains("OWNER"));
        assertTrue(response.getPermissions().contains("class:read"));
        assertEquals(1, response.getMenus().size());
        assertEquals("CLASSES", response.getMenus().get(0).getCode());
    }

    @Test
    @DisplayName("Lấy navigation cho Teacher: Lọc bỏ menu mà teacher không có quyền")
    void getCurrentUserNavigation_whenTeacher_filtersUnauthorizedMenus() {
        // Given
        testUser.setIsOwner(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findRoleCodesByUserIdAndTenantId(userId, tenantId)).thenReturn(List.of("TEACHER"));
        when(permissionRepository.findEffectivePermissionsByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Set.of("attendance:read"));

        when(tenantFeatureFlagRepository.findPlanFeaturesByTenantId(tenantId)).thenReturn(Collections.emptyList());
        when(tenantFeatureFlagRepository.findAllByTenantId(tenantId)).thenReturn(Collections.emptyList());
        when(userFeatureFlagRepository.findAllByTenantIdAndUserId(tenantId, userId)).thenReturn(Collections.emptyList());

        UUID m1Id = UUID.randomUUID();
        Menu m1 = Menu.builder().code("ATTENDANCE").name("Điểm danh").route("/attendance").sortOrder(1).build();
        m1.setId(m1Id);

        UUID m2Id = UUID.randomUUID();
        Menu m2 = Menu.builder().code("SETTINGS").name("Cài đặt").route("/settings").sortOrder(2).build();
        m2.setId(m2Id);

        when(menuRepository.findAllByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(m1, m2));
        when(menuRepository.findAllMenuPermissionMappings()).thenReturn(List.of(
                new Object[]{m1Id.toString(), "attendance:read"},
                new Object[]{m2Id.toString(), "settings:read"}
        ));

        // When
        UserNavigationResponse response = rbacService.getCurrentUserNavigation();

        // Then
        assertNotNull(response);
        assertFalse(response.isOwner());
        // Chỉ có quyền attendance:read -> chỉ nhìn thấy menu ATTENDANCE, không thấy SETTINGS
        assertEquals(1, response.getMenus().size());
        assertEquals("ATTENDANCE", response.getMenus().get(0).getCode());
    }

    @Test
    @DisplayName("Lấy danh sách Feature Flags của Tenant: Kết hợp Plan Features và Tenant Overrides")
    void getTenantFeatureFlags_returnsCombinedFeatures() {
        // Given
        when(tenantFeatureFlagRepository.findAllActiveFeatures()).thenReturn(List.of(
                new Object[]{"AUTO_RECONCILE", "Gạch nợ tự động", "Mô tả", "ADDON"},
                new Object[]{"ATTENDANCE", "Điểm danh", "Mô tả", "CORE"}
        ));
        when(tenantFeatureFlagRepository.findPlanFeaturesByTenantId(tenantId)).thenReturn(List.of(
                new Object[]{"ATTENDANCE", true},
                new Object[]{"AUTO_RECONCILE", false}
        ));
        // Tenant có override mua thêm AUTO_RECONCILE
        TenantFeatureFlag override = TenantFeatureFlag.builder()
                .featureId("AUTO_RECONCILE")
                .isEnabled(true)
                .reason("MUA_ADDON")
                .build();
        when(tenantFeatureFlagRepository.findAllByTenantId(tenantId)).thenReturn(List.of(override));

        // When
        List<FeatureFlagResponse> result = rbacService.getTenantFeatureFlags();

        // Then
        assertEquals(2, result.size());
        FeatureFlagResponse autoReconcile = result.stream().filter(f -> f.getFeatureId().equals("AUTO_RECONCILE")).findFirst().orElseThrow();
        assertTrue(autoReconcile.isEnabled());
        assertEquals("TENANT_OVERRIDE", autoReconcile.getSource());
    }

    @Test
    @DisplayName("Override tính năng cho Tenant: Lưu thành công và ghi log kiểm toán")
    void overrideTenantFeature_success() {
        // Given
        String featureId = "AUTO_RECONCILE";
        OverrideFeatureRequest request = OverrideFeatureRequest.builder()
                .isEnabled(true)
                .reason("Khuyến mãi trung tâm mới")
                .build();

        TenantFeatureFlag flag = TenantFeatureFlag.builder()
                .featureId(featureId)
                .isEnabled(true)
                .reason("Khuyến mãi trung tâm mới")
                .build();
        flag.setTenantId(tenantId);

        when(tenantFeatureFlagRepository.findByTenantIdAndFeatureId(tenantId, featureId)).thenReturn(Optional.empty());
        when(tenantFeatureFlagRepository.save(any(TenantFeatureFlag.class))).thenReturn(flag);

        // When
        FeatureFlagResponse response = rbacService.overrideTenantFeature(featureId, request);

        // Then
        assertNotNull(response);
        assertEquals(featureId, response.getFeatureId());
        assertTrue(response.isEnabled());
        assertEquals("TENANT_OVERRIDE", response.getSource());
        verify(auditLogService, times(1)).logAction(eq(tenantId), eq(userId), eq("OVERRIDE_FEATURE_FLAG"), eq("FEATURE"), eq(featureId), any(), any(), eq("INFO"), any(), any());
    }
}

