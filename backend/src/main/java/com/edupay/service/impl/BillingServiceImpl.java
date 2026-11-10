package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.InvoiceBatchGenerateRequest;
import com.edupay.dto.request.InvoiceItemGenerateRequest;
import com.edupay.dto.request.InvoicePaymentManualRequest;
import com.edupay.dto.response.*;
import com.edupay.entity.*;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.*;
import com.edupay.security.SecuritySanitizer;
import com.edupay.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ Tính học phí, Quản lý Phiếu thu VietQR và Cổng tra cứu phụ huynh.
 * BẮT BUỘC: 100% nghiệp vụ thực thi tại đây, cô lập dữ liệu theo TenantContext (P0).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingServiceImpl implements BillingService {

    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final List<InvoiceStatus> ACTIVE_STATUSES = List.of(InvoiceStatus.PENDING, InvoiceStatus.PAID);

    private final InvoiceRepository invoiceRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final PaymentSettingRepository paymentSettingRepository;
    private final com.edupay.repository.PaymentTransactionRepository paymentTransactionRepository;
    private final com.edupay.service.VietQRService vietQrService;

    @Override
    public BillingPreviewResponse getBillingPreview(UUID classId, String billingMonth) {
        UUID tenantId = TenantContext.getTenantId();
        validateMonthFormat(billingMonth);

        ClassEntity classEntity = findClassOrThrow(classId, tenantId);
        Optional<PaymentSetting> paymentSettingOpt = paymentSettingRepository.findByTenantId(tenantId);

        YearMonth ym = YearMonth.parse(billingMonth);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // 1. Lấy tất cả lượt điểm danh trạng thái PRESENT trong tháng của lớp
        List<Attendance> monthlyAttendances = attendanceRepository
                .findAllByTenantIdAndClassIdAndLessonDateBetweenOrderByLessonDateAsc(
                        tenantId, classId, startDate, endDate);

        Map<UUID, Long> attendedCountMap = monthlyAttendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                .collect(Collectors.groupingBy(Attendance::getStudentId, Collectors.counting()));

        // 2. Lấy danh sách học sinh của lớp
        List<Student> students = studentRepository.findAllByTenantIdAndClassIdOrderByFullNameAsc(tenantId, classId);

        // 3. Lấy các phiếu thu hiện có của lớp trong tháng đó
        List<Invoice> existingInvoices = invoiceRepository
                .findAllByTenantIdAndClassIdAndBillingMonthOrderByCreatedAtDesc(tenantId, classId, billingMonth);

        Map<UUID, Invoice> invoiceMap = existingInvoices.stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED)
                .collect(Collectors.toMap(Invoice::getStudentId, Function.identity(), (a, b) -> a));

        // 4. Xây dựng danh sách BillingPreviewItemResponse
        List<BillingPreviewItemResponse> items = new ArrayList<>();
        int totalAttendedCount = 0;
        BigDecimal totalCalculatedAmount = BigDecimal.ZERO;

        for (Student s : students) {
            int attended = attendedCountMap.getOrDefault(s.getId(), 0L).intValue();
            totalAttendedCount += attended;

            Invoice existingInvoice = invoiceMap.get(s.getId());
            boolean isLocked = (existingInvoice != null);

            BigDecimal unitPrice;
            BigDecimal baseAmount;
            BigDecimal extraFee;
            BigDecimal discount;
            BigDecimal totalAmount;
            BigDecimal defaultFee = (classEntity.getDefaultFee() != null) ? classEntity.getDefaultFee() : BigDecimal.ZERO;

            if (isLocked) {
                unitPrice = existingInvoice.getUnitPrice() != null ? existingInvoice.getUnitPrice() : defaultFee;
                attended = existingInvoice.getAttendedCount() != null ? existingInvoice.getAttendedCount() : 0;
                extraFee = existingInvoice.getExtraFee() != null ? existingInvoice.getExtraFee() : BigDecimal.ZERO;
                discount = existingInvoice.getDiscount() != null ? existingInvoice.getDiscount() : BigDecimal.ZERO;
                if (classEntity.getFeeType() == FeeType.MONTHLY_PACKAGE) {
                    baseAmount = unitPrice;
                } else {
                    baseAmount = unitPrice.multiply(BigDecimal.valueOf(attended));
                }
                totalAmount = existingInvoice.getTotalAmount() != null ? existingInvoice.getTotalAmount() : BigDecimal.ZERO;
            } else {
                unitPrice = (s.getCustomFee() != null && s.getCustomFee().compareTo(BigDecimal.ZERO) > 0)
                        ? s.getCustomFee()
                        : defaultFee;

                if (classEntity.getFeeType() == FeeType.MONTHLY_PACKAGE) {
                    baseAmount = unitPrice;
                } else {
                    baseAmount = unitPrice.multiply(BigDecimal.valueOf(attended));
                }

                extraFee = BigDecimal.ZERO;
                discount = BigDecimal.ZERO;
                totalAmount = baseAmount.add(extraFee).subtract(discount).max(BigDecimal.ZERO);
            }

            totalCalculatedAmount = totalCalculatedAmount.add(totalAmount);

            BillingPreviewItemResponse item = BillingPreviewItemResponse.builder()
                    .studentId(s.getId())
                    .fullName(s.getFullName())
                    .nickname(s.getNickname())
                    .parentPhone(s.getParentPhone())
                    .attendedCount(attended)
                    .unitPrice(unitPrice)
                    .baseAmount(baseAmount)
                    .extraFee(extraFee)
                    .discount(discount)
                    .totalAmount(totalAmount)
                    .isLocked(isLocked)
                    .existingInvoiceId(existingInvoice != null ? existingInvoice.getId() : null)
                    .existingInvoiceStatus(existingInvoice != null ? existingInvoice.getStatus() : null)
                    .existingTransferCode(existingInvoice != null ? existingInvoice.getTransferCode() : null)
                    .existingPublicHash(existingInvoice != null ? existingInvoice.getPublicHash() : null)
                    .build();

            items.add(item);
        }

        return BillingPreviewResponse.builder()
                .classId(classEntity.getId())
                .className(classEntity.getName())
                .feeType(classEntity.getFeeType())
                .billingMonth(billingMonth)
                .totalStudents(students.size())
                .totalAttendedCount(totalAttendedCount)
                .totalCalculatedAmount(totalCalculatedAmount)
                .hasBankConfigured(paymentSettingOpt.isPresent())
                .bankName(paymentSettingOpt.map(PaymentSetting::getBankName).orElse(null))
                .accountNumber(paymentSettingOpt.map(PaymentSetting::getAccountNumber).orElse(null))
                .accountName(paymentSettingOpt.map(PaymentSetting::getAccountName).orElse(null))
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public List<InvoiceResponse> generateInvoices(InvoiceBatchGenerateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        validateMonthFormat(request.getBillingMonth());

        ClassEntity classEntity = findClassOrThrow(request.getClassId(), tenantId);
        PaymentSetting paymentSetting = paymentSettingRepository.findByTenantId(tenantId)
                .orElse(createDefaultPaymentSetting(tenantId));

        // Kiểm tra danh sách học sinh bị trùng phiếu thu trong kỳ
        List<UUID> requestedStudentIds = request.getItems().stream()
                .map(InvoiceItemGenerateRequest::getStudentId)
                .toList();

        Set<UUID> lockedStudentIds = invoiceRepository.findLockedStudentIds(
                tenantId, requestedStudentIds, request.getBillingMonth(), ACTIVE_STATUSES);

        Map<UUID, Student> studentMap = studentRepository.findAllByTenantIdAndIdIn(tenantId, requestedStudentIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));

        if (!lockedStudentIds.isEmpty()) {
            List<String> lockedNames = lockedStudentIds.stream()
                    .map(id -> studentMap.containsKey(id) ? studentMap.get(id).getFullName() : id.toString())
                    .toList();
            throw new BusinessException(
                    "INVOICE_ALREADY_EXISTS",
                    String.format("Không thể xuất phiếu thu vì các học sinh sau đã có phiếu thu trong kỳ %s: %s",
                            request.getBillingMonth(), String.join(", ", lockedNames))
            );
        }

        List<Invoice> invoicesToSave = new ArrayList<>();
        // Chuẩn bị danh sách phiếu thu xuất hàng loạt

        for (InvoiceItemGenerateRequest itemReq : request.getItems()) {
            Student student = studentMap.get(itemReq.getStudentId());
            if (student == null) {
                throw new ResourceNotFoundException("STUDENT_NOT_FOUND", "Không tìm thấy học sinh với ID: " + itemReq.getStudentId());
            }

            BigDecimal extra = itemReq.getExtraFee() != null ? itemReq.getExtraFee() : BigDecimal.ZERO;
            BigDecimal disc = itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO;
            BigDecimal unitPrice = itemReq.getUnitPrice();
            int attended = itemReq.getAttendedCount();

            BigDecimal total;
            if (classEntity.getFeeType() == FeeType.MONTHLY_PACKAGE) {
                total = unitPrice.add(extra).subtract(disc);
            } else {
                total = unitPrice.multiply(BigDecimal.valueOf(attended)).add(extra).subtract(disc);
            }
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                total = BigDecimal.ZERO;
            }

            String publicHash = generateUniquePublicHash();
            String transferCode = vietQrService.generateTransferCode(
                    paymentSetting.getTransferPrefix(), request.getBillingMonth(), classEntity.getName(), student);

            Invoice invoice = Invoice.builder()
                    .classId(request.getClassId())
                    .studentId(student.getId())
                    .billingMonth(request.getBillingMonth())
                    .attendedCount(attended)
                    .unitPrice(unitPrice)
                    .extraFee(extra)
                    .discount(disc)
                    .totalAmount(total)
                    .transferCode(transferCode)
                    .publicHash(publicHash)
                    .status(InvoiceStatus.PENDING)
                    .note(SecuritySanitizer.sanitizeNote(itemReq.getNote()))
                    .build();
            invoice.setTenantId(tenantId);

            invoicesToSave.add(invoice);
        }

        List<Invoice> savedInvoices = invoiceRepository.saveAll(invoicesToSave);
        log.info("Xuất thành công {} phiếu thu cho lớp {} kỳ {}", savedInvoices.size(), classEntity.getName(), request.getBillingMonth());

        return savedInvoices.stream()
                .map(inv -> toInvoiceResponse(inv, classEntity, studentMap.get(inv.getStudentId()), paymentSetting))
                .toList();
    }

    @Override
    public List<InvoiceResponse> getInvoices(UUID classId, String billingMonth, InvoiceStatus status) {
        UUID tenantId = TenantContext.getTenantId();
        PaymentSetting paymentSetting = paymentSettingRepository.findByTenantId(tenantId)
                .orElse(createDefaultPaymentSetting(tenantId));

        List<Invoice> invoices;
        if (classId != null && billingMonth != null) {
            validateMonthFormat(billingMonth);
            if (status != null) {
                invoices = invoiceRepository.findAllByTenantIdAndClassIdAndBillingMonthAndStatusOrderByCreatedAtDesc(
                        tenantId, classId, billingMonth, status);
            } else {
                invoices = invoiceRepository.findAllByTenantIdAndClassIdAndBillingMonthOrderByCreatedAtDesc(
                        tenantId, classId, billingMonth);
            }
        } else if (billingMonth != null) {
            validateMonthFormat(billingMonth);
            invoices = invoiceRepository.findAllByTenantIdAndBillingMonthOrderByCreatedAtDesc(tenantId, billingMonth);
        } else {
            invoices = invoiceRepository.findAllByTenantIdAndBillingMonthOrderByCreatedAtDesc(
                    tenantId, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }

        if (invoices.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> classIds = invoices.stream().map(Invoice::getClassId).collect(Collectors.toSet());
        Set<UUID> studentIds = invoices.stream().map(Invoice::getStudentId).collect(Collectors.toSet());

        Map<UUID, ClassEntity> classMap = classRepository.findAllByTenantIdAndIdIn(tenantId, classIds).stream()
                .collect(Collectors.toMap(ClassEntity::getId, Function.identity()));
        Map<UUID, Student> studentMap = studentRepository.findAllByTenantIdAndIdIn(tenantId, studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));

        return invoices.stream()
                .map(inv -> toInvoiceResponse(inv, classMap.get(inv.getClassId()), studentMap.get(inv.getStudentId()), paymentSetting))
                .toList();
    }

    @Override
    public InvoiceResponse getInvoiceById(UUID invoiceId) {
        UUID tenantId = TenantContext.getTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("INVOICE_NOT_FOUND", "Không tìm thấy phiếu thu với ID: " + invoiceId));

        ClassEntity classEntity = findClassOrThrow(invoice.getClassId(), tenantId);
        Student student = studentRepository.findByIdAndTenantId(invoice.getStudentId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("STUDENT_NOT_FOUND", "Không tìm thấy học sinh của phiếu thu"));
        PaymentSetting paymentSetting = paymentSettingRepository.findByTenantId(tenantId)
                .orElse(createDefaultPaymentSetting(tenantId));

        return toInvoiceResponse(invoice, classEntity, student, paymentSetting);
    }

    @Override
    @Transactional
    public void cancelInvoice(UUID invoiceId) {
        UUID tenantId = TenantContext.getTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("INVOICE_NOT_FOUND", "Không tìm thấy phiếu thu với ID: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("INVOICE_ALREADY_PAID", "Không thể hủy phiếu thu đã thanh toán thành công.");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
        log.info("Đã hủy phiếu thu {} của học sinh {}, mở khóa điểm danh kỳ {}",
                invoice.getId(), invoice.getStudentId(), invoice.getBillingMonth());
    }

    @Override
    @Transactional
    public InvoiceResponse markAsPaid(UUID invoiceId, InvoicePaymentManualRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("INVOICE_NOT_FOUND", "Không tìm thấy phiếu thu với ID: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("INVOICE_ALREADY_PAID", "Phiếu thu này đã được thanh toán trước đó.");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        invoice.setConfirmedAt(Instant.now());
        invoice.setPaidMethod(request.getPaidMethod() != null ? request.getPaidMethod().toUpperCase() : "CASH");
        if (request.getNote() != null && !request.getNote().isBlank()) {
            String sanitized = SecuritySanitizer.sanitizeNote(request.getNote());
            invoice.setNote((invoice.getNote() != null ? invoice.getNote() + " | " : "") + sanitized);
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Đã gạch nợ thành công cho phiếu thu {} qua hình thức {}", saved.getId(), saved.getPaidMethod());

        // Ghi log giao dịch thanh toán vào payment_transactions
        try {
            com.edupay.entity.PaymentTransaction tx = com.edupay.entity.PaymentTransaction.builder()
                    .invoiceId(saved.getId())
                    .referenceNumber("MANUAL-" + saved.getId().toString().substring(0, 8) + "-" + System.currentTimeMillis())
                    .amount(saved.getTotalAmount())
                    .bankBrandName("TIEN_MAT_HOAC_MANUAL")
                    .paymentSource("MANUAL_" + saved.getPaidMethod())
                    .transactionContent("Gạch nợ thủ công: " + (request.getNote() != null ? request.getNote() : "Thành công"))
                    .transactionTime(Instant.now())
                    .build();
            tx.setTenantId(tenantId);
            paymentTransactionRepository.save(tx);
        } catch (Exception ex) {
            log.warn("Không thể lưu payment_transaction cho invoice [{}]: {}", saved.getId(), ex.getMessage());
        }

        ClassEntity classEntity = findClassOrThrow(saved.getClassId(), tenantId);
        Student student = studentRepository.findByIdAndTenantId(saved.getStudentId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("STUDENT_NOT_FOUND", "Không tìm thấy học sinh"));
        PaymentSetting paymentSetting = paymentSettingRepository.findByTenantId(tenantId)
                .orElse(createDefaultPaymentSetting(tenantId));

        return toInvoiceResponse(saved, classEntity, student, paymentSetting);
    }

    @Override
    public PublicInvoiceResponse getPublicInvoice(String publicHash) {
        if (publicHash == null || publicHash.isBlank()) {
            throw new ResourceNotFoundException("INVOICE_NOT_FOUND", "Mã tra cứu phiếu thu không hợp lệ.");
        }

        Invoice invoice = invoiceRepository.findByPublicHash(publicHash)
                .orElseThrow(() -> new ResourceNotFoundException("INVOICE_NOT_FOUND", "Không tìm thấy thông tin phiếu thu tương ứng."));

        ClassEntity classEntity = classRepository.findById(invoice.getClassId())
                .orElse(ClassEntity.builder().name("Lớp học").build());
        Student student = studentRepository.findById(invoice.getStudentId())
                .orElse(Student.builder().fullName("Học sinh").build());

        PaymentSetting paymentSetting = paymentSettingRepository.findByTenantId(invoice.getTenantId())
                .orElse(createDefaultPaymentSetting(invoice.getTenantId()));

        YearMonth ym = YearMonth.parse(invoice.getBillingMonth());
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<LocalDate> attendedDates = attendanceRepository
                .findAllByTenantIdAndClassIdAndLessonDateBetweenOrderByLessonDateAsc(
                        invoice.getTenantId(), invoice.getClassId(), startDate, endDate)
                .stream()
                .filter(a -> a.getStudentId().equals(invoice.getStudentId()) && a.getStatus() == AttendanceStatus.PRESENT)
                .map(Attendance::getLessonDate)
                .toList();

        String vietQrUrl = vietQrService.generateVietQrUrl(
                paymentSetting.getBankBin(),
                paymentSetting.getAccountNumber(),
                paymentSetting.getAccountName(),
                invoice.getTotalAmount(),
                invoice.getTransferCode()
        );

        return PublicInvoiceResponse.builder()
                .publicHash(invoice.getPublicHash())
                .className(classEntity.getName())
                .studentName(student.getFullName())
                .nickname(student.getNickname())
                .billingMonth(invoice.getBillingMonth())
                .attendedCount(invoice.getAttendedCount())
                .unitPrice(invoice.getUnitPrice())
                .extraFee(invoice.getExtraFee())
                .discount(invoice.getDiscount())
                .totalAmount(invoice.getTotalAmount())
                .transferCode(invoice.getTransferCode())
                .status(invoice.getStatus())
                .paidAt(invoice.getPaidAt())
                .note(invoice.getNote())
                .bankBin(paymentSetting.getBankBin())
                .bankName(paymentSetting.getBankName())
                .accountNumber(paymentSetting.getAccountNumber())
                .accountName(paymentSetting.getAccountName())
                .vietQrUrl(vietQrUrl)
                .attendedDates(attendedDates)
                .build();
    }

    // ─── PRIVATE HELPER METHODS ──────────────────────────────────────────────────

    private InvoiceResponse toInvoiceResponse(Invoice inv, ClassEntity clazz, Student student, PaymentSetting setting) {
        String vietQrUrl = vietQrService.generateVietQrUrl(
                setting.getBankBin(),
                setting.getAccountNumber(),
                setting.getAccountName(),
                inv.getTotalAmount(),
                inv.getTransferCode()
        );

        return InvoiceResponse.builder()
                .id(inv.getId())
                .classId(inv.getClassId())
                .className(clazz != null ? clazz.getName() : null)
                .studentId(inv.getStudentId())
                .studentName(student != null ? student.getFullName() : null)
                .nickname(student != null ? student.getNickname() : null)
                .parentPhone(student != null ? student.getParentPhone() : null)
                .billingMonth(inv.getBillingMonth())
                .attendedCount(inv.getAttendedCount())
                .unitPrice(inv.getUnitPrice())
                .extraFee(inv.getExtraFee())
                .discount(inv.getDiscount())
                .totalAmount(inv.getTotalAmount())
                .transferCode(inv.getTransferCode())
                .publicHash(inv.getPublicHash())
                .publicUrl("/p/" + inv.getPublicHash())
                .vietQrUrl(vietQrUrl)
                .status(inv.getStatus())
                .paidAt(inv.getPaidAt())
                .paidMethod(inv.getPaidMethod())
                .note(inv.getNote())
                .createdAt(inv.getCreatedAt())
                .build();
    }

    private String buildVietQrUrl(String bankBin, String accountNo, String accountName, BigDecimal amount, String transferCode) {
        if (bankBin == null || accountNo == null) {
            return null;
        }
        String encodedName = (accountName != null)
                ? URLEncoder.encode(removeDiacritics(accountName).toUpperCase(), StandardCharsets.UTF_8)
                : "";
        long amountLong = (amount != null) ? amount.longValue() : 0L;

        return String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankBin, accountNo, amountLong, transferCode, encodedName
        );
    }

    private String generateUniquePublicHash() {
        String hash;
        do {
            hash = UUID.randomUUID().toString().replace("-", "");
        } while (invoiceRepository.existsByPublicHash(hash));
        return hash;
    }

    private String generateUniqueTransferCode(String prefix, String className, Student student, String monthSuffix) {
        String cleanPrefix = (prefix != null && !prefix.isBlank()) ? removeDiacritics(prefix).toUpperCase() : "HP";
        String cleanClass = removeDiacritics(className).replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (cleanClass.length() > 6) {
            cleanClass = cleanClass.substring(0, 6);
        }

        String studentKey;
        if (student.getNickname() != null && !student.getNickname().isBlank()) {
            studentKey = removeDiacritics(student.getNickname()).replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            if (studentKey.length() > 4) {
                studentKey = studentKey.substring(0, 4);
            }
        } else {
            String cleanName = removeDiacritics(student.getFullName()).replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            studentKey = (cleanName.length() >= 4) ? cleanName.substring(cleanName.length() - 4) : cleanName;
        }

        String baseCode = cleanPrefix + cleanClass + studentKey + monthSuffix;
        String code = baseCode;
        int counter = 1;
        while (invoiceRepository.existsByTransferCode(code)) {
            code = baseCode + counter;
            counter++;
        }
        return code;
    }

    private String removeDiacritics(String str) {
        if (str == null) return "";
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("đ", "d")
                .replace("Đ", "D")
                .trim();
    }

    private ClassEntity findClassOrThrow(UUID classId, UUID tenantId) {
        return classRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CLASS_NOT_FOUND", "Không tìm thấy lớp học với ID: " + classId));
    }

    private PaymentSetting createDefaultPaymentSetting(UUID tenantId) {
        return PaymentSetting.builder()
                .bankBin("970422") // MB Bank default
                .bankName("MBBank - Ngân hàng Quân Đội")
                .accountNumber("0987654321")
                .accountName("EDUPAY DEMO")
                .transferPrefix("HP")
                .build();
    }

    private void validateMonthFormat(String month) {
        if (month == null || !MONTH_PATTERN.matcher(month).matches()) {
            throw new BusinessException("INVALID_MONTH_FORMAT", "Định dạng tháng không hợp lệ (Bắt buộc là YYYY-MM, ví dụ 2026-10)");
        }
    }
}

