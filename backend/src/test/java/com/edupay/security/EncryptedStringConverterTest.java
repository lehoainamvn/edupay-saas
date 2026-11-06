package com.edupay.security;

import com.edupay.entity.converter.EncryptedStringConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EncryptedStringConverter - JPA AttributeConverter cho AES-256")
class EncryptedStringConverterTest {

    private static final String VALID_KEY = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private EncryptedStringConverter converter;

    @BeforeEach
    void setUp() {
        AesGcmEncryptor encryptor = new AesGcmEncryptor(VALID_KEY);
        converter = new EncryptedStringConverter();
        converter.setEncryptor(encryptor);
    }

    @Test
    @DisplayName("Chuyển đổi dữ liệu sang database column (mã hóa)")
    void convertToDatabaseColumn_Success() {
        String plain = "123456789";
        String encrypted = converter.convertToDatabaseColumn(plain);

        assertNotNull(encrypted);
        assertNotEquals(plain, encrypted);
    }

    @Test
    @DisplayName("Chuyển đổi dữ liệu từ database column sang entity attribute (giải mã)")
    void convertToEntityAttribute_Success() {
        String plain = "Nguyen Van A";
        String encrypted = converter.convertToDatabaseColumn(plain);

        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    @DisplayName("Xử lý an toàn khi giá trị là null")
    void convertNull_ReturnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}

