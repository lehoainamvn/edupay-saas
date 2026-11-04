package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.PaymentSettingRequest;
import com.edupay.dto.response.PaymentSettingResponse;
import com.edupay.entity.PaymentSetting;
import com.edupay.repository.PaymentSettingRepository;
import com.edupay.security.SecuritySanitizer;
import com.edupay.service.PaymentSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Triển khai dịch vụ Quản lý cấu hình tài khoản ngân hàng nhận tiền VietQR (Self-Service).
 * 100% nghiệp vụ cô lập theo TenantContext.getTenantId() (P0).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentSettingServiceImpl implements PaymentSettingService {

    private final PaymentSettingRepository paymentSettingRepository;

    @Override
    public PaymentSettingResponse getPaymentSetting() {
        UUID tenantId = TenantContext.getTenantId();
        Optional<PaymentSetting> opt = paymentSettingRepository.findByTenantId(tenantId);

        if (opt.isEmpty()) {
            return PaymentSettingResponse.builder()
                    .configured(false)
                    .transferPrefix("HP")
                    .build();
        }

        PaymentSetting setting = opt.get();
        return toResponse(setting);
    }

    @Override
    @Transactional
    public PaymentSettingResponse savePaymentSetting(PaymentSettingRequest request) {
        UUID tenantId = TenantContext.getTenantId();

        // 1. Chuẩn hóa dữ liệu đầu vào
        String cleanBin = request.getBankBin().trim();
        String cleanBankName = SecuritySanitizer.sanitizePlainText(request.getBankName().trim());
        String cleanAccNum = request.getAccountNumber().trim().replaceAll("\\s+", "");
        String cleanAccName = removeVietnameseAccents(request.getAccountName().trim()).toUpperCase();

        String cleanPrefix = (request.getTransferPrefix() != null && !request.getTransferPrefix().isBlank())
                ? request.getTransferPrefix().trim().toUpperCase().replaceAll("[^A-Z0-9]", "")
                : "HP";
        if (cleanPrefix.isBlank()) {
            cleanPrefix = "HP";
        }

        // 2. Tìm cấu hình hiện tại để Upsert
        Optional<PaymentSetting> existingOpt = paymentSettingRepository.findByTenantId(tenantId);
        PaymentSetting setting;

        if (existingOpt.isPresent()) {
            setting = existingOpt.get();
            setting.setBankBin(cleanBin);
            setting.setBankName(cleanBankName);
            setting.setAccountNumber(cleanAccNum);
            setting.setAccountName(cleanAccName);
            setting.setTransferPrefix(cleanPrefix);
            log.info("Cập nhật cấu hình nhận tiền VietQR cho Tenant: {}", tenantId);
        } else {
            setting = PaymentSetting.builder()
                    .bankBin(cleanBin)
                    .bankName(cleanBankName)
                    .accountNumber(cleanAccNum)
                    .accountName(cleanAccName)
                    .transferPrefix(cleanPrefix)
                    .build();
            setting.setTenantId(tenantId);
            log.info("Tạo mới cấu hình nhận tiền VietQR cho Tenant: {}", tenantId);
        }

        PaymentSetting saved = paymentSettingRepository.save(setting);
        return toResponse(saved);
    }

    private PaymentSettingResponse toResponse(PaymentSetting setting) {
        String sampleUrl = buildSampleVietQrUrl(setting.getBankBin(), setting.getAccountNumber(), setting.getAccountName());
        return PaymentSettingResponse.builder()
                .id(setting.getId())
                .bankBin(setting.getBankBin())
                .bankName(setting.getBankName())
                .accountNumber(setting.getAccountNumber())
                .accountName(setting.getAccountName())
                .transferPrefix(setting.getTransferPrefix())
                .vietQrSampleUrl(sampleUrl)
                .configured(true)
                .build();
    }

    private String buildSampleVietQrUrl(String bankBin, String accountNumber, String accountName) {
        try {
            String encodedName = URLEncoder.encode(accountName != null ? accountName : "", StandardCharsets.UTF_8);
            return String.format(
                    "https://img.vietqr.io/image/%s-%s-compact2.png?amount=50000&addInfo=TEST%%20EDUPAY&accountName=%s",
                    bankBin, accountNumber, encodedName);
        } catch (Exception e) {
            return String.format("https://img.vietqr.io/image/%s-%s-compact2.png", bankBin, accountNumber);
        }
    }

    private String removeVietnameseAccents(String str) {
        if (str == null) return "";
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}

