package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho Log giao dịch thanh toán (bảng payment_transactions).
 * Lưu vết đối soát tự động từ Webhook (SePay, Casso) hoặc xác nhận gạch nợ thủ công.
 */
@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_tx_tenant", columnList = "tenant_id"),
        @Index(name = "idx_payment_tx_invoice", columnList = "invoice_id"),
        @Index(name = "idx_payment_tx_ref", columnList = "reference_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends BaseEntity {

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "reference_number", nullable = false, unique = true, length = 100)
    private String referenceNumber;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_brand_name", length = 50)
    private String bankBrandName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "transaction_content", columnDefinition = "TEXT")
    private String transactionContent;

    @Column(name = "payment_source", length = 50)
    @Builder.Default
    private String paymentSource = "WEBHOOK"; // WEBHOOK, MANUAL_CASH, MANUAL_TRANSFER

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "transaction_time")
    @Builder.Default
    private Instant transactionTime = Instant.now();
}
