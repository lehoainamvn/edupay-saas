# ⚖️ BỘ LUẬT PHÁT TRIỂN & TIÊU CHUẨN MÃ NGUỒN (DEVELOPMENT RULES)

> **Dự án:** EduPay SaaS - Quản lý Học sinh & Đối soát Học phí VietQR  
> **Ngôn ngữ:** Backend Java (Spring Boot 3) | Frontend Angular 17+ | CSDL PostgreSQL 16

---

## PHẦN 1: TIÊU CHUẨN BACKEND JAVA (SPRING BOOT 3)

### 1. Kiến trúc phân lớp nghiêm ngặt (Strict Layered Architecture)
* **`Controller`**: Chỉ xử lý request HTTP, validate dữ liệu đầu vào (`@Valid`), gọi Service và trả về `ResponseEntity<ApiResponse<T>>`. Tuyệt đối không viết logic nghiệp vụ trong Controller!
* **`Service`**: Nơi chứa 100% nghiệp vụ (Business Logic). Quản lý transaction bằng `@Transactional`.
* **`Repository`**: Chỉ thao tác với CSDL qua Spring Data JPA hoặc JPQL/Native SQL có kiểm soát.
* **`DTO`**: Tách bạch hoàn toàn giữa `RequestDTO` (nhận vào) và `ResponseDTO` (trả ra). Không bao giờ trả trực tiếp JPA Entity ra cho Client. Dùng **MapStruct** để map qua lại.

### 2. Định dạng phản hồi API chuẩn (Standard API Response)
Mọi API trả về JSON phải tuân thủ format:
```json
{
  "success": true,
  "message": "Điểm danh buổi học thành công",
  "data": { ... },
  "timestamp": "2026-10-24T11:50:00Z"
}
```
Khi có lỗi:
```json
{
  "success": false,
  "errorCode": "STUDENT_NOT_FOUND",
  "message": "Không tìm thấy thông tin học sinh trong lớp của bạn",
  "errors": [],
  "timestamp": "2026-10-24T11:50:00Z"
}
```

### 3. Xử lý ngoại lệ tập trung (Global Exception Handling)
* Sử dụng `@RestControllerAdvice` và `@ExceptionHandler`.
* Tạo các Exception rõ nghĩa: `ResourceNotFoundException`, `TenantAccessDeniedException`, `InvalidInvoiceStateException`.
* Không dùng `e.printStackTrace()`, luôn dùng logger: `log.error("Chi tiết lỗi: ", e);`.

---

## PHẦN 2: TIÊU CHUẨN FRONTEND ANGULAR (ANGULAR 17+)

### 1. Triết lý Thiết kế Mobile-First (BẮT BUỘC)
* 90% giáo viên dùng điện thoại để điểm danh và gửi link qua Zalo.
* Kích thước nút bấm cảm ứng (Touch Target): **Tối thiểu 44px × 44px**, lý tưởng là 48px.
* Thẻ học sinh, nút đổi trạng thái điểm danh phải to, màu sắc tương phản rõ (Xanh: Có mặt, Vàng: Nghỉ phép, Đỏ: Nghỉ không phép).

### 2. Tiêu chuẩn Mã nguồn Angular
* Ưu tiên sử dụng **Standalone Components** (Angular 17+).
* Mọi form nhập liệu phải dùng **Reactive Forms** có validation rõ ràng (báo lỗi màu đỏ ngay dưới ô nhập).
* Sử dụng **Async Pipe (`| async`)** hoặc `Signals` để quản lý state và tránh rò rỉ bộ nhớ (Memory Leak), không subscribe vô tận.
* Mọi HTTP call phải thông qua `ApiService` và đi qua `AuthInterceptor` (tự đính kèm `Bearer <JWT_TOKEN>`).

---

## PHẦN 3: TIÊU CHUẨN VIETQR & ĐỐI SOÁT THANH TOÁN

### 1. Chuẩn link VietQR động (Napas247)
URL ảnh QR động sinh theo định dạng:
```text
https://img.vietqr.io/image/{BANK_BIN}-{ACCOUNT_NO}-compact2.png?amount={AMOUNT}&addInfo={TRANSFER_CODE}&accountName={ACCOUNT_NAME}
```
* `amount`: Số tiền chính xác từng đồng (không có số thập phân).
* `addInfo`: Mã đối soát định danh duy nhất (VD: `HP-T10-TOAN9A-001`). Tuyệt đối không dùng ký tự đặc biệt có dấu tiếng Việt trong mã này.

### 2. Trang Tra Cứu Phụ Huynh (Public Route)
* Route `/p/:hash` phải load nhanh dưới 1.5 giây trên 4G di động.
* Không yêu cầu đăng nhập.
* Có sẵn nút:
  * 📋 **Sao chép Số tài khoản** (Copy vào clipboard kèm toast thông báo).
  * 📋 **Sao chép Nội dung chuyển khoản**.
  * 📥 **Tải ảnh mã QR về máy**.
  * 📱 **Mở App Ngân hàng** (Deep link).

---

## PHẦN 4: CHECKLIST TRƯỚC KHI TẠO PULL REQUEST (DEV CHECKLIST)

Trước khi gửi PR cho Leader duyệt, Dev tự tích kiểm tra:
- [ ] Code không vi phạm luật Multi-tenant (đã lọc theo `tenant_id`).
- [ ] Không có file thừa, không có file `.env` hoặc secret keys bị commit.
- [ ] Chạy build local thành công (không có lỗi compile / lint).
- [ ] Tên nhánh và commit message tuân thủ đúng quy chuẩn.
- [ ] Đã test giao diện trên kích thước màn hình điện thoại (Mobile Viewport 375px - 430px).
