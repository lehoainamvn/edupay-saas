package com.edupay.service.impl;

import com.edupay.dto.request.BankWebhookPayload;
import com.edupay.entity.Invoice;
import com.edupay.entity.InvoiceStatus;
import com.edupay.entity.PaymentTransaction;
import com.edupay.exception.BusinessException;
import com.edupay.repository.InvoiceRepository;
import com.edupay.repository.PaymentTransactionRepository;
import com.edupay.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private static final Pattern TRANSFER_CODE_PATTERN = Pattern.compile("HP[-_]?[0-9]{2}[-_]?[A-Z0-9]+[-_]?[A-Z0-9]*", Pattern.CASE_INSENSITIVE);

    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${edupay.webhook.secret:edupay_webhook_secret_2026}")
    private String configuredWebhookSecret;

    @Override
    @Transactional
    public String processBankWebhook(String secretHeader, BankWebhookPayload payload) {
        validateSecret(secretHeader);

        // Hỗ trợ cả SePay payload và Casso wrapper payload
        if (payload.getData() != null && !payload.getData().isEmpty()) {
            int successCount = 0;
            for (BankWebhookPayload.CassoTransactionItem item : payload.getData()) {
                boolean processed = processSingleTransaction(
                        item.getTid() != null ? item.getTid() : String.valueOf(item.getId()),
                        item.getAmount(),
                        item.getDescription(),
                        item.getBankSubAccId(),
                        "Casso"
                );
                if (processed) successCount++;
            }
            return String.format("Casso webhook: Xử lý thành công %d/%d giao dịch", successCount, payload.getData().size());
        }

        // SePay standard payload
        String refNum = payload.getReferenceNumber() != null
                ? payload.getReferenceNumber()
                : (payload.getId() != null ? String.valueOf(payload.getId()) : null);

        if (refNum == null || refNum.isBlank()) {
            throw new BusinessException("INVALID_WEBHOOK_PAYLOAD", "Thiếu mã định danh giao dịch ngân hàng (referenceNumber)");
        }

        BigDecimal amount = payload.getAmountIn() != null ? payload.getAmountIn() : BigDecimal.ZERO;
        boolean processed = processSingleTransaction(
                refNum,
                amount,
                payload.getTransactionContent(),
                payload.getAccountNumber(),
                payload.getGateway() != null ? payload.getGateway() : "SePay"
        );

        return processed ? "Đối soát & gạch nợ thành công" : "Giao dịch đã được xử lý trước đó hoặc không khớp hóa đơn";
    }

    private boolean processSingleTransaction(
            String referenceNumber,
            BigDecimal amount,
            String content,
            String accountNumber,
            String gateway
    ) {
        // 1. Kiểm tra Idempotency - Không gạch nợ 2 lần khi webhook retry
        if (paymentTransactionRepository.existsByReferenceNumber(referenceNumber)) {
            log.warn("Giao dịch tham chiếu [{}] đã được xử lý trước đó (Idempotent ignore)", referenceNumber);
            return false;
        }

        if (content == null || content.isBlank()) {
            log.warn("Nội dung chuyển khoản trống cho giao dịch [{}]", referenceNumber);
            return false;
        }

        // 2. Tìm mã đối soát định danh trong nội dung chuyển khoản
        Optional<String> extractedCodeOpt = extractTransferCode(content);
        if (extractedCodeOpt.isEmpty()) {
            log.info("Không tìm thấy cú pháp mã đối soát học phí trong nội dung: [{}]", content);
            return false;
        }

        String transferCode = extractedCodeOpt.get();
        Optional<Invoice> invoiceOpt = findInvoiceByTransferCode(transferCode);

        if (invoiceOpt.isEmpty()) {
            log.warn("Không tìm thấy phiếu thu nào khớp mã đối soát [{}] từ giao dịch [{}]", transferCode, referenceNumber);
            return false;
        }

        Invoice invoice = invoiceOpt.get();

        // 3. Kiểm tra số tiền chuyển khớp hoặc vượt số tiền phiếu thu
        if (amount != null && invoice.getTotalAmount() != null) {
            if (amount.compareTo(invoice.getTotalAmount()) < 0) {
                log.warn("Số tiền chuyển [{}] nhỏ hơn số tiền phiếu thu [{}] cho mã [{}]",
                        amount, invoice.getTotalAmount(), transferCode);
                // Vẫn ghi log giao dịch thiếu để giáo viên kiểm tra
            }
        }

        // 4. Cập nhật phiếu thu sang trạng thái PAID
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        invoice.setConfirmedAt(Instant.now());
        invoice.setPaidMethod("VIETQR");
        invoiceRepository.save(invoice);

        // 5. Ghi log giao dịch vào bảng payment_transactions
        PaymentTransaction tx = PaymentTransaction.builder()
                .invoiceId(invoice.getId())
                .referenceNumber(referenceNumber)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .bankBrandName(gateway)
                .accountNumber(accountNumber)
                .transactionContent(content)
                .paymentSource("WEBHOOK")
                .confirmedBy(null) // Tự động bởi Webhook
                .transactionTime(Instant.now())
                .build();
        tx.setTenantId(invoice.getTenantId());

        paymentTransactionRepository.save(tx);
        log.info("Auto gạch nợ thành công phiếu thu [{}] của tenant [{}] với số tiền [{}]",
                invoice.getId(), invoice.getTenantId(), amount);

        // 6. Mô phỏng kích hoạt Zalo ZNS thông báo cho giáo viên
        sendZaloZnsNotification(invoice, amount);

        return true;
    }

    private void validateSecret(String secretHeader) {
        if (configuredWebhookSecret == null || configuredWebhookSecret.isBlank()) {
            return; // Dev mode
        }
        if (secretHeader == null || secretHeader.isBlank()) {
            log.warn("Webhook request thiếu Secret Header xác thực");
            throw new BusinessException("UNAUTHORIZED_WEBHOOK", "Thiếu chữ ký xác thực Webhook");
        }

        // Hỗ trợ cả "Apikey <secret>" hoặc "<secret>"
        String cleanHeader = secretHeader.replace("Apikey", "").replace("Bearer", "").trim();
        if (!configuredWebhookSecret.equals(cleanHeader)) {
            log.warn("Webhook secret không hợp lệ: [{}]", secretHeader);
            throw new BusinessException("UNAUTHORIZED_WEBHOOK", "Chữ ký xác thực Webhook không hợp lệ");
        }
    }

    private Optional<String> extractTransferCode(String content) {
        String normalized = removeDiacritics(content).toUpperCase();
        Matcher matcher = TRANSFER_CODE_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }

    private Optional<Invoice> findInvoiceByTransferCode(String transferCode) {
        // Thử tìm chính xác
        Optional<Invoice> exact = invoiceRepository.findAll().stream()
                .filter(i -> i.getTransferCode() != null && i.getTransferCode().equalsIgnoreCase(transferCode))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }

        // Thử tìm chuẩn hóa (loại bỏ dấu gạch ngang)
        String cleanCode = transferCode.replaceAll("[^A-Z0-9]", "");
        return invoiceRepository.findAll().stream()
                .filter(i -> i.getTransferCode() != null && i.getTransferCode().replaceAll("[^A-Z0-9]", "").equalsIgnoreCase(cleanCode))
                .findFirst();
    }

    private void sendZaloZnsNotification(Invoice invoice, BigDecimal amount) {
        log.info("[Zalo ZNS Auto-Dispatch] Đã gửi thông báo xác nhận thu học phí thành công tới Giáo viên Tenant [{}]: Phiếu thu [{}], Số tiền [{}] đ",
                invoice.getTenantId(), invoice.getTransferCode(), amount);
    }

    private String removeDiacritics(String str) {
        if (str == null) return "";
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("đ", "d")
                .replace("Đ", "D")
                .trim();
    }
}
