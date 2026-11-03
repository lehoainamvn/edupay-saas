package com.edupay.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecuritySanitizerTest {

    @Test
    @DisplayName("Làm sạch ghi chú: Loại bỏ thẻ <script> độc hại")
    void sanitizeNote_StripsScriptTags() {
        String input = "Bị sốt nhẹ <script>alert('hack')</script>";
        String result = SecuritySanitizer.sanitizeNote(input);

        assertEquals("Bị sốt nhẹ", result);
    }

    @Test
    @DisplayName("Làm sạch ghi chú: Loại bỏ sự kiện onerror trong thẻ img")
    void sanitizeNote_StripsImgWithOnError() {
        String input = "Đi muộn <img src=x onerror=alert(1)>";
        String result = SecuritySanitizer.sanitizeNote(input);

        assertEquals("Đi muộn", result);
    }

    @Test
    @DisplayName("Làm sạch ghi chú: Loại bỏ giao thức javascript:")
    void sanitizeNote_StripsJavascriptProtocol() {
        String input = "Ghi chú javascript:alert(document.cookie)";
        String result = SecuritySanitizer.sanitizeNote(input);

        assertFalse(result.contains("javascript:"));
    }

    @Test
    @DisplayName("Làm sạch ghi chú: Giữ nguyên văn bản tiếng Việt hợp lệ")
    void sanitizeNote_PreservesValidVietnameseText() {
        String input = "Học sinh xin phép nghỉ vì gia đình có việc bận.";
        String result = SecuritySanitizer.sanitizeNote(input);

        assertEquals(input, result);
    }

    @Test
    @DisplayName("Làm sạch ghi chú: Cắt bớt nếu vượt quá 255 ký tự")
    void sanitizeNote_TruncatesOver255Chars() {
        String longText = "A".repeat(300);
        String result = SecuritySanitizer.sanitizeNote(longText);

        assertNotNull(result);
        assertEquals(255, result.length());
    }

    @Test
    @DisplayName("Làm sạch ghi chú: Trả về null nếu đầu vào null hoặc chỉ có khoảng trắng")
    void sanitizeNote_ReturnsNullForBlankInput() {
        assertNull(SecuritySanitizer.sanitizeNote(null));
        assertNull(SecuritySanitizer.sanitizeNote("   "));
        assertNull(SecuritySanitizer.sanitizeNote("<script></script>"));
    }
}

