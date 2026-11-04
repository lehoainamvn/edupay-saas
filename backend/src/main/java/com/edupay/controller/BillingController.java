package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.InvoiceBatchGenerateRequest;
import com.edupay.dto.request.InvoicePaymentManualRequest;
import com.edupay.dto.response.BillingPreviewResponse;
import com.edupay.dto.response.InvoiceResponse;
import com.edupay.entity.InvoiceStatus;
import com.edupay.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller quản lý Tính học phí tự động, Bảng tính xem trước và Quản lý Phiếu thu VietQR.
 * Chỉ xử lý HTTP & Validation (@Valid), 100% nghiệp vụ nằm tại BillingService.
 */
@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
@Tag(name = "Tính học phí & Phiếu thu (Billing)", description = "API tính học phí tự động theo số buổi điểm danh, xem trước bảng tính, xuất phiếu thu VietQR hàng loạt và gạch nợ")
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/preview")
    @Operation(summary = "Xem trước bảng tính học phí cả lớp theo tháng",
            description = "Tự động đếm số buổi có mặt (PRESENT), áp dụng đơn giá riêng hoặc đơn giá lớp, tính sẵn số tiền học phí và kiểm tra trạng thái khóa phiếu thu.")
    public ResponseEntity<ApiResponse<BillingPreviewResponse>> getBillingPreview(
            @RequestParam(name = "classId") UUID classId,
            @RequestParam(name = "month") String month) {
        BillingPreviewResponse data = billingService.getBillingPreview(classId, month);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy dữ liệu bảng tính học phí thành công"));
    }

    @PostMapping("/invoices/generate")
    @Operation(summary = "Xuất phiếu thu học phí hàng loạt cho các học sinh được chọn",
            description = "Tạo phiếu thu, sinh mã chuyển khoản Napas247 không dấu, sinh link public hash /p/:hash và tự động khóa điểm danh của học sinh trong tháng.")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> generateInvoices(
            @Valid @RequestBody InvoiceBatchGenerateRequest request) {
        List<InvoiceResponse> data = billingService.generateInvoices(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, String.format("Xuất thành công %d phiếu thu học phí", data.size())));
    }

    @GetMapping("/invoices")
    @Operation(summary = "Lấy danh sách phiếu thu đã xuất",
            description = "Hỗ trợ lọc theo lớp học, kỳ thu phí (YYYY-MM) và trạng thái (PENDING, PAID, CANCELLED).")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoices(
            @RequestParam(name = "classId", required = false) UUID classId,
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "status", required = false) InvoiceStatus status) {
        List<InvoiceResponse> data = billingService.getInvoices(classId, month, status);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách phiếu thu thành công"));
    }

    @GetMapping("/invoices/{id}")
    @Operation(summary = "Lấy chi tiết 1 phiếu thu theo ID",
            description = "Trả về thông tin chi tiết phiếu thu, bao gồm link VietQR Napas247 và link public.")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable UUID id) {
        InvoiceResponse data = billingService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy chi tiết phiếu thu thành công"));
    }

    @PostMapping("/invoices/{id}/cancel")
    @Operation(summary = "Hủy phiếu thu học phí",
            description = "Chuyển trạng thái phiếu sang CANCELLED và mở khóa để giáo viên có thể chỉnh sửa lại điểm danh của học sinh trong tháng đó.")
    public ResponseEntity<ApiResponse<Void>> cancelInvoice(@PathVariable UUID id) {
        billingService.cancelInvoice(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã hủy phiếu thu học phí thành công"));
    }

    @PostMapping("/invoices/{id}/mark-paid")
    @Operation(summary = "Gạch nợ thủ công phiếu thu (Đã thu tiền)",
            description = "Xác nhận học sinh đã hoàn tất nộp tiền (CASH: Tiền mặt, MANUAL: Chuyển khoản xác nhận tay, VIETQR: Quét mã).")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markAsPaid(
            @PathVariable UUID id,
            @Valid @RequestBody InvoicePaymentManualRequest request) {
        InvoiceResponse data = billingService.markAsPaid(id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Gạch nợ phiếu thu thành công"));
    }
}
