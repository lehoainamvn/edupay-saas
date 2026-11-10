package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.BankWebhookPayload;
import com.edupay.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller tiếp nhận Webhook biến động số dư từ cổng ngân hàng (SePay / Casso).
 * Endpoint này nằm trong danh sách Public Whitelist không yêu cầu JWT User.
 */
@Slf4j
@RestController
@RequestMapping({"/webhooks", "/api/webhooks"})
@RequiredArgsConstructor
@Tag(name = "Webhooks Ngân hàng (SePay / Casso)", description = "Tiếp nhận webhook biến động số dư, tự động đối soát VietQR và gạch nợ tức thì")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/payment")
    @Operation(summary = "Tiếp nhận Webhook giao dịch thanh toán",
            description = "Xác thực chữ ký, bóc tách mã chuyển khoản, gạch nợ phiếu thu và đảm bảo tính idempotent chống trùng")
    public ResponseEntity<ApiResponse<String>> handlePaymentWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secretHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody BankWebhookPayload payload) {

        String token = (secretHeader != null && !secretHeader.isBlank()) ? secretHeader : authHeader;
        String result = webhookService.processBankWebhook(token, payload);
        return ResponseEntity.ok(ApiResponse.ok(result, "Xử lý webhook giao dịch thành công"));
    }
}
