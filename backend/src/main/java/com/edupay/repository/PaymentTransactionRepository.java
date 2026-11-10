package com.edupay.repository;

import com.edupay.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository quản lý Log giao dịch thanh toán (payment_transactions).
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    /**
     * Kiểm tra tính duy nhất (Idempotency) của mã tham chiếu ngân hàng (SePay / Casso).
     */
    boolean existsByReferenceNumber(String referenceNumber);

    Optional<PaymentTransaction> findByReferenceNumber(String referenceNumber);

    List<PaymentTransaction> findAllByTenantIdOrderByTransactionTimeDesc(UUID tenantId);

    List<PaymentTransaction> findAllByInvoiceId(UUID invoiceId);
}
