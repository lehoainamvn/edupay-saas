package com.edupay.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Tiện ích mã hóa/giải mã AES-256-GCM cho dữ liệu ngân hàng nhạy cảm.
 *
 * Thuật toán: AES/GCM/NoPadding
 * - Key: 256-bit (32 bytes) từ biến môi trường APP_ENCRYPTION_KEY (Base64 encoded)
 * - IV: 96-bit (12 bytes) ngẫu nhiên mỗi lần mã hóa (đảm bảo nonce uniqueness)
 * - Tag length: 128-bit (GCM authentication tag)
 * - Output format: Base64(IV + ciphertext)
 *
 * Production: APP_ENCRYPTION_KEY phải là biến môi trường, không được hardcode.
 * Dev: Dùng fallback key trong application-dev.yml chỉ cho môi trường phát triển.
 */
@Slf4j
@Component
public class AesGcmEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES  = 12;   // 96-bit IV for GCM
    private static final int TAG_LENGTH_BITS  = 128;  // 128-bit GCM authentication tag
    private static final int KEY_LENGTH_BYTES = 32;   // 256-bit key

    /** Tiền tố đánh dấu dữ liệu đã mã hóa (hỗ trợ tương thích ngược với dữ liệu legacy) */
    public static final String ENC_PREFIX = "ENC:";
    private static final String DEV_FALLBACK_KEY = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmEncryptor(String base64Key) {
        this(base64Key, "dev");
    }

    public AesGcmEncryptor(@Value("${app.encryption.key}") String base64Key,
                           @Value("${spring.profiles.active:dev}") String activeProfile) {
        if ("prod".equalsIgnoreCase(activeProfile) && DEV_FALLBACK_KEY.equals(base64Key)) {
            throw new IllegalStateException(
                "NGUY HIỂM: Phát hiện đang sử dụng khóa mã hóa mặc định DEV trên môi trường PRODUCTION! " +
                "Vui lòng cấu hình biến môi trường APP_ENCRYPTION_KEY với khóa AES-256 ngẫu nhiên an toàn."
            );
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                "app.encryption.key phải là Base64 của 32 bytes (256-bit). " +
                "Hiện tại: " + keyBytes.length + " bytes."
            );
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("AesGcmEncryptor khởi tạo thành công với key 256-bit");
    }

    /**
     * Mã hóa plaintext thành chuỗi ENC:Base64(IV + ciphertext).
     * Mỗi lần gọi tạo IV ngẫu nhiên mới → ciphertext luôn khác nhau.
     * Nếu chuỗi đã có tiền tố ENC: thì không mã hóa lại (idempotent).
     *
     * @param plaintext Văn bản cần mã hóa (null-safe: trả về null nếu input null)
     * @return Chuỗi có tiền tố ENC: chứa IV + ciphertext, hoặc null nếu input null
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        if (plaintext.startsWith(ENC_PREFIX)) return plaintext;

        try {
            // Sinh IV ngẫu nhiên 12 bytes
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            // Khởi tạo cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Mã hóa
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Ghép IV + ciphertext, encode Base64
            byte[] combined = new byte[IV_LENGTH_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH_BYTES);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH_BYTES, ciphertext.length);

            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("Lỗi mã hóa AES-GCM: {}", e.getMessage());
            throw new RuntimeException("Không thể mã hóa dữ liệu", e);
        }
    }

    /**
     * Giải mã chuỗi ENC:Base64(IV + ciphertext) thành plaintext.
     * Tương thích ngược: Nếu dữ liệu trong DB không có tiền tố ENC: (legacy plaintext),
     * phương thức sẽ trả về nguyên bản chuỗi đó thay vì ném ngoại lệ.
     *
     * @param encryptedData Chuỗi cần giải mã (null-safe)
     * @return Văn bản gốc, hoặc null nếu input null
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null) return null;

        // Tương thích ngược: dữ liệu plaintext cũ không có prefix ENC:
        if (!encryptedData.startsWith(ENC_PREFIX)) {
            log.debug("Dữ liệu chưa qua mã hóa (legacy plaintext) — trả về nguyên bản");
            return encryptedData;
        }

        try {
            String base64Cipher = encryptedData.substring(ENC_PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64Cipher);

            // Tách IV và ciphertext
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combined, IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            // Giải mã
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Lỗi giải mã AES-GCM: {}", e.getMessage());
            throw new RuntimeException("Không thể giải mã dữ liệu", e);
        }
    }
}

