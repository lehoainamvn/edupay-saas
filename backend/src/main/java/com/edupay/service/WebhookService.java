package com.edupay.service;

import com.edupay.dto.request.BankWebhookPayload;

/**
 * Service tiếp nhận và xử lý Webhook biến động số dư từ cổng ngân hàng (SePay / Casso).
 * Đảm bảo tính Idempotent (chống gạch nợ trùng lặp) và tự động đối soát VietQR.
 */
public interface WebhookService {

    /**
     * Xử lý webhook giao dịch ngân hàng, bóc tách mã chuyển khoản và gạch nợ tự động.
     *
     * @param secretHeader Giá trị X-Webhook-Secret hoặc Authorization từ cổng ngân hàng
     * @param payload Dữ liệu giao dịch ngân hàng
     * @return Thông điệp kết quả đối soát
     */
    String processBankWebhook(String secretHeader, BankWebhookPayload payload);
}
