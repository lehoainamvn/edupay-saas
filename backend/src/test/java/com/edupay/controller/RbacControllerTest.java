package com.edupay.controller;

import com.edupay.dto.request.OverrideFeatureRequest;
import com.edupay.dto.response.FeatureFlagResponse;
import com.edupay.dto.response.MenuItemResponse;
import com.edupay.dto.response.UserNavigationResponse;
import com.edupay.security.GlobalRateLimitingFilter;
import com.edupay.security.JwtTokenProvider;
import com.edupay.security.RateLimitingFilter;
import com.edupay.security.SecurityHeadersFilter;
import com.edupay.service.RbacService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacController.class)
@AutoConfigureMockMvc(addFilters = false)
class RbacControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RbacService rbacService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private GlobalRateLimitingFilter globalRateLimitingFilter;

    @MockBean
    private SecurityHeadersFilter securityHeadersFilter;

    @Test
    @DisplayName("GET /me/navigation: Trả về 200 OK kèm cây menu và quyền")
    void getCurrentUserNavigation_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        UserNavigationResponse response = UserNavigationResponse.builder()
                .userId(userId)
                .tenantId(tenantId)
                .fullName("Cô Mai")
                .isOwner(true)
                .roles(List.of("OWNER"))
                .permissions(Set.of("class:read", "attendance:create"))
                .features(Map.of("ATTENDANCE", true))
                .menus(List.of(
                        MenuItemResponse.builder().code("CLASSES").name("Lớp học").route("/classes").sortOrder(1).build()
                ))
                .build();

        when(rbacService.getCurrentUserNavigation()).thenReturn(response);

        mockMvc.perform(get("/me/navigation")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Cô Mai"))
                .andExpect(jsonPath("$.data.roles[0]").value("OWNER"))
                .andExpect(jsonPath("$.data.menus[0].code").value("CLASSES"));
    }

    @Test
    @DisplayName("GET /features: Trả về danh sách Feature Flags")
    void getTenantFeatureFlags_returns200() throws Exception {
        List<FeatureFlagResponse> flags = List.of(
                FeatureFlagResponse.builder().featureId("ATTENDANCE").name("Điểm danh").enabled(true).source("PLAN").build(),
                FeatureFlagResponse.builder().featureId("AUTO_RECONCILE").name("Gạch nợ tự động").enabled(false).source("PLAN").build()
        );

        when(rbacService.getTenantFeatureFlags()).thenReturn(flags);

        mockMvc.perform(get("/features")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].featureId").value("ATTENDANCE"));
    }

    @Test
    @DisplayName("PUT /features/{featureId}/override: Cập nhật thành công trả về 200")
    void overrideTenantFeature_returns200() throws Exception {
        String featureId = "AUTO_RECONCILE";
        OverrideFeatureRequest request = OverrideFeatureRequest.builder()
                .isEnabled(true)
                .reason("Cấp thêm tính năng")
                .build();

        FeatureFlagResponse response = FeatureFlagResponse.builder()
                .featureId(featureId)
                .enabled(true)
                .source("TENANT_OVERRIDE")
                .reason("Cấp thêm tính năng")
                .build();

        when(rbacService.overrideTenantFeature(eq(featureId), any())).thenReturn(response);

        mockMvc.perform(put("/features/{featureId}/override", featureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.featureId").value(featureId))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }
}

