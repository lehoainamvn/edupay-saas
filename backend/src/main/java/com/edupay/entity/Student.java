package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity đại diện cho Học sinh (bảng students).
 * Kế thừa BaseEntity để đảm bảo cô lập dữ liệu Multi-Tenant (tenant_id).
 */
@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_students_tenant", columnList = "tenant_id"),
        @Index(name = "idx_students_class", columnList = "class_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student extends BaseEntity {

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "parent_name", length = 255)
    private String parentName;

    @Column(name = "parent_phone", length = 20)
    private String parentPhone;

    @Column(name = "custom_fee", precision = 12, scale = 2)
    private BigDecimal customFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private StudentStatus status = StudentStatus.STUDYING;
}
