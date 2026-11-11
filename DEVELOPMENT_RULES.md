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

## PHẦN 4: LUẬT TÁI SỬ DỤNG TÀI NGUYÊN & CHỐNG TRÙNG LẶP (REUSE-FIRST)

* **Nguyên tắc "Đã có là phải dùng":** Bất kỳ thành phần nào đã được thiết kế sẵn (thanh Navigation `<app-navbar>`, Modal, Toast, Card style, BaseEntity, DTO chuẩn, TokenService...) thì Developer và Agent **phải tự động vào lấy lại để tái sử dụng**, tuyệt đối không được tạo mới hoặc sao chép trùng lặp.
* **Không phân mảnh giao diện:** Mọi trang mới (Học sinh, Điểm danh, Học phí...) phải kế thừa trực tiếp các thành phần chung từ `src/app/shared/`, đảm bảo toàn bộ hệ thống đồng nhất 100% về kích thước, màu sắc và trải nghiệm.

---

## PHẦN 5: TIÊU CHUẨN GIAO DIỆN & CẤM DÙNG ICON / EMOJI DO AI BỊA (CLEAN UI LAW)

* **Cấm chèn emoji / icon lạ tự bịa:** Tuyệt đối **CẤM** chèn emoji hoặc icon ngẫu hứng do AI tự sinh (ví dụ: `👋`, `📱`, `⚡`, `🧮`, `🎓`, `💸`...) vào tiêu đề H1/H2, button label, kicker badge, và các mục menu.
* **Clean Typography:** Giao diện quản lý giảng dạy & tài chính B2B cho giáo viên cần sự chuẩn mực, tối giản, thanh lịch, dễ đọc (Clean Typography First).
* **Bộ Icon chuẩn:** Chỉ sử dụng icon vector tối giản đã chuẩn hóa (nét vẽ mỏng stroke `1.5px` - `1.8px`, mũi tên `→`, checkmark `✓`, `✕`).
* **Chốt thanh Navigation (`NavbarComponent`):** Sử dụng nhãn chữ thuần túy (Text-only tabs) cho các mục: "Tổng quan", "Lớp học", "Học sinh", "Điểm danh 1 chạm", "Học phí VietQR". Không chèn thêm icon thừa vào trước chữ.

---

## PHẦN 6: THỐNG NHẤT ROUTING TẬP TRUNG TẠI DUY NHẤT 1 FILE (SINGLE SOURCE OF TRUTH)

* **Duy nhất 1 file:** Toàn bộ cấu hình định tuyến (Routing) của ứng dụng Frontend **BẮT BUỘC** tập trung 100% tại duy nhất một file: `frontend/src/app/app.routes.ts`.
* **Cấm phân mảnh:** Tuyệt đối không tạo các file routing con phân tán trong từng feature. Mọi route mới, route con, redirect và guard đều phải khai báo trực tiếp trong `app.routes.ts`.

---

## PHẦN 7: KẾT NỐI API THẬT & THIẾT KẾ TRẠNG THÁI RỖNG (REAL API & EMPTY STATE FIRST)

* **Không dùng dữ liệu giả cứng (No Hardcoded Mock Data):** Mọi màn hình sau khi hoàn thiện giao diện bắt buộc phải kết nối API gọi xuống cơ sở dữ liệu thật để lấy và hiển thị dữ liệu động của Tenant.
* **Thiết kế Empty State chỉn chu:** Khi cơ sở dữ liệu chưa có bản ghi (chưa có lớp học, chưa có ca dạy hôm nay, chưa phát sinh giao dịch VietQR, không có học sinh nợ), giao diện phải hiển thị trạng thái rỗng sạch sẽ kèm thông báo hướng dẫn rõ ràng (ví dụ: *"Chưa có ca dạy nào hôm nay"*, *"Chưa có giao dịch VietQR nào phát sinh"*) kèm nút hành động tạo mới, tuyệt đối không để màn hình lỗi hay hiển thị dữ liệu giả.

---

## PHẦN 8: CHECKLIST TRƯỚC KHI TẠO PULL REQUEST (DEV CHECKLIST)

Trước khi gửi PR cho Leader duyệt, Dev tự tích kiểm tra:
- [ ] Code không vi phạm luật Multi-tenant (đã lọc theo `tenant_id`).
- [ ] Đã tái sử dụng tối đa các component và class có sẵn trong `shared/`, không tạo trùng lặp.
- [ ] Không chèn emoji hay icon lạ do AI tự bịa vào giao diện (Clean Typography).
- [ ] Tuân thủ tuyệt đối Bộ quy chuẩn Typography & Thiết kế cho độ tuổi 22 - 45 (Phần 9).
- [ ] Mọi cấu hình route đều nằm duy nhất trong `frontend/src/app/app.routes.ts`.
- [ ] Đã kết nối API thật và xử lý đầy đủ các trường hợp dữ liệu rỗng (Empty States).
- [ ] Không có file thừa, không có file `.env` hoặc secret keys bị commit.
- [ ] Chạy build local thành công (không có lỗi compile / lint).
- [ ] Tên nhánh và commit message tuân thủ đúng quy chuẩn.
- [ ] Đã test giao diện trên kích thước màn hình điện thoại (Mobile Viewport 375px - 430px).

---

## PHẦN 9: BỘ QUY CHUẨN TYPOGRAPHY & THIẾT KẾ GIAO DIỆN ĐỘ TUỔI 22 - 45 (BẮT BUỘC TOÀN HỆ THỐNG)

> **Mục tiêu:** Nhóm người dùng 22 - 45 tuổi (giáo viên, gia sư, chủ cơ sở) đòi hỏi sự hiện đại, gọn gàng, độ tương phản cao, dễ đọc, không gây mỏi mắt và tuyệt đối không bấm trượt trên thiết bị cảm ứng. Mọi màn hình mới hoặc khi chỉnh sửa giao diện **BẮT BUỘC** áp dụng chuẩn sau:

