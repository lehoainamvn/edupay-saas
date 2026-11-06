package com.edupay.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AesGcmEncryptor - Mã hóa AES-256-GCM")
class AesGcmEncryptorTest {

    // 32-byte key: "01234567890123456789012345678901"
    private static final String VALID_BASE64_KEY = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private AesGcmEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new AesGcmEncryptor(VALID_BASE64_KEY);
    }

    @Test
    @DisplayName("Mã hóa và giải mã thành công (roundtrip)")
    void encryptAndDecrypt_Success() {
        String original = "0987654321 - NGUYEN VAN A";

        String encrypted = encryptor.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    @DisplayName("Cùng một plaintext sinh ra ciphertext khác nhau (do IV ngẫu nhiên)")
    void encrypt_SamePlaintext_ProducesDifferentCiphertext() {
        String plaintext = "19036578901234";

        String ciphertext1 = encryptor.encrypt(plaintext);
        String ciphertext2 = encryptor.encrypt(plaintext);

        assertNotEquals(ciphertext1, ciphertext2);
        assertEquals(plaintext, encryptor.decrypt(ciphertext1));
        assertEquals(plaintext, encryptor.decrypt(ciphertext2));
    }

    @Test
    @DisplayName("Xử lý an toàn khi plaintext hoặc ciphertext là null")
    void encryptAndDecrypt_NullHandling() {
        assertNull(encryptor.encrypt(null));
        assertNull(encryptor.decrypt(null));
    }

    @Test
    @DisplayName("Khởi tạo thất bại nếu key không đủ 32 bytes (256-bit)")
    void constructor_InvalidKeyLength_ThrowsException() {
        // 16 bytes key: "1234567890123456"
        String invalidKey = Base64.getEncoder().encodeToString("1234567890123456".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new AesGcmEncryptor(invalidKey)
        );
        assertTrue(ex.getMessage().contains("32 bytes"));
    }

    @Test
    @DisplayName("Khởi tạo thất bại trên production nếu dùng dev fallback key")
    void constructor_DevKeyOnProd_ThrowsIllegalStateException() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new AesGcmEncryptor(VALID_BASE64_KEY, "prod")
        );
        assertTrue(ex.getMessage().contains("PRODUCTION"));
    }

    @Test
    @DisplayName("Giải mã dữ liệu bị can thiệp/hư hỏng sẽ ném RuntimeException")
    void decrypt_TamperedData_ThrowsException() {
        String validEncrypted = encryptor.encrypt("Sensitive bank account");
        assertTrue(validEncrypted.startsWith("ENC:"));

        String base64Part = validEncrypted.substring(4);
        byte[] bytes = Base64.getDecoder().decode(base64Part);

        // Can thiệp vào byte cuối cùng của tag
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = "ENC:" + Base64.getEncoder().encodeToString(bytes);

        assertThrows(RuntimeException.class, () -> encryptor.decrypt(tampered));
    }

    @Test
    @DisplayName("Tương thích ngược: dữ liệu plaintext cũ không có prefix ENC: được giữ nguyên")
    void decrypt_LegacyPlaintextWithoutPrefix_ReturnsAsIs() {
        String legacyPlaintext = "0987654321";
        String result = encryptor.decrypt(legacyPlaintext);

        assertEquals(legacyPlaintext, result);
    }

    @Test
    @DisplayName("Tính lũy đẳng (Idempotent): không mã hóa lặp lại nếu chuỗi đã có prefix ENC:")
    void encrypt_AlreadyEncryptedString_DoesNotDoubleEncrypt() {
        String original = "0987654321";
        String firstEncrypted = encryptor.encrypt(original);
        String secondEncrypted = encryptor.encrypt(firstEncrypted);

        assertEquals(firstEncrypted, secondEncrypted);
        assertEquals(original, encryptor.decrypt(secondEncrypted));
    }
}

