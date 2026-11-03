package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity đại diện cho Phiếu thu học phí (bảng invoices).
 * Kế thừa BaseEntity để đảm bảo cô lập Multi-Tenant (tenant_id).
 */
@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoices_tenant", columnList = "tenant_id"),
        @Index(name = "idx_invoices_hash", columnList = "public_hash"),
        @Index(name = "idx_invoices_code", columnList = "transfer_code"),
        @Index(name = "idx_invoices_student_month", columnList = "tenant_id, student_id, billing_month")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth; // Định dạng 'YYYY-MM'

    @Column(name = "attended_count", nullable = false)
    @Builder.Default
    private Integer attendedCount = 0;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "extra_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal extraFee = BigDecimal.ZERO;

    @Column(name = "discount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "transfer_code", nullable = false, unique = true, length = 100)
    private String transferCode;

    @Column(name = "public_hash", nullable = false, unique = true, length = 64)
    private String publicHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "paid_method", length = 50)
    private String paidMethod;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}

