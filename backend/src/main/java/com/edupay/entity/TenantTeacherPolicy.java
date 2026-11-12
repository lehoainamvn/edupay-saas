package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_teacher_policies")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantTeacherPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "can_edit_student_fee", nullable = false)
    @Builder.Default
    private Boolean canEditStudentFee = false;

    @Column(name = "can_create_class", nullable = false)
    @Builder.Default
    private Boolean canCreateClass = false;

    @Column(name = "can_create_student", nullable = false)
    @Builder.Default
    private Boolean canCreateStudent = false;

    @Column(name = "can_view_billing", nullable = false)
    @Builder.Default
    private Boolean canViewBilling = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}