### 1. Phông chữ chuẩn (Font Family)
* **Font bắt buộc:** `'Be Vietnam Pro'` (fallback: `'Inter', -apple-system, BlinkMacSystemFont, sans-serif`).
* **Lý do:** Đây là font thiết kế chuẩn quốc tế dành riêng cho tiếng Việt, cân bằng hoàn hảo các dấu thanh (*sắc, huyền, hỏi, ngã, nặng, mũ, ơ, ư*), không bị dính vào ký tự hoặc đẩy dòng lệch.
* **Cấm:** Không dùng font Serif (chân), font trang trí viết tay hoặc font không hỗ trợ đầy đủ ký tự tiếng Việt.

### 2. Bảng phân cấp cỡ chữ & Kiểu chữ (Typography Scale)

| Thành phần giao diện | Cỡ chữ (Font Size) | Độ đậm (Font Weight) | Chiều cao dòng (Line Height) | Màu sắc (Color) | Ghi chú áp dụng |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Thanh Menu (Navbar)** | **14px** | 600 (SemiBold) | 1.4 | `#0f172a` (Active: `#2563eb`) | Chiều cao thanh Nav cố định `60px`. Tab text-only, khoảng cách `16px - 20px`. |
| **Nhãn dẫn (Eyebrow / Kicker)** | **11px – 12px** | 700 (Bold) | 1.3 | `#2563eb` hoặc `#64748b` | Viết hoa toàn bộ (`uppercase`), `letter-spacing: 0.05em`. VD: `ĐIỂM DANH BUỔI HỌC` |
| **Tiêu đề trang (H1)** | **24px – 26px** | 700 (Bold) | 1.25 – 1.3 | `#0f172a` / `#111827` | `letter-spacing: -0.02em`. VD: `Hôm nay, Lê Hoài Nam có 2 ca dạy.` |
| **Tiêu đề phân khu (H2)** | **17px – 18px** | 700 (Bold) | 1.35 | `#111827` | `letter-spacing: -0.015em`. VD: `Thời khóa biểu tuần`, `Ca dạy hôm nay` |
| **Tiêu đề thẻ / Card (H3)** | **15px – 16px** | 600 (SemiBold) | 1.4 | `#111827` | VD: Tên lớp học, tiêu đề bảng `Giao dịch VietQR gần đây` |
| **Nội dung chính (Body Text)** | **14px** | 400 (Regular) / 500 (Medium) | **1.5** | `#334155` | Tên học sinh, đoạn mô tả hướng dẫn, văn bản bảng dữ liệu |
| **Nội dung phụ (Meta / Subtext)** | **12.5px – 13px** | 400 (Regular) | 1.45 | `#64748b` | Số điện thoại PH, ngày giờ, địa điểm phòng học, mã chuyển khoản |
| **Nhãn trạng thái (Badge / Tag)** | **11px – 12px** | 600 (SemiBold) / 700 (Bold) | 1.2 | Xanh/Vàng/Đỏ tùy ngữ cảnh | Padding `2px 8px`, bo tròn `999px`. VD: `[T2] [T4] [T6]`, `● Lịch hôm nay`, `✓ Đã gạch nợ` |
| **Chỉ số lớn & Tiền tệ (KPI)** | **22px – 26px** | 700 (Bold) | 1.2 | `#0f172a` (Xanh: `#059669`) | Các số tiền tổng hợp: `150.000 đ`, `24.500.000 đ` |

### 3. Quy chuẩn Nút bấm (Button) & Ô nhập liệu (Input)
* **Nút bấm chính (Primary Action):**
  * Chiều cao: **`44px – 48px`** (đảm bảo Touch Target $\ge 44\text{px}$ cho cảm ứng điện thoại).
  * Chữ nút: **`14px – 14.5px`**, SemiBold (600), màu trắng `#ffffff`.
  * Bo góc: `8px – 10px`.
* **Nút bấm phụ / Mini (Secondary / Mini Button):**
  * Chiều cao: **`32px – 36px`**.
  * Chữ nút: **`12.5px – 13px`**, SemiBold (600).
* **Ô nhập liệu & Chọn ngày/tháng (Inputs & Pickers):**
  * Chiều cao: **`42px – 48px`**.
  * Chữ bên trong: **`14px – 14.5px`**, màu `#0f172a`.
  * Bộ chọn tháng **BẮT BUỘC** hiển thị thuần tiếng Việt: `Tháng 11/2026 📅`, cấm hiển thị 2 ô thừa cạnh nhau hoặc tiếng Anh (`November 2026`).

### 4. 3 Nguyên tắc Bất Di Bất Dịch
1. **CẤM font < 11px:** Kể cả nhãn phụ hay chú thích nhỏ nhất cũng không được đặt cỡ chữ dưới 11px.
2. **Hệ thống 3 cấp màu tương phản chuẩn:**
   * Cấp 1 (Chính): `#0f172a` hoặc `#111827` (Đen than đậm) cho tiêu đề, tên học sinh, số tiền.
   * Cấp 2 (Phụ): `#64748b` (Xám đá) cho nhãn mô tả, ngày giờ, số điện thoại.
   * Cấp 3 (Hành động & Điểm nhấn): `#2563eb` (Xanh thương hiệu), `#059669` (Xanh lá thực thu), `#d97706` (Cam nợ phí).
3. **Khoảng cách dòng (Line-height) tối thiểu 1.4 – 1.5:** Đảm bảo tiếng Việt có đủ không gian hiển thị các dấu thanh không bị đè dính lên dòng trên.
