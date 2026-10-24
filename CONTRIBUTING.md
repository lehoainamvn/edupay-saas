# 📖 Quy Định Đóng Góp & Phát Triển (CONTRIBUTING GUIDE)

Chào mừng bạn tham gia phát triển dự án **EduPay SaaS**. Để đảm bảo chất lượng mã nguồn, tính bảo mật dữ liệu giữa các giáo viên (Multi-Tenant) và quy trình phối hợp mượt mà, tất cả thành viên trong nhóm **bắt buộc** tuân thủ các quy định dưới đây.

---

## 1. 🌿 Chiến Lược Phân Nhánh Git (Git Branching Rules)

Dự án áp dụng mô hình **Git Flow chuẩn Agile/Sprint**:

```text
main (Production - Chỉ chạy code ổn định)
  ▲
  └── release/v* (Staging / UAT test nghiệm thu)
        ▲
        └── develop (Nhánh tích hợp chính)
              ▲
              ├── feature/sprint<X>/TASK<ID>-<ten-task> (Dev làm tính năng)
              └── bugfix/<ten-loi> (Sửa bug trong sprint)

hotfix/v* (Vá lỗi khẩn cấp, tách trực tiếp từ main)
```

### ⚠️ QUY TẮC BẤT DI BẤT DỊCH:
1. **KHÔNG BAO GIỜ commit trực tiếp vào `main` hoặc `develop`**.
2. Mọi tính năng phải xuất phát từ nhánh **`develop`** mới nhất:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/sprint1/TASK05-jwt-tenant-auth
   ```
3. Sau khi hoàn thành và test kỹ ở local, đẩy nhánh lên GitHub và tạo **Pull Request (PR)** trỏ vào nhánh **`develop`**.

---

## 2. 📝 Quy Chuẩn Đặt Tên Nhánh (Branch Naming)

| Loại công việc | Cú pháp đặt tên nhánh | Ví dụ |
| :--- | :--- | :--- |
| **Tính năng mới** | `feature/sprint<N>/<Mã-Task>-<tên-ngắn>` | `feature/sprint1/TASK07-bank-config-api` |
| **Sửa lỗi trong Sprint** | `bugfix/<tên-lỗi>` | `bugfix/fix-attendance-date-timezone` |
| **Đóng gói phát hành** | `release/v<major>.<minor>.<patch>` | `release/v1.0.0-mvp` |
| **Vá lỗi khẩn cấp (Prod)** | `hotfix/v<version>-<lỗi>` | `hotfix/v1.0.1-fix-vietqr-generator` |

---

## 3. 💬 Quy Chuẩn Commit Message (Conventional Commits)

Mỗi commit message phải rõ ràng, tuân thủ định dạng chuẩn quốc tế:

```text
<type>(<scope>): <mô tả ngắn gọn bằng tiếng Anh hoặc tiếng Việt>
```

### Các Type hợp lệ:
* `feat`: Thêm tính năng mới (vd: `feat(auth): implement JWT token provider with tenant_id`)
* `fix`: Sửa lỗi (vd: `fix(billing): correct attendance count calculation formula`)
* `refactor`: Tối ưu code, không thay đổi logic (vd: `refactor(attendance): optimize batch upsert query`)
* `docs`: Cập nhật tài liệu (vd: `docs: update API endpoints for public invoice`)
* `style`: Format code, chỉnh CSS (vd: `style(ui): improve touch target size for mobile attendance`)
* `test`: Thêm unit test hoặc integration test (vd: `test(student): add test case for duplicate phone check`)
* `chore`: Cập nhật cấu hình build, dependency (vd: `chore: upgrade flyway postgres dependency`)

---

## 4. 🔒 Luật Bảo Mật Multi-Tenant (BẮT BUỘC ĐỌC)

EduPay là hệ thống cho nhiều giáo viên thuê chung (Multi-Tenant). **Lộ dữ liệu học sinh giữa 2 giáo viên là lỗi nghiêm trọng mức P0 (Block Release)!**

1. **Mọi bảng nghiệp vụ** (`classes`, `students`, `attendance`, `invoices`, `payment_settings`) đều phải kế thừa `BaseEntity` và có cột `tenant_id`.
2. **Backend:** Tuyệt đối không nhận `tenant_id` từ Body request của client gửi lên! Luôn lấy `tenant_id` từ `TenantContext.getTenantId()` (đã được giải mã từ JWT token an toàn).
3. **Mọi câu truy vấn SQL / JPA:** Bắt buộc phải có điều kiện `WHERE tenant_id = :tenantId`.
4. **Public Link Phụ huynh:** Dùng mã `public_hash` ngẫu nhiên bảo mật (UUID v4), không bao giờ dùng ID tăng dần (`1, 2, 3...`) để tránh bị cào trộm dữ liệu.

---

## 5. 🔍 Quy Trình Tạo Pull Request (PR) & Code Review

1. Trước khi tạo PR, đảm bảo code đã chạy test và build thành công:
   * Backend: `./mvnw clean test` pass 100%.
   * Frontend: `ng build` không có lỗi TypeScript.
2. Tiêu đề PR: `[TASK-ID] Mô tả ngắn gọn tính năng` (Ví dụ: `[TASK-14] Hoàn thành giao diện Điểm danh 1 chạm`).
3. Nội dung PR phải có:
   * Tóm tắt những thay đổi chính.
   * Ảnh chụp màn hình / video test thực tế (đối với Frontend).
   * Hướng dẫn test nhanh API (đối với Backend).
4. Bắt buộc có ít nhất **1 đồng đội (Peer Review) duyệt (Approve)** và **Leader Merge**.
