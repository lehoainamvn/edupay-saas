package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.response.PublicInvoiceResponse;
import com.edupay.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller phục vụ Cổng Phụ huynh tra cứu học phí và quét VietQR công khai (/p/:hash).
 * Endpoint này hoàn toàn không yêu cầu đăng nhập (Public Whitelist).
 */
@RestController
@RequestMapping("/public/invoices")
@RequiredArgsConstructor
@Tag(name = "Cổng Phụ huynh Tra cứu (Public Invoice)", description = "API công khai không cần đăng nhập dành cho phụ huynh quét VietQR và xem chi tiết buổi học của con")
public class PublicInvoiceController {

    private final BillingService billingService;

    @GetMapping("/{publicHash}")
    @Operation(summary = "Tra cứu phiếu thu bằng mã hash an toàn",
            description = "Trả về thông tin học phí, danh sách ngày con đã học, số tài khoản nhận tiền và mã QR Napas247 động.")
    public ResponseEntity<ApiResponse<PublicInvoiceResponse>> getPublicInvoice(@PathVariable String publicHash) {
        PublicInvoiceResponse data = billingService.getPublicInvoice(publicHash);
        return ResponseEntity.ok(ApiResponse.ok(data, "Tra cứu phiếu thu thành công"));
    }
}

