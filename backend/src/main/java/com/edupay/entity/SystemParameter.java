package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity đại diện cho một tham số cấu hình động của hệ thống (bảng system_parameters).
 * Kế thừa BaseEntity để đảm bảo cô lập Multi-Tenant (tenant_id).
 */
@Entity
@Table(name = "system_parameters", uniqueConstraints = {
        @UniqueConstraint(name = "uk_system_parameters_tenant_key", columnNames = {"tenant_id", "param_key"})
}, indexes = {
        @Index(name = "idx_sys_params_tenant", columnList = "tenant_id"),
        @Index(name = "idx_sys_params_tenant_group", columnList = "tenant_id, param_group")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemParameter extends BaseEntity {

    @Column(name = "param_key", nullable = false, length = 100)
    private String paramKey;

    @Column(name = "param_value", nullable = false, columnDefinition = "TEXT")
    private String paramValue;

    @Column(name = "param_group", nullable = false, length = 50)
    private String paramGroup;

    @Column(name = "data_type", nullable = false, length = 20)
    @Builder.Default
    private String dataType = "STRING";

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;
}

