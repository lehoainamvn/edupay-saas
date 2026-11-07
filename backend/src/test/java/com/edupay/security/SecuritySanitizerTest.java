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

    // ─── Tests cho dữ liệu ngân hàng & VietQR ─────────────────────────────────

    @Test
    @DisplayName("Làm sạch mã chuyển khoản: Chuyển hoa, bỏ dấu tiếng Việt và ký tự đặc biệt")
    void sanitizeTransferCode_NormalizesVietnameseAndSpecialChars() {
        String input = "hp tiền học 11/2024!";
        String result = SecuritySanitizer.sanitizeTransferCode(input);

        // "hp tiền học 11/2024!" -> diacritics removed -> "HP TIEN HOC 112024" -> digits and A-Z only -> "HPTIENHOC112024"
        assertEquals("HPTIENHOC112024", result);
    }

    @Test
    @DisplayName("Làm sạch mã chuyển khoản: Cắt tối đa 20 ký tự")
    void sanitizeTransferCode_TruncatesTo20Chars() {
        String input = "A".repeat(30);
        String result = SecuritySanitizer.sanitizeTransferCode(input);

        assertEquals(20, result.length());
    }

    @Test
    @DisplayName("Làm sạch số tài khoản: Chỉ giữ lại chữ số 0-9")
    void sanitizeAccountNumber_OnlyDigitsKept() {
        String input = " 1903-6578-9012 34 ";
        String result = SecuritySanitizer.sanitizeAccountNumber(input);

        assertEquals("19036578901234", result);
    }

    @Test
    @DisplayName("Làm sạch tên tài khoản: Giữ tiếng Việt, bỏ thẻ XSS và ký tự lạ")
    void sanitizeAccountName_StripsXssAndSpecialChars() {
        String input = "<script>alert(1)</script>NGUYỄN VĂN A";
        String result = SecuritySanitizer.sanitizeAccountName(input);

        assertEquals("NGUYỄN VĂN A", result);
    }

    @Test
    @DisplayName("Xử lý an toàn khi đầu vào chỉ gồm ký tự đặc biệt hoặc null")
    void sanitizeBanking_SpecialCharsOnlyOrNull_ReturnsNull() {
        assertNull(SecuritySanitizer.sanitizeTransferCode(null));
        assertNull(SecuritySanitizer.sanitizeTransferCode("!@#$%^"));
        assertNull(SecuritySanitizer.sanitizeAccountNumber(null));
        assertNull(SecuritySanitizer.sanitizeAccountNumber("abcd---"));
        assertNull(SecuritySanitizer.sanitizeAccountName(null));
        assertNull(SecuritySanitizer.sanitizeAccountName("<script></script>"));
    }
}

