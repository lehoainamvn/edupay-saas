package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.PaymentSettingRequest;
import com.edupay.dto.response.PaymentSettingResponse;
import com.edupay.service.PaymentSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings/payment")
@RequiredArgsConstructor
@Tag(name = "Cài đặt Thanh toán VietQR (Payment Settings)", description = "API cho phép giáo viên tự cấu hình và quản lý tài khoản ngân hàng nhận học phí Napas247")
public class PaymentSettingController {

    private final PaymentSettingService paymentSettingService;

    @GetMapping
    @Operation(summary = "Lấy thông tin tài khoản ngân hàng nhận tiền", description = "Truy vấn STK, ngân hàng và tiền tố chuyển khoản của giáo viên đang đăng nhập")
    public ResponseEntity<ApiResponse<PaymentSettingResponse>> getPaymentSetting() {
        PaymentSettingResponse data = paymentSettingService.getPaymentSetting();
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin tài khoản ngân hàng thành công"));
    }

    @PutMapping
    @Operation(summary = "Lưu hoặc Cập nhật tài khoản ngân hàng nhận tiền", description = "Thiết lập STK, ngân hàng, tên chủ tài khoản và tiền tố chuyển khoản (Upsert)")
    public ResponseEntity<ApiResponse<PaymentSettingResponse>> savePaymentSetting(
            @Valid @RequestBody PaymentSettingRequest request) {
        PaymentSettingResponse data = paymentSettingService.savePaymentSetting(request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lưu cấu hình tài khoản ngân hàng thành công"));
    }
}

