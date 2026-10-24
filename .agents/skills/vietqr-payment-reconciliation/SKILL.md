---
name: vietqr-payment-reconciliation
description: Technical guidelines, URL formats, webhook handlers, and idempotency patterns for VietQR Napas247 generation and automatic tuition fee reconciliation in EduPay SaaS.
---

# ⚡ VietQR Payment & Automatic Reconciliation Skill

Use this skill whenever you are generating VietQR codes, building the public parent portal (`/p/:hash`), handling banking webhooks (SePay, Casso), or updating invoice payment states in EduPay SaaS.

---

## 1. Chuẩn Sinh URL VietQR Động (Napas247)

Mã QR động được sinh trực tiếp theo chuẩn Napas247 thông qua dịch vụ VietQR API:

```text
https://img.vietqr.io/image/{BANK_BIN}-{ACCOUNT_NO}-{TEMPLATE}.png?amount={AMOUNT}&addInfo={TRANSFER_CODE}&accountName={ACCOUNT_NAME}
```

### Tham Số Chi Tiết:
- `{BANK_BIN}`: Mã định danh 6 chữ số của ngân hàng (VD: `970422` cho MBBank, `970415` cho VietinBank, `970436` cho Vietcombank).
- `{ACCOUNT_NO}`: Số tài khoản thụ hưởng của giáo viên / trung tâm.
- `{TEMPLATE}`: Mẫu giao diện QR, chuẩn dùng là `compact2` (gọn gàng, có logo VietQR + Napas247).
- `{AMOUNT}`: Số tiền chính xác từng đồng (không có số thập phân). VD: `650000`.
- `{TRANSFER_CODE}`: Mã chuyển khoản đối soát định danh (VD: `HP9A00124`).
- `{ACCOUNT_NAME}`: Tên chủ tài khoản (URL encoded, chữ hoa không dấu). VD: `LE%20HOAI%20NAM`.

### Quy Chuẩn Sinh Mã Đối Soát (`TRANSFER_CODE`):
1. **Tuyệt đối không dùng dấu tiếng Việt và ký tự đặc biệt:** Chỉ dùng chữ cái hoa (`A-Z`) và số (`0-9`).
2. **Cấu trúc mã chuẩn:**
   ```text
   {PREFIX}{CLASS_CODE}{STUDENT_ID}{MONTH}
   Ví dụ: HP + 9A + 012 + 10  →  HP9A01210
   ```
3. Khách hàng quét mã này thì trong sao kê ngân hàng sẽ có đúng mã `HP9A01210`, giúp máy tự động nhận diện và gạch nợ 100% không nhầm lẫn.

---

## 2. Trang Tra Cứu Phụ Huynh (Public Portal `/p/:hash`)

Phụ huynh nhận link qua Zalo (VD: `https://edupay.vn/p/b8c19a0f-48d2...`).

### Nguyên Tắc Bắt Buộc:
- **Tuyệt đối không bắt đăng nhập:** Phụ huynh chỉ cần ấn vào link là thấy ngay chi tiết các buổi con đã học, số tiền và mã VietQR.
- **Bảo mật Hash:** Không truyền ID tăng dần (Auto-increment ID). Phải dùng `hash` (UUID v4 hoặc SHA-256 slug ngẫu nhiên) để chống người lạ đoán link học sinh khác.
- **Tiện ích 1 Chạm:**
  - Nút **Sao chép STK** (Kèm toast thông báo).
  - Nút **Sao chép Nội dung chuyển khoản**.
  - Nút **Tải ảnh QR về máy**.
  - Nút **Mở App Ngân hàng (Deeplink)**.

---

## 3. Webhook Xử Lý Đối Soát Tự Động (Auto-Reconciliation)

Endpoint tiếp nhận webhook từ SePay hoặc Casso: `POST /api/webhooks/payment`

### Quy Trình Xử Lý & Đảm Bảo Tính Bất Biến (Idempotency):

```
[Webhook Request]
       ↓
[Xác thực API Key / Secret Token] → (401 nếu sai Token)
       ↓
[Kiểm tra transaction_id trùng lặp] → (Bỏ qua nếu đã xử lý rồi - Idempotency)
       ↓
[Regex tìm TRANSFER_CODE trong nội dung CK]
       ↓
[Tìm Invoice tương ứng với mã đối soát]
       ↓
[So khớp số tiền: receivedAmount >= invoiceAmount]
       ↓
[Cập nhật Invoice: status = PAID, paidAt = now(), paymentMethod = VIETQR]
       ↓
[Ghi log vào bảng payment_transactions]
       ↓
[Phản hồi HTTP 200 { success: true }]
```

### Mã Mẫu Webhook Handler (Spring Boot):
```java
@PostMapping("/webhooks/payment")
@Transactional
public ResponseEntity<ApiResponse<Void>> handleBankWebhook(
        @RequestHeader("X-Webhook-Secret") String secret,
        @RequestBody BankWebhookPayload payload) {

    // 1. Validate Secret Token
    if (!webhookSecret.equals(secret)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // 2. Kiểm tra Idempotency - Tránh gạch nợ 2 lần khi webhook retry
    if (paymentTransactionRepository.existsByReferenceCode(payload.getReferenceNumber())) {
        log.warn("Giao dịch {} đã được xử lý trước đó", payload.getReferenceNumber());
        return ResponseEntity.ok(ApiResponse.success("Already processed", null));
    }

    // 3. Phân tích nội dung chuyển khoản tìm mã invoice
    String transferCode = extractTransferCode(payload.getContent());
    if (transferCode == null) {
        log.warn("Nội dung CK không chứa mã đối soát hợp lệ: {}", payload.getContent());
        return ResponseEntity.ok(ApiResponse.success("Ignored: no match", null));
    }

    // 4. Tìm kiếm và cập nhật trạng thái phiếu thu
    Invoice invoice = invoiceRepository.findByTransferCode(transferCode)
            .orElse(null);

    if (invoice != null && invoice.getStatus() == InvoiceStatus.PENDING) {
        if (payload.getAmount() >= invoice.getTotalAmount()) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now());
            invoice.setPaymentMethod("VIETQR");
            invoiceRepository.save(invoice);
            log.info("Gạch nợ thành công cho phiếu thu {} số tiền {}", invoice.getId(), payload.getAmount());
        }
    }

    // 5. Lưu lại lịch sử giao dịch
    savePaymentTransaction(payload, invoice);

    return ResponseEntity.ok(ApiResponse.success("Webhook processed", null));
}
```
