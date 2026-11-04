package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.InvoiceBatchGenerateRequest;
import com.edupay.dto.request.InvoiceItemGenerateRequest;
import com.edupay.dto.request.InvoicePaymentManualRequest;
import com.edupay.dto.response.BillingPreviewResponse;
import com.edupay.dto.response.InvoiceResponse;
import com.edupay.dto.response.PublicInvoiceResponse;
import com.edupay.entity.*;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private PaymentSettingRepository paymentSettingRepository;

    @InjectMocks
    private BillingServiceImpl billingService;

    private UUID tenantId;
    private UUID classId;
    private UUID studentId1;
    private UUID studentId2;
    private ClassEntity classEntity;
    private Student student1;
    private Student student2;
    private PaymentSetting paymentSetting;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        classId = UUID.randomUUID();
        studentId1 = UUID.randomUUID();
        studentId2 = UUID.randomUUID();

        classEntity = ClassEntity.builder()
                .name("Lớp Toán 9A")
                .feeType(FeeType.PER_LESSON)
                .defaultFee(new BigDecimal("150000.00"))
                .build();
        classEntity.setId(classId);
        classEntity.setTenantId(tenantId);

        student1 = Student.builder()
                .classId(classId)
                .fullName("Trần Minh Quân")
                .nickname("Tít")
                .parentPhone("0988111222")
                .customFee(null) // Dùng phí mặc định 150.000
                .status(StudentStatus.STUDYING)
                .build();
        student1.setId(studentId1);
        student1.setTenantId(tenantId);

        student2 = Student.builder()
                .classId(classId)
                .fullName("Lê Ngọc Bích")
                .nickname("Bông")
                .parentPhone("0988333444")
                .customFee(new BigDecimal("120000.00")) // Phí ưu đãi riêng
                .status(StudentStatus.STUDYING)
                .build();
        student2.setId(studentId2);
        student2.setTenantId(tenantId);

        paymentSetting = PaymentSetting.builder()
                .bankBin("970422")
                .bankName("MBBank")
                .accountNumber("0987654321")
                .accountName("NGUYEN THI MAI")
                .transferPrefix("HP")
                .build();
        paymentSetting.setId(UUID.randomUUID());
        paymentSetting.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("getBillingPreview - Tính đúng số buổi có mặt và thành tiền cho từng học sinh")
    void testGetBillingPreview_Success() {
        String month = "2026-10";
        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(paymentSettingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(paymentSetting));

        // Mock 8 buổi có mặt cho học sinh 1, 6 buổi cho học sinh 2
        List<Attendance> attendances = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            attendances.add(Attendance.builder()
                    .studentId(studentId1)
                    .lessonDate(LocalDate.of(2026, 10, i))
                    .status(AttendanceStatus.PRESENT)
                    .build());
        }
        for (int i = 1; i <= 6; i++) {
            attendances.add(Attendance.builder()
                    .studentId(studentId2)
                    .lessonDate(LocalDate.of(2026, 10, i))
                    .status(AttendanceStatus.PRESENT)
                    .build());
        }

        when(attendanceRepository.findAllByTenantIdAndClassIdAndLessonDateBetweenOrderByLessonDateAsc(
                eq(tenantId), eq(classId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(attendances);

        when(studentRepository.findAllByTenantIdAndClassIdOrderByFullNameAsc(tenantId, classId))
                .thenReturn(List.of(student1, student2));
        when(invoiceRepository.findAllByTenantIdAndClassIdAndBillingMonthOrderByCreatedAtDesc(tenantId, classId, month))
                .thenReturn(Collections.emptyList());

        BillingPreviewResponse response = billingService.getBillingPreview(classId, month);

        assertNotNull(response);
        assertEquals(2, response.getTotalStudents());
        assertEquals(14, response.getTotalAttendedCount());
        assertEquals(2, response.getItems().size());

        // Học sinh 1: 8 buổi * 150.000 = 1.200.000
        var item1 = response.getItems().stream().filter(it -> it.getStudentId().equals(studentId1)).findFirst().orElseThrow();
        assertEquals(8, item1.getAttendedCount());
        assertEquals(new BigDecimal("150000.00"), item1.getUnitPrice());
        assertEquals(new BigDecimal("1200000.00"), item1.getTotalAmount());
        assertFalse(item1.isLocked());

        // Học sinh 2: 6 buổi * 120.000 (phí riêng) = 720.000
        var item2 = response.getItems().stream().filter(it -> it.getStudentId().equals(studentId2)).findFirst().orElseThrow();
        assertEquals(6, item2.getAttendedCount());
        assertEquals(new BigDecimal("120000.00"), item2.getUnitPrice());
        assertEquals(new BigDecimal("720000.00"), item2.getTotalAmount());
        assertFalse(item2.isLocked());
    }

    @Test
    @DisplayName("getBillingPreview - Ném lỗi BusinessException khi định dạng tháng sai")
    void testGetBillingPreview_InvalidMonth_ThrowsException() {
        assertThrows(BusinessException.class, () -> billingService.getBillingPreview(classId, "2026/10"));
    }

    @Test
    @DisplayName("generateInvoices - Xuất phiếu thu hàng loạt thành công")
    void testGenerateInvoices_Success() {
        String month = "2026-10";
        InvoiceItemGenerateRequest itemReq1 = InvoiceItemGenerateRequest.builder()
                .studentId(studentId1)
                .attendedCount(8)
                .unitPrice(new BigDecimal("150000.00"))
                .extraFee(new BigDecimal("50000.00")) // Phụ phí 50k
                .discount(BigDecimal.ZERO)
                .note("Sách bài tập Toán")
                .build();

        InvoiceBatchGenerateRequest request = InvoiceBatchGenerateRequest.builder()
                .classId(classId)
                .billingMonth(month)
                .items(List.of(itemReq1))
                .build();

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(paymentSettingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(paymentSetting));
        when(invoiceRepository.findLockedStudentIds(eq(tenantId), any(), eq(month), any()))
                .thenReturn(Collections.emptySet());
        when(studentRepository.findAllByTenantIdAndIdIn(eq(tenantId), any()))
                .thenReturn(List.of(student1));
        when(invoiceRepository.existsByPublicHash(any())).thenReturn(false);
        when(invoiceRepository.existsByTransferCode(any())).thenReturn(false);

        when(invoiceRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Invoice> invs = invocation.getArgument(0);
            invs.forEach(inv -> inv.setId(UUID.randomUUID()));
            return invs;
        });

        List<InvoiceResponse> responses = billingService.generateInvoices(request);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        InvoiceResponse res = responses.get(0);
        // (8 * 150.000) + 50.000 = 1.250.000
        assertEquals(new BigDecimal("1250000.00"), res.getTotalAmount());
        assertNotNull(res.getTransferCode());
        assertNotNull(res.getPublicHash());
        assertNotNull(res.getVietQrUrl());
        assertEquals(InvoiceStatus.PENDING, res.getStatus());
    }

    @Test
    @DisplayName("generateInvoices - Ném lỗi BusinessException khi học sinh đã có phiếu thu")
    void testGenerateInvoices_AlreadyExists_ThrowsException() {
        String month = "2026-10";
        InvoiceItemGenerateRequest itemReq = InvoiceItemGenerateRequest.builder()
                .studentId(studentId1)
                .attendedCount(8)
                .unitPrice(new BigDecimal("150000.00"))
                .build();

        InvoiceBatchGenerateRequest request = InvoiceBatchGenerateRequest.builder()
                .classId(classId)
                .billingMonth(month)
                .items(List.of(itemReq))
                .build();

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(paymentSettingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(paymentSetting));
        when(invoiceRepository.findLockedStudentIds(eq(tenantId), any(), eq(month), any()))
                .thenReturn(Set.of(studentId1));
        when(studentRepository.findAllByTenantIdAndIdIn(eq(tenantId), any()))
                .thenReturn(List.of(student1));

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.generateInvoices(request));
        assertEquals("INVOICE_ALREADY_EXISTS", ex.getErrorCode());
    }

    @Test
    @DisplayName("cancelInvoice - Hủy phiếu thu PENDING thành công")
    void testCancelInvoice_Success() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.builder()
                .classId(classId)
                .studentId(studentId1)
                .status(InvoiceStatus.PENDING)
                .billingMonth("2026-10")
                .build();
        invoice.setId(invoiceId);
        invoice.setTenantId(tenantId);

        when(invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));

        billingService.cancelInvoice(invoiceId);

        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    @DisplayName("cancelInvoice - Chặn không cho hủy phiếu thu đã PAID")
    void testCancelInvoice_Paid_ThrowsException() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.builder()
                .classId(classId)
                .studentId(studentId1)
                .status(InvoiceStatus.PAID)
                .billingMonth("2026-10")
                .build();
        invoice.setId(invoiceId);
        invoice.setTenantId(tenantId);

        when(invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.cancelInvoice(invoiceId));
        assertEquals("INVOICE_ALREADY_PAID", ex.getErrorCode());
    }

    @Test
    @DisplayName("markAsPaid - Gạch nợ thủ công thành công")
    void testMarkAsPaid_Success() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.builder()
                .classId(classId)
                .studentId(studentId1)
                .status(InvoiceStatus.PENDING)
                .totalAmount(new BigDecimal("1200000.00"))
                .transferCode("HPTOAN9ATIT10")
                .publicHash("8a9f20123456789")
                .billingMonth("2026-10")
                .build();
        invoice.setId(invoiceId);
        invoice.setTenantId(tenantId);

        when(invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(studentRepository.findByIdAndTenantId(studentId1, tenantId)).thenReturn(Optional.of(student1));
        when(paymentSettingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(paymentSetting));

        InvoicePaymentManualRequest req = InvoicePaymentManualRequest.builder()
                .paidMethod("CASH")
                .note("Thu tiền trực tiếp tại lớp")
                .build();

        InvoiceResponse response = billingService.markAsPaid(invoiceId, req);

        assertEquals(InvoiceStatus.PAID, response.getStatus());
        assertEquals("CASH", response.getPaidMethod());
        assertNotNull(response.getPaidAt());
    }

    @Test
    @DisplayName("getPublicInvoice - Tra cứu thành công không cần tenant login")
    void testGetPublicInvoice_Success() {
        String hash = "abc123def456";
        Invoice invoice = Invoice.builder()
                .classId(classId)
                .studentId(studentId1)
                .billingMonth("2026-10")
                .attendedCount(8)
                .unitPrice(new BigDecimal("150000.00"))
                .totalAmount(new BigDecimal("1200000.00"))
                .transferCode("HPTOAN9ATIT10")
                .publicHash(hash)
                .status(InvoiceStatus.PENDING)
                .build();
        invoice.setId(UUID.randomUUID());
        invoice.setTenantId(tenantId);

        when(invoiceRepository.findByPublicHash(hash)).thenReturn(Optional.of(invoice));
        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(studentRepository.findById(studentId1)).thenReturn(Optional.of(student1));
        when(paymentSettingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(paymentSetting));
        when(attendanceRepository.findAllByTenantIdAndClassIdAndLessonDateBetweenOrderByLessonDateAsc(
                eq(tenantId), eq(classId), any(), any()))
                .thenReturn(List.of(
                        Attendance.builder().studentId(studentId1).lessonDate(LocalDate.of(2026, 10, 5)).status(AttendanceStatus.PRESENT).build()
                ));

        PublicInvoiceResponse response = billingService.getPublicInvoice(hash);

        assertNotNull(response);
        assertEquals("Lớp Toán 9A", response.getClassName());
        assertEquals("Trần Minh Quân", response.getStudentName());
        assertEquals(hash, response.getPublicHash());
        assertEquals(1, response.getAttendedDates().size());
        assertNotNull(response.getVietQrUrl());
    }
}

