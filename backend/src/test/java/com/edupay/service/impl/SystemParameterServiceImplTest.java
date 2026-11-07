package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.UpdateSystemParameterRequest;
import com.edupay.dto.response.SystemParameterResponse;
import com.edupay.entity.SystemParameter;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.SystemParameterRepository;
import com.edupay.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemParameterServiceImplTest {

    @Mock
    private SystemParameterRepository systemParameterRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private SystemParameterServiceImpl systemParameterService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private SystemParameter createParam(String key, String group, String type, String value) {
        SystemParameter param = SystemParameter.builder()
                .paramKey(key)
                .paramGroup(group)
                .dataType(type)
                .paramValue(value)
                .displayName("Display " + key)
                .description("Desc " + key)
                .isSystem(false)
                .build();
        param.setId(UUID.randomUUID());
        param.setTenantId(tenantId);
        return param;
    }

    @Test
    @DisplayName("getAllParameters - Đọc danh sách và cache in-memory lần thứ 2 không query DB")
    void testGetAllParameters_CacheHit() {
        SystemParameter p1 = createParam("TUITION_DUE_DAYS", "TUITION", "NUMBER", "7");
        SystemParameter p2 = createParam("SYSTEM_CENTER_NAME", "SYSTEM", "STRING", "EduPay Demo");

        when(systemParameterRepository.findAllByTenantIdOrderByParamGroupAscParamKeyAsc(tenantId))
                .thenReturn(List.of(p1, p2));

        // Lần 1: query DB
        List<SystemParameterResponse> list1 = systemParameterService.getAllParameters("ALL", null);
        assertEquals(2, list1.size());

        // Lần 2: lấy từ in-memory cache, không query lại DB
        List<SystemParameterResponse> list2 = systemParameterService.getAllParameters("ALL", null);
        assertEquals(2, list2.size());

        verify(systemParameterRepository, times(1))
                .findAllByTenantIdOrderByParamGroupAscParamKeyAsc(tenantId);
    }

    @Test
    @DisplayName("getAllParameters - Lọc theo nhóm và từ khóa")
    void testGetAllParameters_FilterAndKeyword() {
        SystemParameter p1 = createParam("TUITION_DUE_DAYS", "TUITION", "NUMBER", "7");
        SystemParameter p2 = createParam("SYSTEM_CENTER_NAME", "SYSTEM", "STRING", "EduPay Demo");

        when(systemParameterRepository.findAllByTenantIdOrderByParamGroupAscParamKeyAsc(tenantId))
                .thenReturn(List.of(p1, p2));

        List<SystemParameterResponse> tuitionParams = systemParameterService.getAllParameters("TUITION", null);
        assertEquals(1, tuitionParams.size());
        assertEquals("TUITION_DUE_DAYS", tuitionParams.get(0).getParamKey());

        List<SystemParameterResponse> searchResult = systemParameterService.getAllParameters("ALL", "center");
        assertEquals(1, searchResult.size());
        assertEquals("SYSTEM_CENTER_NAME", searchResult.get(0).getParamKey());
    }

    @Test
    @DisplayName("updateParameter - Cập nhật thành công và hot-reload cache ngay tức thì")
    void testUpdateParameter_Success() {
        SystemParameter p = createParam("TUITION_DUE_DAYS", "TUITION", "NUMBER", "7");
        when(systemParameterRepository.findByTenantIdAndParamKey(tenantId, "TUITION_DUE_DAYS"))
                .thenReturn(Optional.of(p));
        when(systemParameterRepository.save(any(SystemParameter.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateSystemParameterRequest req = new UpdateSystemParameterRequest("10");
        SystemParameterResponse updated = systemParameterService.updateParameter("TUITION_DUE_DAYS", req);

        assertEquals("10", updated.getParamValue());

        // Kiểm tra hàm truy xuất nhanh 0ms đọc được giá trị mới ngay tức thì
        int cachedValue = systemParameterService.getIntValue("TUITION_DUE_DAYS", 7);
        assertEquals(10, cachedValue);

        verify(auditLogService, times(1)).logAction(
                eq(tenantId), any(), eq("UPDATE_SYSTEM_PARAM"), eq("SYSTEM_PARAMETER"),
                eq("TUITION_DUE_DAYS"), eq("7"), eq("10"), eq("INFO"), any(), any()
        );
    }

    @Test
    @DisplayName("updateParameter - Báo lỗi khi giá trị số không hợp lệ hoặc ngoài giới hạn")
    void testUpdateParameter_InvalidNumber() {
        SystemParameter p = createParam("TUITION_DUE_DAYS", "TUITION", "NUMBER", "7");
        when(systemParameterRepository.findByTenantIdAndParamKey(tenantId, "TUITION_DUE_DAYS"))
                .thenReturn(Optional.of(p));

        UpdateSystemParameterRequest req1 = new UpdateSystemParameterRequest("abc");
        assertThrows(BusinessException.class, () ->
                systemParameterService.updateParameter("TUITION_DUE_DAYS", req1));

        UpdateSystemParameterRequest req2 = new UpdateSystemParameterRequest("200"); // > 90 ngày
        assertThrows(BusinessException.class, () ->
                systemParameterService.updateParameter("TUITION_DUE_DAYS", req2));

        UpdateSystemParameterRequest req3 = new UpdateSystemParameterRequest("7.5"); // decimal
        assertThrows(BusinessException.class, () ->
                systemParameterService.updateParameter("TUITION_DUE_DAYS", req3));
    }

    @Test
    @DisplayName("updateParameter - Báo lỗi khi giá trị boolean không hợp lệ")
    void testUpdateParameter_InvalidBoolean() {
        SystemParameter p = createParam("NOTIFICATION_AUTO_REMINDER", "NOTIFICATION", "BOOLEAN", "true");
        when(systemParameterRepository.findByTenantIdAndParamKey(tenantId, "NOTIFICATION_AUTO_REMINDER"))
                .thenReturn(Optional.of(p));

        UpdateSystemParameterRequest req = new UpdateSystemParameterRequest("maybe");
        assertThrows(BusinessException.class, () ->
                systemParameterService.updateParameter("NOTIFICATION_AUTO_REMINDER", req));
    }

    @Test
    @DisplayName("getParameterByKey - Throw ResourceNotFoundException nếu key không tồn tại")
    void testGetParameterByKey_NotFound() {
        when(systemParameterRepository.findByTenantIdAndParamKey(tenantId, "UNKNOWN_KEY"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                systemParameterService.getParameterByKey("UNKNOWN_KEY"));
    }

    @Test
    @DisplayName("Fast lookup - getIntValue, getBooleanValue, getStringValue hoạt động chính xác")
    void testFastLookups() {
        SystemParameter p1 = createParam("TUITION_DUE_DAYS", "TUITION", "NUMBER", "14");
        SystemParameter p2 = createParam("NOTIFICATION_AUTO_REMINDER", "NOTIFICATION", "BOOLEAN", "true");
        SystemParameter p3 = createParam("SYSTEM_CENTER_NAME", "SYSTEM", "STRING", "EduPay HaNoi");

        when(systemParameterRepository.findAllByTenantIdOrderByParamGroupAscParamKeyAsc(tenantId))
                .thenReturn(List.of(p1, p2, p3));

        assertEquals(14, systemParameterService.getIntValue("TUITION_DUE_DAYS", 7));
        assertTrue(systemParameterService.getBooleanValue("NOTIFICATION_AUTO_REMINDER", false));
        assertEquals("EduPay HaNoi", systemParameterService.getStringValue("SYSTEM_CENTER_NAME", "Default"));

        // Fallback default khi key không tồn tại
        assertEquals(99, systemParameterService.getIntValue("NON_EXISTENT", 99));
        assertFalse(systemParameterService.getBooleanValue("NON_EXISTENT", false));
        assertEquals("Fallback", systemParameterService.getStringValue("NON_EXISTENT", "Fallback"));
    }
}

