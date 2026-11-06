package com.edupay.service;

import com.edupay.dto.request.PaymentSettingRequest;
import com.edupay.dto.response.BankResponse;
import com.edupay.dto.response.PaymentSettingResponse;

import java.util.List;

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

    /**
     * Lấy danh sách 54+ ngân hàng hỗ trợ chuẩn VietQR Napas247.
     * Tự động gọi VietQR API v2 (https://api.vietqr.io/v2/banks) kèm cơ chế cache và danh sách dự phòng.
     */
    List<BankResponse> getSupportedBanks();
}
