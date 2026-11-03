package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity đại diện cho buổi điểm danh từng học sinh (bảng attendance).
 * Luôn kế thừa BaseEntity để đảm bảo cô lập dữ liệu Multi-Tenant (tenant_id).
 */
@Entity
@Table(name = "attendance", indexes = {
        @Index(name = "idx_attendance_tenant", columnList = "tenant_id"),
        @Index(name = "idx_attendance_date", columnList = "lesson_date"),
        @Index(name = "idx_attendance_class_date", columnList = "tenant_id, class_id, lesson_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_lesson_date", columnNames = {"student_id", "lesson_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance extends BaseEntity {

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Convert(converter = AttendanceStatusConverter.class)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "note", length = 255)
    private String note;
}

