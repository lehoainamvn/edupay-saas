package com.edupay.security;

import com.edupay.config.TenantContext;
import com.edupay.security.annotation.AuditAction;
import com.edupay.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
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

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogAspect - AOP tự động ghi vết kiểm toán")
class AuditLogAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private AuditAction auditAction;

    @InjectMocks
    private AuditLogAspect aspect;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID resourceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
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
    @DisplayName("Thực thi thành công: ghi audit log với action gốc và INFO severity")
    void auditMethod_Success_LogsAction() throws Throwable {
        when(auditAction.action()).thenReturn("INVOICE_PAID");
        when(auditAction.resourceType()).thenReturn("INVOICE");
        when(auditAction.severity()).thenReturn("INFO");
        when(joinPoint.getArgs()).thenReturn(new Object[]{resourceId});
        when(joinPoint.proceed()).thenReturn("success-result");

        Object result = aspect.auditMethod(joinPoint, auditAction);

        assertEquals("success-result", result);
        verify(auditLogService).logAction(
                eq(tenantId),
                eq(userId),
                eq("INVOICE_PAID"),
                eq("INVOICE"),
                eq(resourceId.toString()),
                isNull(),
                isNull(),
                eq("INFO"),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Thực thi ném lỗi: ghi audit log với hậu tố _FAILED, severity WARN và rethrow exception")
    void auditMethod_ThrowsException_LogsFailedAndRethrows() throws Throwable {
        when(auditAction.action()).thenReturn("INVOICE_PAID");
        when(auditAction.resourceType()).thenReturn("INVOICE");
        when(joinPoint.getArgs()).thenReturn(new Object[]{resourceId});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                aspect.auditMethod(joinPoint, auditAction)
        );
        assertEquals("DB error", ex.getMessage());

        verify(auditLogService).logAction(
                eq(tenantId),
                eq(userId),
                eq("INVOICE_PAID_FAILED"),
                eq("INVOICE"),
                eq(resourceId.toString()),
                isNull(),
                isNull(),
                eq("WARN"),
                any(),
                any()
        );
    }
}

