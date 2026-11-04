package com.edupay.service;

import com.edupay.dto.request.InvoiceBatchGenerateRequest;
import com.edupay.dto.request.InvoicePaymentManualRequest;
import com.edupay.dto.response.BillingPreviewResponse;
import com.edupay.dto.response.InvoiceResponse;
import com.edupay.dto.response.PublicInvoiceResponse;
import com.edupay.entity.InvoiceStatus;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý Tính học phí, Xuất phiếu thu VietQR và Cổng phụ huynh.
 * BẮT BUỘC: 100% logic nghiệp vụ được thực thi tại đây và cô lập theo TenantContext (P0).
 */
public interface BillingService {

    /**
     * Lấy dữ liệu xem trước bảng tính học phí cả lớp trong tháng.
     */
    BillingPreviewResponse getBillingPreview(UUID classId, String billingMonth);

    /**
     * Xuất phiếu thu hàng loạt cho danh sách học sinh được chọn.
     */
    List<InvoiceResponse> generateInvoices(InvoiceBatchGenerateRequest request);

    /**
     * Lấy danh sách phiếu thu đã xuất (lọc theo lớp, tháng, trạng thái).
     */
    List<InvoiceResponse> getInvoices(UUID classId, String billingMonth, InvoiceStatus status);

    /**
     * Lấy chi tiết 1 phiếu thu theo ID.
     */
    InvoiceResponse getInvoiceById(UUID invoiceId);

    /**
     * Hủy phiếu thu (để mở khóa sửa điểm danh).
     */
    void cancelInvoice(UUID invoiceId);

    /**
     * Gạch nợ thủ công (Đã thu tiền mặt hoặc CK).
     */
    InvoiceResponse markAsPaid(UUID invoiceId, InvoicePaymentManualRequest request);

    /**
     * Tra cứu phiếu thu công khai cho Phụ huynh qua mã hash (không cần đăng nhập).
     */
    PublicInvoiceResponse getPublicInvoice(String publicHash);
}

