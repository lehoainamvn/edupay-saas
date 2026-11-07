package com.edupay.entity.converter;

import com.edupay.security.AesGcmEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter mã hóa/giải mã dữ liệu String nhạy cảm bằng AES-256-GCM.
 *
 * Cách dùng trên Entity field:
 * <pre>
 *   {@literal @}Convert(converter = EncryptedStringConverter.class)
 *   {@literal @}Column(name = "account_number", length = 500)
 *   private String accountNumber;
 * </pre>
 *
 * - convertToDatabaseColumn: plaintext → encrypted Base64 (lưu DB)
 * - convertToEntityAttribute: encrypted Base64 → plaintext (đọc từ DB)
 *
 * autoApply = false: chỉ áp dụng khi field được đánh @Convert — không tự động mã hóa mọi String.
 */
@Converter(autoApply = false)
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    // Static field để inject vào converter (JPA không hỗ trợ DI trực tiếp)
    private static AesGcmEncryptor encryptor;

    @Autowired
    public void setEncryptor(AesGcmEncryptor encryptor) {
        EncryptedStringConverter.encryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        return encryptor.encrypt(plaintext);
    }

    @Override
    public String convertToEntityAttribute(String encryptedBase64) {
        if (encryptedBase64 == null) return null;
        return encryptor.decrypt(encryptedBase64);
    }
}

