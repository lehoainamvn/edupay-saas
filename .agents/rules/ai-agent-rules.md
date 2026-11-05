# 🤖 EDU-PAY SAAS — QUY TẮC PHÁT TRIỂN DÀNH CHO AI AGENT (AGENT RULES)

Tài liệu này là **tập luật tối cao (Ground Truth)** dành riêng cho AI Coding Agents (Antigravity, Cursor, Windsurf, GitHub Copilot) khi tự động phân tích, sinh mã hoặc sửa code trong dự án EduPay SaaS.

---

## 🛑 BỘ LUẬT BẤT KHẢ XÂM PHẠM (P0 NON-NEGOTIABLE LAWS)

### 1. Luật Bảo Mật Multi-Tenant (Multi-Tenant Isolation Law)
* **KHÔNG BAO GIỜ** nhận `tenant_id` từ Body JSON hoặc Query Param của Client request.
* **LUÔN LUÔN** lấy `tenant_id` từ `TenantContext.getTenantId()`.
* **LUÔN LUÔN** gán `tenant_id` cho mọi Entity nghiệp vụ khi lưu vào database.
* **LUÔN LUÔN** bổ sung điều kiện `tenant_id = :tenantId` trong mọi câu query tìm kiếm, cập nhật hoặc xóa dữ liệu.
* Rò rỉ dữ liệu giữa 2 tenant là **lỗi nghiêm trọng mức P0**.

### 2. Luật Kiến Trúc Phân Lớp (Layered Architecture Law)
* **Controller**: Cấm tuyệt đối viết logic nghiệp vụ, tính toán học phí hoặc gọi trực tiếp Repository. Controller chỉ nhận request, kích hoạt validation (`@Valid`), gọi Service và trả về `ResponseEntity<ApiResponse<T>>`.
* **Service**: Chứa 100% nghiệp vụ. Đánh dấu `@Transactional` cho các hàm ghi và `@Transactional(readOnly = true)` cho các hàm đọc.
* **DTO & Entity**: Cấm trả Entity JPA trực tiếp ra ngoài API. Luôn ánh xạ qua `ResponseDTO` bằng MapStruct.

