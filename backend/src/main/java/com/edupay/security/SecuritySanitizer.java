package com.edupay.security;

import java.util.regex.Pattern;

/**
 * Tiện ích làm sạch và khử độc dữ liệu đầu vào (Input Sanitization).
 * Chuyên trách bảo vệ chống Stored XSS và chèn mã độc/ký tự điều khiển cho ghi chú điểm danh (Attendance Notes).
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
     * Làm sạch ghi chú buổi học:
     * - Loại bỏ toàn bộ thẻ script, iframe, html tags
     * - Loại bỏ các event handler (onload=, onerror=, ...)
     * - Loại bỏ giao thức javascript:
     * - Cắt bỏ null-byte (\0) và ký tự điều khiển hướng văn bản đảo ngược (\u202E)
     * - Cắt khoảng trắng thừa và giới hạn độ dài an toàn tối đa 255 ký tự
     */
    public static String sanitizeNote(String note) {
        if (note == null) {
            return null;
        }

        String cleaned = note.trim();
        if (cleaned.isEmpty()) {
            return null;
        }

        // Loại bỏ null byte và các ký tự điều khiển ẩn
        cleaned = cleaned.replace("\0", "").replace("\u202E", "");

        // Loại bỏ thẻ script, sự kiện on*, javascript: và toàn bộ HTML tags
        cleaned = SCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = ON_EVENT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = JAVASCRIPT_PROTOCOL_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");

        cleaned = cleaned.trim();

        if (cleaned.length() > 255) {
            cleaned = cleaned.substring(0, 255);
        }

        return cleaned.isEmpty() ? null : cleaned;
    }
}

