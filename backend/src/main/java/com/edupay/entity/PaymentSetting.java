package com.edupay.entity;

import com.edupay.entity.converter.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity đại diện cho Cấu hình Ngân hàng nhận tiền VietQR của Giáo viên (bảng payment_settings).
 * Kế thừa BaseEntity để đảm bảo cô lập Multi-Tenant (tenant_id).
 *
 * Bảo mật: accountNumber và accountName được mã hóa AES-256-GCM trước khi lưu DB.
 * Length tăng lên 500/600 để chứa ciphertext (IV + encrypted data, Base64 encoded).
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

    /** Số tài khoản ngân hàng — mã hóa AES-256-GCM trước khi lưu DB */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "account_number", nullable = false, length = 500)
    private String accountNumber;

    /** Tên chủ tài khoản ngân hàng — mã hóa AES-256-GCM trước khi lưu DB */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "account_name", nullable = false, length = 600)
    private String accountName;

    @Column(name = "transfer_prefix", length = 50)
    @Builder.Default
    private String transferPrefix = "HP";
}

