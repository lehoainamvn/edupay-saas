package com.edupay.repository;

import com.edupay.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository cho bảng audit_logs.
 * Hỗ trợ tra cứu audit log theo tenant, actor và khoảng thời gian.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /** Lấy audit logs theo tenant, sắp xếp mới nhất trước (phân trang) */
    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    /** Lấy audit logs theo actor user */
    List<AuditLog> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId);

    /** Lấy audit logs theo tenant và action type */
    List<AuditLog> findByTenantIdAndActionOrderByCreatedAtDesc(UUID tenantId, String action);

    /** Đếm số audit logs trong khoảng thời gian */
    long countByTenantIdAndCreatedAtBetween(UUID tenantId, Instant from, Instant to);
}

