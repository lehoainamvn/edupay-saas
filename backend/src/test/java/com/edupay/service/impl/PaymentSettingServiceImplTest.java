package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.PaymentSettingRequest;
import com.edupay.dto.response.BankResponse;
import com.edupay.dto.response.PaymentSettingResponse;
import com.edupay.entity.PaymentSetting;
import com.edupay.repository.PaymentSettingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSettingServiceImplTest {

    @Mock
    private PaymentSettingRepository paymentSettingRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentSettingServiceImpl paymentSettingService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("getPaymentSetting - Đã cấu hình: Trả về đầy đủ thông tin, cú pháp mẫu và configured = true")
    void testGetPaymentSetting_Configured() {
        PaymentSetting setting = PaymentSetting.builder()
                .bankBin("970422")
                .bankName("MBBank")
                .accountNumber("0988111222")
                .accountName("LE HOAI NAM")
                .transferPrefix("HP")
                .build();
        setting.setId(UUID.randomUUID());
        setting.setTenantId(tenantId);

        when(paymentSettingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(setting));

        PaymentSettingResponse response = paymentSettingService.getPaymentSetting();

        assertNotNull(response);
        assertTrue(response.isConfigured());
        assertEquals("970422", response.getBankBin());
        assertEquals("MBBank", response.getBankName());
        assertEquals("0988111222", response.getAccountNumber());
        assertEquals("LE HOAI NAM", response.getAccountName());
        assertEquals("HP", response.getTransferPrefix());
        assertEquals("HP9A01210", response.getSampleTransferContent());
        assertNotNull(response.getVietQrSampleUrl());
        assertTrue(response.getVietQrSampleUrl().contains("970422"));
        assertTrue(response.getVietQrSampleUrl().contains("HP9A01210"));
    }

    @Test
    @DisplayName("getPaymentSetting - Chưa cấu hình: Trả về configured = false và prefix & cú pháp mặc định HP")
    void testGetPaymentSetting_NotConfigured() {
        when(paymentSettingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        PaymentSettingResponse response = paymentSettingService.getPaymentSetting();

        assertNotNull(response);
        assertFalse(response.isConfigured());
        assertEquals("HP", response.getTransferPrefix());
        assertEquals("HP9A01210", response.getSampleTransferContent());
        assertNull(response.getAccountNumber());
    }

    @Test
    @DisplayName("savePaymentSetting - Tạo mới thành công và chuẩn hóa chữ in hoa không dấu cùng cú pháp mẫu")
    void testSavePaymentSetting_CreateNew_Success() {
        when(paymentSettingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(paymentSettingRepository.save(any(PaymentSetting.class))).thenAnswer(invocation -> {
            PaymentSetting p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PaymentSettingRequest request = PaymentSettingRequest.builder()
                .bankBin("970436")
                .bankName("Vietcombank")
                .accountNumber(" 0011 0022 3344 ")
                .accountName("Lê Hoài Nam")
                .transferPrefix("hocphi-toan")
                .build();

        PaymentSettingResponse response = paymentSettingService.savePaymentSetting(request);

        assertNotNull(response);
        assertTrue(response.isConfigured());
        assertEquals("970436", response.getBankBin());
        assertEquals("Vietcombank", response.getBankName());
        assertEquals("001100223344", response.getAccountNumber()); // Bỏ khoảng trắng
        assertEquals("LE HOAI NAM", response.getAccountName()); // In hoa không dấu
        assertEquals("HOCPHITOAN", response.getTransferPrefix()); // In hoa, bỏ ký tự đặc biệt
        assertEquals("HOCPHITOAN9A01210", response.getSampleTransferContent());
    }

    @Test
    @DisplayName("savePaymentSetting - Cập nhật đè lên cấu hình cũ (Upsert)")
    void testSavePaymentSetting_UpdateExisting_Success() {
        PaymentSetting existing = PaymentSetting.builder()
                .bankBin("970422")
                .bankName("MBBank")
                .accountNumber("0988111222")
                .accountName("LE HOAI NAM")
                .transferPrefix("HP")
                .build();
        existing.setId(UUID.randomUUID());
        existing.setTenantId(tenantId);

        when(paymentSettingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existing));
        when(paymentSettingRepository.save(any(PaymentSetting.class))).thenAnswer(i -> i.getArgument(0));

        PaymentSettingRequest updateReq = PaymentSettingRequest.builder()
                .bankBin("970407")
                .bankName("Techcombank")
                .accountNumber("190333444555")
                .accountName("LE HOAI NAM")
                .transferPrefix("TCB")
                .build();

        PaymentSettingResponse response = paymentSettingService.savePaymentSetting(updateReq);

        assertNotNull(response);
        assertEquals("970407", response.getBankBin());
        assertEquals("Techcombank", response.getBankName());
        assertEquals("190333444555", response.getAccountNumber());
        assertEquals("TCB", response.getTransferPrefix());
        assertEquals("TCB9A01210", response.getSampleTransferContent());
        verify(paymentSettingRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("getSupportedBanks - Trả về danh sách 54+ ngân hàng (kể cả khi offline/fallback)")
    void testGetSupportedBanks_ReturnsBanksList() {
        List<BankResponse> banks = paymentSettingService.getSupportedBanks();

        assertNotNull(banks);
        assertTrue(banks.size() >= 54, "Danh sách ngân hàng phải có tối thiểu 54 ngân hàng");

        // Kiểm tra một số ngân hàng lớn cốt lõi
        assertTrue(banks.stream().anyMatch(b -> "970422".equals(b.getBin()) && "MBBank".equalsIgnoreCase(b.getShortName())));
        assertTrue(banks.stream().anyMatch(b -> "970436".equals(b.getBin()) && "Vietcombank".equalsIgnoreCase(b.getShortName())));
        assertTrue(banks.stream().anyMatch(b -> "970407".equals(b.getBin()) && "Techcombank".equalsIgnoreCase(b.getShortName())));
        assertTrue(banks.stream().anyMatch(b -> "970415".equals(b.getBin())));
    }
}