### 3. Luật Giao Diện Mobile-First & Design System
* Mọi component Angular và bản vẽ Figma **BẮT BUỘC** tuân thủ tài liệu chuẩn [DESIGN_SYSTEM_GUIDELINES.md](file:///Users/buiuyenthu/Documents/LeHoaiNam/GitHub/DuAn_tienti/edupay-saas/docs/DESIGN_SYSTEM_GUIDELINES.md):
  * Phông chữ: `Inter` hoặc `Be Vietnam Pro` (số tiền dùng `tabular-nums`).
  * Bảng màu chuẩn: Nền `--canvas: #FAF8F9`, Chữ `--ink: #0F172A`, Nút `--blue: #2563EB`.
  * Màu điểm danh: Xanh (`#DCFCE7`), Vàng (`#FEF9C3`), Đỏ (`#FEE2E2`).
  * Icon: Phong cách Outline Stroke `2px` (Mũ cử nhân, Checkmark `✓`, Cross `✕`, Arrow `→`).
* Chiều cao nút bấm (Touch Target) tối thiểu **44px**, kích thước chuẩn **48px**.
* Không sử dụng Table tràn ngang trên màn hình `< 640px`. Phải hiển thị dạng Card xếp dọc.
* Mọi form phải hiển thị thông báo lỗi bằng chữ đỏ trực tiếp dưới ô nhập liệu tương ứng.
* Điểm danh phải đổi màu lập tức (Optimistic UI update), không để giao diện bị khựng chờ mạng.

### 4. Luật VietQR & Trang Tra Cứu Phụ Huynh
* Endpoint công khai `/p/:hash` dành cho phụ huynh xem hóa đơn **TUYỆT ĐỐI KHÔNG BẮT ĐĂNG NHẬP**.
* URL VietQR Napas247 phải tuân thủ chuẩn URL:
  `https://img.vietqr.io/image/{BIN}-{STK}-compact2.png?amount={AMOUNT}&addInfo={CODE}`
* Mã chuyển khoản `{CODE}` **KHÔNG DÙNG KÝ TỰ CÓ DẤU** hoặc dấu cách (chỉ dùng A-Z và 0-9).

### 5. Luật Di Trú Cơ Sở Dữ Liệu (Flyway Migration Law)
* **CẤM SỬA ĐỔI** bất kỳ file migration nào đã commit (`V1__...`, `V2__...`).
* Thay đổi bảng, thêm cột mới phải tạo file migration kế tiếp: `V3__...`, `V4__...`.

### 6. Luật Tái Sử Dụng & Chống Trùng Lặp (Reuse-First & Anti-Duplication Law — P0)
* **QUÉT VÀ KIỂM TRA TRƯỚC (Discovery First):** Trước khi tạo mới bất kỳ thành phần nào (UI Component, Layout/Thanh Navigation, Modal, Toast, Service, DTO, Entity, Helper, Enum, CSS Class, Icon), Agent **BẮT BUỘC PHẢI TỰ TRA CỨU** trong thư mục dùng chung (`frontend/src/app/shared/`, `core/`, `backend/src/.../entity/`, `dto/`, `config/`) và các nhánh tính năng (`feature/...`).
* **CẤM TẠO TRÙNG LẶP / TẠO MỚI KHI ĐÃ CÓ SẴN:**
  * Những gì hệ thống **ĐÃ THIẾT KẾ HOẶC XÂY DỰNG SẴN** (ví dụ: `NavbarComponent` `<app-navbar>`, `BaseEntity`, `TenantContext`, `ApiResponse<T>`, `TokenService`, `AuthService`,...):
    - **BẮT BUỘC tự vào lấy lại để tái sử dụng 100%**, chỉ import và truyền tham số (`@Input`, props, method calls) phù hợp ngữ cảnh mới.
    - **TUYỆT ĐỐI CẤM** tự ý tạo ra cái mới trùng lặp, không code lại từ đầu, không copy-paste tạo ra các component song song gây phân mảnh mã nguồn, lệch style giao diện hoặc sai lệch logic.
* **NGUYÊN TẮC "SINGLE SOURCE OF TRUTH":** Mọi nâng cấp, tinh chỉnh cho một thành phần dùng chung phải được sửa trực tiếp tại component gốc trong `shared/` hoặc `core/` để toàn bộ các trang khác cùng được hưởng lợi đồng bộ. Tuyệt đối không tạo bản sao riêng lẻ trong từng feature folder.

### 7. Luật Tránh Dùng Icon / Emoji Tùy Tiện Do AI Bịa (Anti-AI Cliché Icons & Clean Typography Law — P0)
* **CẤM CHÈN EMOJI / ICON LẠ TỰ BỊA:** Tuyệt đối **CẤM** chèn emoji hoặc icon ngẫu hứng do AI tự sinh (ví dụ: `👋`, `📱`, `⚡`, `🧮`, `🎓`, `💸`, `🔥`...) vào tiêu đề H1/H2, button label, kicker badge, và các mục menu của thanh điều hướng.
* **CLEAN TYPOGRAPHY FIRST:** Giao diện quản lý giảng dạy & tài chính B2B cho giáo viên cần sự thanh lịch, chuẩn mực, tối giản, chuyên nghiệp. Nội dung văn bản phải rõ ràng, gãy gọn, không màu mè.
* **BỘ ICON VECTOR CHUẨN:** Chỉ sử dụng icon vector tối giản đã được chuẩn hóa (ví dụ: icon mũi tên `→`, checkmark `✓`, xóa `✕`, hoặc các icon có nét vẽ stroke `1.5px` - `1.8px` đồng bộ). Tuyệt đối không tự vẽ hoặc copy các icon SVG kỳ dị từ các công cụ AI khác.
* **CHỐT THANH NAVIGATION (`NavbarComponent`):** Thanh điều hướng sử dụng nhãn chữ thuần túy (Text-only tabs) cho các mục: "Tổng quan", "Lớp học", "Học sinh", "Điểm danh 1 chạm", "Học phí VietQR". Không chèn thêm icon thừa vào trước chữ.

### 8. Luật Thống Nhất Routing Tập Trung Tại Duy Nhất 1 File (Single Source of Truth for Routing Law — P0)
* **DUY NHẤT 1 FILE ROUTING:** Toàn bộ cấu hình định tuyến (Routing) của ứng dụng Frontend **BẮT BUỘC** tập trung 100% tại duy nhất một file: `frontend/src/app/app.routes.ts`.
* **CẤM PHÂN MẢNH ROUTE:** Tuyệt đối **CẤM** tạo các file routing con (e.g. `classes.routes.ts`, `dashboard.routes.ts`, `feature-routing.module.ts`) rải rác trong từng thư mục feature. Mọi route mới, route con (children), route redirect hay route alias đều phải được khai báo trực tiếp trong `app.routes.ts`.
* **QUẢN LÝ BẢO VỆ ROUTE TẬP TRUNG:** Các guard như `authGuard` phải được gắn và quản lý tường minh ngay trên từng route trong file này để đảm bảo bức tranh toàn cảnh về bảo mật và điều hướng.

### 9. Luật Kết Nối API Thật & Thiết Kế Trạng Thái Rỗng (Real API & Empty State First Law — P0)
* **KHÔNG DÙNG DỮ LIỆU GIẢ CỨNG TRONG CODE (No Hardcoded Mock Data):** Mọi màn hình ứng dụng sau khi thiết kế xong khung giao diện **BẮT BUỘC** phải kết nối API gọi xuống cơ sở dữ liệu thật để lấy và hiển thị dữ liệu động của Tenant.
* **THIẾT KẾ EMPTY STATE CHỈN CHU:** Khi cơ sở dữ liệu chưa có bản ghi (ví dụ: giáo viên mới đăng ký chưa có lớp học, hôm nay chưa có ca dạy, chưa phát sinh giao dịch VietQR, không có học sinh nợ), giao diện **BẮT BUỘC** phải hiển thị trạng thái rỗng (Empty State) sạch sẽ, lịch sự với thông báo hướng dẫn rõ ràng (ví dụ: *"Chưa có ca dạy nào hôm nay"*, *"Chưa có giao dịch VietQR nào phát sinh"*) kèm nút hành động thích hợp (như *"+ Tạo lớp học mới"*), tuyệt đối không để màn hình lỗi hay hiển thị dữ liệu giả của người khác.

### 10. Luật Thống Nhất Thông Báo Toàn Hệ Thống (Single Source of Truth for Toast & Notifications Law — P0)
* **DUY NHẤT 1 TOAST SERVICE & COMPONENT TOÀN ỨNG DỤNG:** Toàn bộ thông báo người dùng (Thành công, Thất bại, Cảnh báo, Thông tin) trên toàn bộ ứng dụng Frontend **BẮT BUỘC** sử dụng tập trung qua `ToastService` (`frontend/src/app/core/services/toast.service.ts`) và hiển thị qua component dùng chung `ToastComponent` (`<app-toast>`) được gắn duy nhất tại gốc ứng dụng (`AppComponent`).
* **CẤM TỰ TẠO THÔNG BÁO CỤC BỘ (No Local Toasts):**
  * **TUYỆT ĐỐI CẤM** tự khai báo signal cục bộ như `toastMessage`, `errorMessage`, `toastType` và tự viết HTML `@if (toastMessage()) { <div class="toast">...</div> }` hoặc định nghĩa CSS `.toast` trong từng feature component riêng lẻ (`billing`, `settings`, `attendance`, `classes`, `students`, `dashboard`, `public-invoice`,...).
  * Mọi hành động thông báo phản hồi người dùng **BẮT BUỘC** gọi các hàm chuẩn hóa của `ToastService`:
    - **Thành công (Success):** `this.toastService.success(message, title?, durationMs?)` (Màu xanh lục `#10b981`, icon `✓`, mặc định 3.5s)
    - **Thất bại / Lỗi (Error):** `this.toastService.error(message, title?, durationMs?)` (Màu đỏ `#ef4444`, icon `✕`, mặc định 4.5s)
    - **Cảnh báo (Warning):** `this.toastService.warning(message, title?, durationMs?)` (Màu vàng cam `#f59e0b`, icon `⚠`, mặc định 4.0s)
    - **Thông tin (Info):** `this.toastService.info(message, title?, durationMs?)` (Màu xanh dương `#3b82f6`, icon `ℹ`, mặc định 3.5s)
* **QUY CHUẨN TRẢI NGHIỆM & GIAO DIỆN (UI/UX Standards):**
  * Vị trí hiển thị: Cố định góc trên bên phải màn hình (`top: 24px; right: 24px; z-index: 999999`), tự co giãn full chiều ngang trên mobile (`left: 16px; right: 16px`).
  * Animation: Trượt từ trên xuống `slideInDown`, hỗ trợ nút bấm tắt thủ công `✕` với touch target thân thiện di động.
  * Tối đa 4 thông báo cùng lúc để không làm che khuất nội dung làm việc của giáo viên.


### 11. Luật Thống Nhất Cỡ Chữ 16px & Khung Bố Cục Chuẩn 1280px Toàn Hệ Thống (Unified 16px Typography & 1280px Layout Frame Law — P0)
* **CHUẨN FONT-SIZE 16PX GỐC TOÀN HỆ THỐNG (Base 16px Typography):**
  * Toàn bộ ứng dụng lấy cỡ chữ **16px** làm chuẩn mực thiết kế cơ sở (`html { font-size: 16px; }`, `body { font-size: 16px; line-height: 1.5; }`).
  * Mọi ô nhập liệu (`input`, `select`, `textarea`), nút bấm (`button`) chính và nội dung đọc chính **BẮT BUỘC** đạt kích thước tối thiểu **15px - 16px** (chống hiện tượng tự động phóng to khó chịu trên iOS Safari di động, đồng thời đảm bảo giáo viên đọc thông tin học sinh và số tiền rõ ràng, không mỏi mắt).
  * **CẤM DÙNG FONT CHỮ LI TI:** Tuyệt đối không dùng font chữ < 13px cho nội dung đọc chính, danh sách học sinh, bảng tính học phí hoặc nhãn form. Chỉ cho phép dùng 11px - 12px duy nhất cho các tag kicker/badge in hoa phụ.
  * Phân cấp cỡ chữ chuẩn:
    - **Tiêu đề trang (H1):** `font-size: 26px - 28px; font-weight: 700 - 800;`
    - **Tiêu đề mục / Thẻ (H2, H3, Modal Title):** `font-size: 18px - 20px; font-weight: 700;`
    - **Đoạn mô tả dưới tiêu đề (Subtitle/Desc):** `font-size: 15px - 16px; line-height: 1.5; color: #64748b;`
    - **Nội dung bảng, thẻ học sinh, ô nhập liệu:** `font-size: 15px - 16px;`
    - **Nhãn phụ / Badge kicker:** `font-size: 11px - 12px; font-weight: 700; text-transform: uppercase;`
* **THỐNG NHẤT KHUNG BỐ CỤC 1280PX CHO TẤT CẢ CÁC TRANG (No Layout Jumping):**
  * Toàn bộ các trang nghiệp vụ (`dashboard`, `classes`, `students`, `attendance`, `billing`, `settings`) **BẮT BUỘC DÙNG CHUNG DUY NHẤT 1 BỘ THÔNG SỐ KHUNG BỐ CỤC**:
    - **Chiều rộng tối đa (Max Width):** Đúng chuẩn **`1280px`** căn giữa (`margin: 0 auto;`).
    - **Khoảng đệm đồng bộ (Padding):** `padding: 28px 24px 96px;` trên desktop/laptop; `padding: 24px 20px 88px;` trên tablet; và `padding: 20px 16px 80px;` trên mobile.
  * **CẤM LỆCH KHUNG TRANG:** Tuyệt đối cấm mỗi trang tự đặt `max-width` cục bộ khác nhau (ví dụ: `1120px`, `1200px`, `1240px`) hoặc đặt khoảng đệm lệch nhau, gây hiện tượng co giật chiều ngang hoặc nhảy vị trí khi giáo viên bấm chuyển giữa các tab trên thanh điều hướng.

---

## 🛠️ DANH SÁCH SKILLS CỦA DỰ ÁN

Khi thực hiện task, Agent hãy nạp và đọc kỹ hướng dẫn trong các Skill tương ứng tại `.agents/skills/`:

| Tên Skill | Vị trí thư mục | Mục đích sử dụng |
| :--- | :--- | :--- |
| **`edupay-workflow`** | `.agents/skills/edupay-workflow/` | Quy trình Git, phân nhánh `develop`/`feature`, convention chung |
| **`angular-mobile-ui`** | `.agents/skills/angular-mobile-ui/` | Thiết kế Standalone Component, Signals, Mobile Touch UX, Form Validation |
| **`spring-boot-multitenant`** | `.agents/skills/spring-boot-multitenant/` | Xây dựng API Spring Boot 3, Entity BaseEntity, Repository lọc `tenant_id`, DTO |
| **`vietqr-payment-reconciliation`** | `.agents/skills/vietqr-payment-reconciliation/` | Sinh link VietQR động, Webhook gạch nợ tự động, xử lý Idempotency |
| **`edupay-testing-quality`** | `.agents/skills/edupay-testing-quality/` | Viết Unit Test Mockito, Spring Slice Test, Angular Component Test |
