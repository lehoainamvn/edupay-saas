package com.edupay.controller;

import com.edupay.dto.request.UpdateSystemParameterRequest;
import com.edupay.dto.response.SystemParameterResponse;
import com.edupay.security.GlobalRateLimitingFilter;
import com.edupay.security.JwtTokenProvider;
import com.edupay.security.RateLimitingFilter;
import com.edupay.security.SecurityHeadersFilter;
import com.edupay.service.SystemParameterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemParameterController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemParameterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemParameterService systemParameterService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private GlobalRateLimitingFilter globalRateLimitingFilter;

    @MockBean
    private SecurityHeadersFilter securityHeadersFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /settings/parameters - Trả về 200 OK và danh sách tham số")
    void testGetAllParameters() throws Exception {
        SystemParameterResponse p = SystemParameterResponse.builder()
                .id(UUID.randomUUID())
                .paramKey("TUITION_DUE_DAYS")
                .paramValue("7")
                .paramGroup("TUITION")
                .dataType("NUMBER")
                .displayName("Hạn thanh toán học phí (ngày)")
                .description("Số ngày phụ huynh được thanh toán")
                .isSystem(false)
                .updatedAt(Instant.now())
                .build();

        when(systemParameterService.getAllParameters(null, null)).thenReturn(List.of(p));

        mockMvc.perform(get("/settings/parameters")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].paramKey").value("TUITION_DUE_DAYS"))
                .andExpect(jsonPath("$.data[0].paramValue").value("7"));
    }

    @Test
    @DisplayName("PUT /settings/parameters/{key} - Cập nhật tham số thành công")
    void testUpdateParameter() throws Exception {
        UpdateSystemParameterRequest req = new UpdateSystemParameterRequest("10");

        SystemParameterResponse updated = SystemParameterResponse.builder()
                .id(UUID.randomUUID())
                .paramKey("TUITION_DUE_DAYS")
                .paramValue("10")
                .paramGroup("TUITION")
                .dataType("NUMBER")
                .displayName("Hạn thanh toán học phí (ngày)")
                .isSystem(false)
                .updatedAt(Instant.now())
                .build();

        when(systemParameterService.updateParameter(eq("TUITION_DUE_DAYS"), any(UpdateSystemParameterRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/settings/parameters/TUITION_DUE_DAYS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paramValue").value("10"));
    }

    @Test
    @DisplayName("POST /settings/parameters/reload-cache - Gọi service reloadCache thành công")
    void testReloadCache() throws Exception {
        mockMvc.perform(post("/settings/parameters/reload-cache")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(systemParameterService).reloadCache();
    }
}

