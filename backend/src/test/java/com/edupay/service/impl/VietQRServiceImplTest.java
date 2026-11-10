package com.edupay.service.impl;

import com.edupay.entity.Student;
import com.edupay.repository.InvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VietQRServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private VietQRServiceImpl vietQrService;

    @Test
    @DisplayName("Sinh mã chuyển khoản chuẩn HP-[THANG]-[TEN_LOP]-[MA_HS] với học sinh có biệt danh")
    void generateTransferCode_WithNickname_GeneratesValidFormat() {
        Student student = Student.builder()
                .fullName("Trần Minh Quân")
                .nickname("Bé Tít")
                .build();
        student.setId(UUID.randomUUID());

        when(invoiceRepository.existsByTransferCode("HP-10-TOAN9A-BETIT")).thenReturn(false);

        String code = vietQrService.generateTransferCode("HP", "2026-10", "Toán 9A", student);

        assertEquals("HP-10-TOAN9A-BETIT", code);
        verify(invoiceRepository, times(1)).existsByTransferCode("HP-10-TOAN9A-BETIT");
    }

    @Test
    @DisplayName("Sinh mã chuyển khoản khi học sinh không có biệt danh, lấy tên gọi cuối")
    void generateTransferCode_WithoutNickname_UsesLastName() {
        Student student = Student.builder()
                .fullName("Nguyễn Hoàng Nam")
                .nickname(null)
                .build();
        student.setId(UUID.randomUUID());

        when(invoiceRepository.existsByTransferCode("HP-05-LY8B-NAM")).thenReturn(false);

        String code = vietQrService.generateTransferCode("HP", "2026-05", "Lý 8B", student);

        assertEquals("HP-05-LY8B-NAM", code);
    }

    @Test
    @DisplayName("Tự động thêm số thứ tự hậu tố khi mã chuyển khoản bị trùng trong hệ thống")
    void generateTransferCode_DuplicateCode_AppendsCounter() {
        Student student = Student.builder()
                .fullName("Lê Ngọc Bích")
                .nickname("Bông")
                .build();
        student.setId(UUID.randomUUID());

        // Giả lập mã gốc đã tồn tại, mã thứ 2 chưa tồn tại
        when(invoiceRepository.existsByTransferCode("HP-11-HOA10-BONG")).thenReturn(true);
        when(invoiceRepository.existsByTransferCode("HP-11-HOA10-BONG1")).thenReturn(false);

        String code = vietQrService.generateTransferCode("HP", "2026-11", "Hóa 10", student);

        assertEquals("HP-11-HOA10-BONG1", code);
    }

    @Test
    @DisplayName("Sinh URL ảnh VietQR Napas247 compact2 mã hóa URL-Safe chính xác")
    void generateVietQrUrl_ValidParams_ReturnsUrlSafeFormat() {
        String url = vietQrService.generateVietQrUrl(
                "970422",
                "0987654321",
                "NGUYỄN VĂN AN",
                new BigDecimal("650000.00"),
                "HP-10-TOAN9A-001"
        );

        assertNotNull(url);
        assertTrue(url.startsWith("https://img.vietqr.io/image/970422-0987654321-compact2.png"));
        assertTrue(url.contains("amount=650000"));
        assertTrue(url.contains("addInfo=HP-10-TOAN9A-001"));
        // Đã bỏ dấu tiếng Việt và URL encode (thay dấu cách bằng %20)
        assertTrue(url.contains("accountName=NGUYEN%20VAN%20AN"));
    }

    @Test
    @DisplayName("Trả về null khi thiếu bankBin hoặc accountNumber")
    void generateVietQrUrl_MissingBankInfo_ReturnsNull() {
        assertNull(vietQrService.generateVietQrUrl(null, "0987654321", "TEST", BigDecimal.TEN, "HP"));
        assertNull(vietQrService.generateVietQrUrl("970422", "", "TEST", BigDecimal.TEN, "HP"));
    }
}
