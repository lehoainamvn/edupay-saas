package com.edupay.security;

import java.util.regex.Pattern;

/**
 * Tiện ích làm sạch và khử độc dữ liệu đầu vào (Input Sanitization).
 * Chuyên trách bảo vệ chống Stored XSS và chèn mã độc/ký tự điều khiển cho ghi chú điểm danh (Attendance Notes)
 * và các thông tin dạng văn bản thô (Plain Text) của hệ thống.
 */
public final class SecuritySanitizer {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?i)<script.*?>.*?</script.*?>");
    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN = Pattern.compile("(?i)javascript:");
    private static final Pattern ON_EVENT_PATTERN = Pattern.compile("(?i)\\bon\\w+\\s*=");

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
}
