package com.edupay.security;

import java.util.regex.Pattern;

/**
 * Tiện ích làm sạch và khử độc dữ liệu đầu vào (Input Sanitization).
 * Chuyên trách bảo vệ chống Stored XSS và chèn mã độc/ký tự điều khiển cho ghi chú điểm danh (Attendance Notes)
 * và các thông tin dạng văn bản thô (Plain Text) của hệ thống.
 *
 * Mở rộng để bao gồm các phương thức xử lý dữ liệu ngân hàng và mã chuyển khoản VietQR.
 */
public final class SecuritySanitizer {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?i)<script.*?>.*?</script.*?>");
    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN = Pattern.compile("(?i)javascript:");
    private static final Pattern ON_EVENT_PATTERN = Pattern.compile("(?i)\\bon\\w+\\s*=");

    // Patterns cho dữ liệu ngân hàng
    private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile("[^0-9]");
    private static final Pattern TRANSFER_CODE_PATTERN = Pattern.compile("[^A-Z0-9]");
    // Chữ cái Latin, chữ cái tiếng Việt có dấu, khoảng trắng, dấu chấm
    private static final Pattern ACCOUNT_NAME_INVALID = Pattern.compile("[^\\p{L}\\p{M}\\s.\\-]");

    private SecuritySanitizer() {
        // Utility class
    }

    /**
     * Làm sạch văn bản thô chung:
     * - Loại bỏ toàn bộ thẻ script, html tags, iframe
     * - Loại bỏ event handlers và giao thức javascript:
     * - Cắt bỏ null-byte (\0) và ký tự điều khiển đảo ngược (\u202E)
     */
    public static String sanitizePlainText(String text) {
        if (text == null) {
            return null;
        }

        String cleaned = text.trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        cleaned = cleaned.replace("\0", "").replace("\u202E", "");
        cleaned = SCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = ON_EVENT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = JAVASCRIPT_PROTOCOL_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");

        return cleaned.trim();
    }

    /**
     * Làm sạch ghi chú buổi học:
     * - Loại bỏ toàn bộ thẻ script, iframe, html tags
     * - Loại bỏ các event handler (onload=, onerror=, ...)
     * - Loại bỏ giao thức javascript:
     * - Cắt bỏ null-byte (\0) và ký tự điều khiển hướng văn bản đảo ngược (\u202E)
     * - Cắt khoảng trắng thừa và giới hạn độ dài an toàn tối đa 255 ký tự
     */
    public static String sanitizeNote(String note) {
        String cleaned = sanitizePlainText(note);
        if (cleaned == null || cleaned.isEmpty()) {
            return null;
        }

        if (cleaned.length() > 255) {
            cleaned = cleaned.substring(0, 255);
        }

        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Làm sạch mã chuyển khoản VietQR:
     * - Chỉ giữ ký tự chữ hoa A-Z và số 0-9 (không dấu, không ký tự đặc biệt)
     * - Chuyển thành chữ hoa
     * - Giới hạn 20 ký tự
     * Ví dụ: "HP202411" → "HP202411", "hp-test 01" → "HPTEST01"
     */
    public static String sanitizeTransferCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        // Chuyển hoa, loại bỏ dấu và ký tự không hợp lệ
        String upper = removeDiacritics(code).toUpperCase();
        String cleaned = TRANSFER_CODE_PATTERN.matcher(upper).replaceAll("");
        return cleaned.isEmpty() ? null : (cleaned.length() > 20 ? cleaned.substring(0, 20) : cleaned);
    }

    /**
     * Làm sạch số tài khoản ngân hàng:
     * - Chỉ giữ ký tự số 0-9
     * - Giới hạn 20 ký tự
     */
    public static String sanitizeAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }
        String cleaned = DIGITS_ONLY_PATTERN.matcher(accountNumber.trim()).replaceAll("");
        return cleaned.isEmpty() ? null : (cleaned.length() > 20 ? cleaned.substring(0, 20) : cleaned);
    }

    /**
     * Làm sạch tên chủ tài khoản ngân hàng:
     * - Chỉ giữ chữ cái (bao gồm Unicode/tiếng Việt), khoảng trắng, dấu chấm, dấu gạch ngang
     * - Giới hạn 255 ký tự
     * Ví dụ: "NGUYEN VAN A" → "NGUYEN VAN A", "Nguyễn Văn <A>" → "Nguyễn Văn A"
     */
    public static String sanitizeAccountName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        // Trước tiên xóa HTML tags và ký tự nguy hiểm
        String cleaned = sanitizePlainText(name);
        if (cleaned == null || cleaned.isEmpty()) {
            return null;
        }
        // Sau đó lọc chỉ còn ký tự hợp lệ cho tên
        cleaned = ACCOUNT_NAME_INVALID.matcher(cleaned).replaceAll("").trim();
        // Chuẩn hóa khoảng trắng liên tiếp
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Loại bỏ dấu tiếng Việt để tạo mã ASCII thuần túy.
     * Dùng nội bộ cho sanitizeTransferCode.
     */
    private static String removeDiacritics(String input) {
        if (input == null) return null;
        // Dùng java.text.Normalizer để tách dấu, sau đó xóa combining marks
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}

