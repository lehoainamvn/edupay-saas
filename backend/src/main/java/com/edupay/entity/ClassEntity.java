package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity đại diện cho Lớp học (bảng classes).
 * Luôn kế thừa BaseEntity để đảm bảo cô lập dữ liệu Multi-Tenant (tenant_id).
 * Sử dụng isActive (is_active) để thực hiện Soft Delete nhằm bảo toàn lịch sử điểm danh và phiếu thu.
 */
@Entity
@Table(name = "classes", indexes = {
        @Index(name = "idx_classes_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 50)
    @Builder.Default
    private FeeType feeType = FeeType.PER_LESSON;

    @Column(name = "default_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal defaultFee = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;
}
