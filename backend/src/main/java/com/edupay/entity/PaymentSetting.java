package com.edupay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

/**
 * Entity đại diện cho Cấu hình Ngân hàng nhận tiền VietQR của Giáo viên (bảng payment_settings).
 * Kế thừa BaseEntity để đảm bảo cô lập Multi-Tenant (tenant_id).
 */
@Entity
@Table(name = "payment_settings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_payment", columnNames = {"tenant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSetting extends BaseEntity {

    @Column(name = "bank_bin", nullable = false, length = 20)
    private String bankBin;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    @Column(name = "transfer_prefix", length = 50)
    @Builder.Default
    private String transferPrefix = "HP";
}
