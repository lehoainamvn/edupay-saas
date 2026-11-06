package com.edupay.security;

import com.edupay.config.TenantContext;
import com.edupay.security.annotation.AuditAction;
import com.edupay.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

/**
 * AOP Aspect tự động ghi Audit Trail cho các method được đánh {@literal @}AuditAction.
 *
 * Thứ tự thực hiện:
 * 1. Thu thập IP và User-Agent trên luồng HTTP chính (tránh lỗi request recycled khi chạy @Async)
 * 2. Method được gọi (proceed)
 * 3. Nếu thành công → trích xuất resourceId từ args hoặc return value, ghi log INFO/WARN
 * 4. Nếu ném exception → ghi log WARN kèm hậu tố _FAILED rồi rethrow
 *
 * Audit log được ghi @Async trong AuditLogServiceImpl — không block main thread.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditAction)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        String resourceId = extractResourceId(joinPoint.getArgs());

        UUID tenantId  = TenantContext.getTenantId();
        UUID actorId   = extractActorId();

        // Thu thập thông tin client TRÊN LUỒNG CHÍNH trước khi sang luồng @Async
        HttpServletRequest request = extractRequest();
        String ipAddress = resolveIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        try {
            Object result = joinPoint.proceed();

            // Nếu tham số chưa có resourceId, trích xuất từ kết quả trả về
            if (resourceId == null && result != null) {
                resourceId = extractResourceIdFromResult(result);
            }

            auditLogService.logAction(
                    tenantId,
                    actorId,
                    auditAction.action(),
                    auditAction.resourceType(),
                    resourceId,
                    null,
                    null,
                    auditAction.severity(),
                    ipAddress,
                    userAgent
            );

            return result;

        } catch (Throwable ex) {
            auditLogService.logAction(
                    tenantId,
                    actorId,
                    auditAction.action() + "_FAILED",
                    auditAction.resourceType(),
                    resourceId,
                    null,
                    null,
                    "WARN",
                    ipAddress,
                    userAgent
            );
            throw ex;
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Lấy UUID đầu tiên trong danh sách tham số làm resourceId */
    private String extractResourceId(Object[] args) {
        if (args == null || args.length == 0) return null;
        for (Object arg : args) {
            if (arg instanceof UUID uuid) return uuid.toString();
            if (arg instanceof String s && isValidUuid(s)) return s;
        }
        return null;
    }

    /** Trích xuất ID từ đối tượng trả về nếu có phương thức getId() hoặc là Collection */
    private String extractResourceIdFromResult(Object result) {
        if (result == null) return null;
        if (result instanceof Collection<?> coll) {
            return coll.size() + " mục";
        }
        try {
            Method getIdMethod = result.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(result);
            return id != null ? id.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Lấy userId từ SecurityContext */
    private UUID extractActorId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof String principal) {
                return UUID.fromString(principal);
            }
        } catch (Exception ignored) {
            // Không có authentication context (system events)
        }
        return null;
    }

    /** Lấy HttpServletRequest từ RequestContextHolder */
    private HttpServletRequest extractRequest() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Trích xuất IP an toàn từ HttpServletRequest */
    private String resolveIpAddress(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isValidUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
