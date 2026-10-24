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

### 3. Luật Giao Diện Mobile-First (Mobile-First Touch Law)
* Mọi component Angular phải được thiết kế cho **màn hình cảm ứng điện thoại** trước tiên.
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
