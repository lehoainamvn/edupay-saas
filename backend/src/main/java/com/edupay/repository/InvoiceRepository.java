package com.edupay.repository;

import com.edupay.entity.Invoice;
import com.edupay.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository quản lý Phiếu thu học phí.
 * Phục vụ kiểm tra khóa (Locking Rule) để bảo vệ tính toàn vẹn công nợ khi sửa điểm danh.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    /**
     * Kiểm tra xem học sinh đã có phiếu thu hợp lệ trong kỳ thu (tháng) hay chưa.
     */
    boolean existsByTenantIdAndStudentIdAndBillingMonthAndStatusIn(
            UUID tenantId, UUID studentId, String billingMonth, Collection<InvoiceStatus> statuses);

    /**
     * Tìm danh sách ID các học sinh trong lớp đã có phiếu thu hợp lệ trong kỳ thu (tháng).
     */
    @Query("""
            SELECT i.studentId FROM Invoice i
            WHERE i.tenantId = :tenantId
              AND i.classId = :classId
              AND i.billingMonth = :billingMonth
              AND i.status IN :statuses
            """)
    Set<UUID> findLockedStudentIdsByClassAndMonth(
            @Param("tenantId") UUID tenantId,
            @Param("classId") UUID classId,
            @Param("billingMonth") String billingMonth,
            @Param("statuses") Collection<InvoiceStatus> statuses);

    /**
     * Tìm danh sách ID học sinh trong danh sách truyền vào đang có phiếu thu hợp lệ trong kỳ thu.
     */
    @Query("""
            SELECT i.studentId FROM Invoice i
            WHERE i.tenantId = :tenantId
              AND i.studentId IN :studentIds
              AND i.billingMonth = :billingMonth
              AND i.status IN :statuses
            """)
    Set<UUID> findLockedStudentIds(
            @Param("tenantId") UUID tenantId,
            @Param("studentIds") Collection<UUID> studentIds,
            @Param("billingMonth") String billingMonth,
            @Param("statuses") Collection<InvoiceStatus> statuses);

    Optional<Invoice> findByIdAndTenantId(UUID id, UUID tenantId);
}

