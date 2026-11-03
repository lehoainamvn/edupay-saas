package com.edupay.entity;

/**
 * Trạng thái của Phiếu thu học phí (Invoices).
 */
public enum InvoiceStatus {
    PENDING,    // Chờ nộp tiền
    PAID,       // Đã thanh toán / Đã đối soát
    CANCELLED   // Đã hủy phiếu thu
}

