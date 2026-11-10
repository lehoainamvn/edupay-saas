package com.edupay.service.impl;

import com.edupay.entity.Student;
import com.edupay.repository.InvoiceRepository;
import com.edupay.service.VietQRService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Triển khai dịch vụ sinh mã chuyển khoản định danh và liên kết ảnh VietQR chuẩn Napas247.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VietQRServiceImpl implements VietQRService {

    private final InvoiceRepository invoiceRepository;

    @Override
    public String generateTransferCode(String prefix, String billingMonth, String className, Student student) {
        // 1. Tiền tố (mặc định HP)
        String cleanPrefix = (prefix != null && !prefix.isBlank())
                ? removeDiacritics(prefix).replaceAll("[^A-Za-z0-9]", "").toUpperCase()
                : "HP";

        // 2. Tháng thu phí: "2026-10" -> "10", "2026-05" -> "05"
        String monthPart = extractMonthPart(billingMonth);

        // 3. Tên lớp học rút gọn không dấu: "Toán 9A" -> "TOAN9A"
        String cleanClass = removeDiacritics(className).replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (cleanClass.length() > 8) {
            cleanClass = cleanClass.substring(0, 8);
        }
        if (cleanClass.isEmpty()) {
            cleanClass = "LOP";
        }

        // 4. Mã định danh học sinh: ưu tiên biệt danh ở nhà, nếu không có lấy tên gọi
        String studentKey = extractStudentKey(student);

        // Ghép theo chuẩn định danh: HP-[THANG]-[TEN_LOP]-[MA_HS]
        String baseCode = String.format("%s-%s-%s-%s", cleanPrefix, monthPart, cleanClass, studentKey);
        String finalCode = baseCode;
        int counter = 1;

        // Đảm bảo tính duy nhất tuyệt đối trong hệ thống hóa đơn
        while (invoiceRepository.existsByTransferCode(finalCode)) {
            finalCode = baseCode + counter;
            counter++;
        }

        log.debug("Sinh mã chuyển khoản định danh: [{}] cho học sinh [{}]", finalCode, student.getFullName());
        return finalCode;
    }

    @Override
    public String generateVietQrUrl(String bankBin, String accountNumber, String accountName, BigDecimal amount, String transferCode) {
        if (bankBin == null || bankBin.isBlank() || accountNumber == null || accountNumber.isBlank()) {
            return null;
        }

        String cleanBin = bankBin.trim();
        String cleanAccountNo = accountNumber.replaceAll("[^A-Za-z0-9]", "");
        long amountLong = (amount != null) ? Math.max(0L, amount.longValue()) : 0L;

        // Xử lý URL-safe cho tên chủ tài khoản và nội dung chuyển khoản
        String cleanName = removeDiacritics(accountName != null ? accountName : "").toUpperCase();
        String encodedAccountName = URLEncoder.encode(cleanName, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedAddInfo = URLEncoder.encode(transferCode != null ? transferCode : "", StandardCharsets.UTF_8).replace("+", "%20");

        // Format chuẩn Napas247 compact2
        return String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                cleanBin,
                cleanAccountNo,
                amountLong,
                encodedAddInfo,
                encodedAccountName
        );
    }

    private String extractMonthPart(String billingMonth) {
        if (billingMonth == null || billingMonth.isBlank()) {
            return "00";
        }
        // Kỳ dạng YYYY-MM
        String[] parts = billingMonth.split("-");
        if (parts.length >= 2) {
            return parts[1]; // VD "10" hoặc "05"
        }
        return billingMonth.replaceAll("[^0-9]", "");
    }

    private String extractStudentKey(Student student) {
        if (student == null) {
            return "HS";
        }

        // Ưu tiên biệt danh ở nhà
        if (student.getNickname() != null && !student.getNickname().isBlank()) {
            String cleanNick = removeDiacritics(student.getNickname()).replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            if (!cleanNick.isEmpty()) {
                return cleanNick.length() > 6 ? cleanNick.substring(0, 6) : cleanNick;
            }
        }

        // Nếu không có biệt danh, lấy tên cuối (First Name)
        String fullName = student.getFullName();
        if (fullName != null && !fullName.isBlank()) {
            String cleanFull = removeDiacritics(fullName).trim();
            String[] words = cleanFull.split("\\s+");
            String lastName = words[words.length - 1].replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            if (!lastName.isEmpty()) {
                return lastName.length() > 6 ? lastName.substring(0, 6) : lastName;
            }
        }

        return "HS";
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
