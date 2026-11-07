package com.edupay.service.impl;

import com.edupay.entity.AuditLog;
import com.edupay.repository.AuditLogRepository;
import com.edupay.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Triển khai AuditLogService — ghi audit log bất đồng bộ.
 *
 * @Async: chạy trong thread riêng để không làm chậm main business transaction.
 * Propagation.REQUIRES_NEW: transaction riêng — audit log được ghi kể cả khi main tx rollback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(UUID tenantId,
                          UUID actorUserId,
                          String action,
                          String resourceType,
                          String resourceId,
                          String beforeJson,
                          String afterJson,
                          String severity,
                          String ipAddress,
                          String userAgent) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .tenantId(tenantId)
                    .actorUserId(actorUserId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .beforeValue(beforeJson)
                    .afterValue(afterJson)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .severity(severity != null ? severity : "INFO")
                    .createdAt(Instant.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log ghi thành công: action={}, resourceType={}, resourceId={}, tenantId={}",
                    action, resourceType, resourceId, tenantId);

        } catch (Exception e) {
            // Audit log thất bại KHÔNG được làm crash main flow
            log.error("Lỗi ghi audit log: action={}, error={}", action, e.getMessage());
        }
    }

    @Override
    @Async
    public void logInfo(UUID tenantId, UUID actorUserId, String action,
                        String resourceType, String resourceId,
                        String ipAddress, String userAgent) {
        logAction(tenantId, actorUserId, action, resourceType, resourceId,
                null, null, "INFO", ipAddress, userAgent);
    }

    @Override
    @Async
    public void logWarn(UUID tenantId, UUID actorUserId, String action,
                        String resourceType, String resourceId,
                        String ipAddress, String userAgent) {
        logAction(tenantId, actorUserId, action, resourceType, resourceId,
                null, null, "WARN", ipAddress, userAgent);
    }
}
