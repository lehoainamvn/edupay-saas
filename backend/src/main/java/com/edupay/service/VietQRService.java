package com.edupay.service;

import com.edupay.entity.Student;

import java.math.BigDecimal;

/**
 * Service chuyên biệt sinh mã chuyển khoản định danh và liên kết ảnh VietQR chuẩn Napas247.
 * Tuân thủ quy chuẩn chuyển khoản ngân hàng không dấu tiếng Việt và URL-safe encoding.
 */
public interface VietQRService {

    /**
     * Sinh mã chuyển khoản đối soát định danh theo cú pháp:
     * HP-[THANG]-[TEN_LOP]-[MA_HS]
     * Ví dụ: HP-10-TOAN9A-001 hoặc HP-10-TOAN9A-BONG
     *
     * @param prefix Tiền tố đối soát (mặc định "HP")
     * @param billingMonth Kỳ thu phí dạng YYYY-MM (VD: "2026-10")
     * @param className Tên lớp học (VD: "Toán 9A")
     * @param student Thông tin học sinh (lấy biệt danh hoặc họ tên/mã)
     * @return Mã chuyển khoản viết hoa, không dấu, an toàn cho hệ thống ngân hàng
     */
    String generateTransferCode(String prefix, String billingMonth, String className, Student student);

    /**
     * Sinh URL ảnh mã QR động theo chuẩn Napas247 (VietQR API).
     *
     * @param bankBin Mã 6 số của ngân hàng thụ hưởng (VD: 970422 cho MBBank)
     * @param accountNumber Số tài khoản thụ hưởng
     * @param accountName Tên chủ tài khoản (sẽ được encode URL-safe)
     * @param amount Số tiền học phí chính xác từng đồng
     * @param transferCode Mã chuyển khoản định danh
     * @return Đường dẫn ảnh QR đầy đủ (VD: https://img.vietqr.io/image/970422-0987654321-compact2.png?...)
     */
    String generateVietQrUrl(String bankBin, String accountNumber, String accountName, BigDecimal amount, String transferCode);
}
