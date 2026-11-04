package com.edupay.service;

import com.edupay.dto.request.PaymentSettingRequest;
import com.edupay.dto.response.PaymentSettingResponse;

/**
 * Service quản lý Cấu hình tài khoản ngân hàng nhận tiền VietQR của giáo viên.
 * Luôn tuân thủ nguyên tắc Multi-Tenant Isolation (P0).
 */
public interface PaymentSettingService {

    /**
     * Lấy cấu hình tài khoản nhận tiền của Tenant hiện tại.
     * Nếu chưa cấu hình, trả về DTO với configured = false.
     */
    PaymentSettingResponse getPaymentSetting();

    /**
     * Tạo mới hoặc cập nhật (Upsert) cấu hình tài khoản nhận tiền cho Tenant hiện tại.
     */
    PaymentSettingResponse savePaymentSetting(PaymentSettingRequest request);
}

