package com.edupay.repository;

import com.edupay.entity.Invoice;
import com.edupay.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository quản lý Phiếu thu học phí (bảng invoices).
 * Luôn cô lập dữ liệu theo tenantId (P0), ngoại trừ tra cứu public qua publicHash.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    /**
     * Kiểm tra xem học sinh đã có phiếu thu hợp lệ trong kỳ thu (tháng) hay chưa.
     */
    boolean existsByTenantIdAndStudentIdAndBillingMonthAndStatusIn(
            UUID tenantId, UUID studentId, String billingMonth, Collection<InvoiceStatus> statuses);

    /**
     * Tìm phiếu thu của học sinh trong kỳ thu (nếu có).
     */
    Optional<Invoice> findFirstByTenantIdAndStudentIdAndBillingMonthAndStatusIn(
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

    List<Invoice> findAllByTenantIdAndClassIdAndBillingMonthOrderByCreatedAtDesc(
            UUID tenantId, UUID classId, String billingMonth);

    List<Invoice> findAllByTenantIdAndClassIdAndBillingMonthAndStatusOrderByCreatedAtDesc(
            UUID tenantId, UUID classId, String billingMonth, InvoiceStatus status);

    List<Invoice> findAllByTenantIdAndBillingMonthOrderByCreatedAtDesc(UUID tenantId, String billingMonth);

    /**
     * Tra cứu phiếu thu bằng public_hash (Dành cho Cổng Phụ huynh công khai không cần login).
     */
    Optional<Invoice> findByPublicHash(String publicHash);

    boolean existsByTransferCode(String transferCode);

    boolean existsByPublicHash(String publicHash);
}
