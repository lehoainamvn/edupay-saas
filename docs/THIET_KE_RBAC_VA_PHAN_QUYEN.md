# THIẾT KẾ HỆ THỐNG RBAC & PHÂN QUYỀN TENANT — EDUPAY SAAS

> **Vấn đề cần giải quyết:**
> Hệ thống SaaS cho thuê theo mô hình Multi-Tenant cần đảm bảo:
> 1. Mỗi Tenant (Giáo viên / Trung tâm) **cô lập hoàn toàn** với nhau.
> 2. Mỗi Tenant có thể có **nhiều người dùng** (chủ + giáo viên phụ + trợ giảng).
> 3. Mỗi Tenant có thể có **cấu hình riêng** (tên thương hiệu, màu sắc, cú pháp CK...).
> 4. **Mỗi gói dịch vụ** (Trial / Monthly / Annual) **mở tính năng khác nhau**.
> 5. Trong từng Tenant, mỗi người dùng có **quyền thao tác khác nhau** (OWNER / TEACHER / VIEWER).
> 6. **Sidebar menu được ẩn/hiện tự động** dựa theo quyền của từng người dùng.

---

## PHẦN 1: SƠ ĐỒ KIẾN TRÚC TỔNG QUAN

```
┌─────────────────────────────────────────────────────────────────────┐
│                         SUPER ADMIN (Bạn)                            │
│  Quản lý tất cả Tenant, gói dịch vụ, kích hoạt/gia hạn            │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  SUBSCRIPTION PLANS & FEATURES                       │
│  TRIAL (2 lớp, 30 HS)  │  MONTHLY (không giới hạn)  │  ANNUAL    │
│  Định nghĩa tính năng gì được dùng theo từng gói                    │
└───────────────────────────┬─────────────────────────────────────────┘
                            │  tenant.plan_id
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       TENANT (Giáo viên / Trung tâm)                │
│                                                                       │
│  ┌──────────────┐    ┌───────────────────────────────────┐          │
│  │ tenant_configs│    │           users                   │          │
│  │ (Cấu hình    │    │  OWNER      TEACHER      VIEWER   │          │
│  │ riêng Tenant)│    │  (1 người)  (nhiều)      (nhiều)  │          │
│  └──────────────┘    └──────────────┬────────────────────┘          │
│                                     │  user_roles                    │
│                                     ▼                                │
│                            ┌────────────────┐                       │
│                            │     roles      │                       │
│                            │ OWNER/TEACHER/ │                       │
│                            │    VIEWER      │                       │
│                            └───────┬────────┘                       │
│                                    │  role_permissions               │
│                                    ▼                                 │
│                            ┌───────────────┐                        │
│                            │  permissions  │                        │
│                            │ class:create  │◄──── menu_permissions  │
│                            │ invoice:read  │                        │
│                            │ report:export │      ┌────────┐        │
│                            └───────────────┘      │ menus  │        │
│                                                   │ ẩn/hiện│        │
│                                                   └────────┘        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## PHẦN 2: CHI TIẾT 6 NHÓM BẢNG CSDL

### NHÓM 1 — Gói dịch vụ & Feature Flags

#### `subscription_plans`
Định nghĩa các gói dịch vụ (TRIAL, MONTHLY, ANNUAL).

| Cột | Kiểu | Mô tả |
|:----|:-----|:------|
| `id` | VARCHAR(50) PK | Mã gói: `TRIAL`, `MONTHLY`, `ANNUAL` |
| `name` | VARCHAR | Tên hiển thị |
| `price` | NUMERIC | Giá (VNĐ) |
| `duration_days` | INT | Thời hạn gói (ngày) |
| `is_active` | BOOLEAN | Đang bán / Đã ngưng |

#### `features`
Kho tính năng của toàn bộ hệ thống. Thêm tính năng mới → chỉ cần thêm 1 dòng vào đây.

| Mã Feature | Tên | Nhóm |
|:-----------|:----|:-----|
| `ATTENDANCE` | Điểm danh 1 chạm | CORE |
| `BILLING_MANUAL` | Tính học phí thủ công | CORE |
| `VIETQR_DYNAMIC` | Mã VietQR động định danh | CORE |
| `AUTO_RECONCILE` | Gạch nợ tự động (Webhook) | ADDON |
| `ZALO_ZNS` | Thông báo ZNS tự động | ADDON |
| `CUSTOM_BRANDING` | Tùy chỉnh logo thương hiệu | PREMIUM |

#### `plan_features`
**Bảng trung tâm kiểm soát tính năng theo gói.** Khi FE check tính năng, query bảng này.

| Gói | Feature | Bật? | Giới hạn lớp | Giới hạn HS |
|:----|:--------|:----:|:---:|:---:|
| TRIAL | CLASS_MANAGEMENT | ✅ | **2** | - |
| TRIAL | STUDENT_MANAGEMENT | ✅ | - | **30** |
| TRIAL | AUTO_RECONCILE | ❌ | - | - |
| MONTHLY | tất cả CORE | ✅ | - | - |
| MONTHLY | AUTO_RECONCILE | ❌ | - | - |
| ANNUAL | tất cả | ✅ | - | - |

> **Luồng kiểm tra tính năng trong Code:**
> ```java
> // Backend Spring Boot - Service Layer
> boolean hasFeature = planFeatureRepository
>     .existsByPlanIdAndFeatureIdAndIsEnabled(
>         tenant.getSubscriptionPlan(), "AUTO_RECONCILE", true);
> if (!hasFeature) throw new FeatureNotAvailableException("AUTO_RECONCILE");
> ```

---

### NHÓM 2 — Cấu hình riêng Tenant

#### `tenant_configs` (Key-Value Store per Tenant)
Mỗi tenant tự cấu hình thương hiệu và tham số riêng.

| Config Key | Giá trị mẫu | Mô tả |
|:-----------|:-----------|:------|
| `center_name` | `Trung tâm Toán Anh Nam` | Tên hiển thị trên phiếu thu |
| `transfer_prefix` | `AMATH` | Thay vì `HP` mặc định |
| `invoice_footer` | `Cảm ơn phụ huynh đã tin tưởng!` | Lời cảm ơn cuối phiếu |
| `theme_color` | `#E87722` | Màu thương hiệu |
| `logo_url` | `https://r2.cloudflare.com/...` | Logo trung tâm (Gói Premium) |

---

### NHÓM 3 — RBAC: Roles & Permissions

#### Ma trận Quyền hạn mặc định (Permission Matrix)

| Quyền | OWNER | TEACHER | VIEWER |
|:------|:-----:|:-------:|:------:|
| `class:create/update/delete` | ✅ | ❌ | ❌ |
| `class:read` | ✅ | ✅ | ✅ |
| `student:create/update/delete` | ✅ | ❌ | ❌ |
| `student:read` | ✅ | ✅ | ✅ |
| `attendance:create/update` | ✅ | ✅ | ❌ |
| `attendance:read` | ✅ | ✅ | ✅ |
| `invoice:create/update/delete` | ✅ | ❌ | ❌ |
| `invoice:reconcile` | ✅ | ❌ | ❌ |
| `invoice:export` | ✅ | ❌ | ✅ |
| `report:read` | ✅ | ❌ | ✅ |
| `settings:update` | ✅ | ❌ | ❌ |
| `user:create/update/delete` | ✅ | ❌ | ❌ |

---

### NHÓM 4 — Người dùng trong Tenant

#### `users`
Mỗi Tenant có 1 OWNER (giáo viên chính) và có thể thêm nhiều User (giáo viên phụ, trợ giảng, kế toán).

#### `user_permission_overrides`
Cho phép **ghi đè quyền đặc biệt** mà không cần tạo Role mới:
- `is_granted = TRUE`: Cấp thêm quyền ngoài Role hiện tại.
- `is_granted = FALSE`: Thu hồi 1 quyền cụ thể dù Role có.

---

### NHÓM 5 — Menu & Điều hướng

#### Luồng Angular nhận menu theo quyền

```
Login → Nhận JWT
       → JWT chứa: [tenant_id, user_id, plan_id, role_codes[]]
       → FE gọi GET /api/me/accessible-menus
       → Backend tính toán:
           1. Lấy features của plan_id hiện tại
           2. Lấy permissions của user (role + overrides)
           3. Lấy menus có feature_id và permission_id phù hợp
           4. Trả về danh sách menu được hiển thị
       → Angular render sidebar với ĐÚNG menu cho user đó
```

#### Cấu trúc Menu Sidebar (cấp 1):

| Menu | Icon | Route | Cần Feature | Cần Permission |
|:-----|:-----|:------|:------------|:---------------|
| Tổng quan | dashboard | /dashboard | REPORT_BASIC | report:read |
| Lớp học | school | /classes | CLASS_MANAGEMENT | class:read |
| Học sinh | groups | /students | STUDENT_MANAGEMENT | student:read |
| Điểm danh | how_to_reg | /attendance | ATTENDANCE | attendance:read |
| Học phí | receipt_long | /billing | BILLING_MANUAL | invoice:read |
| Báo cáo | bar_chart | /reports | REPORT_BASIC | report:read |
| Cài đặt | settings | /settings | ─ | settings:read |
| Nhân sự | manage_accounts | /admin/users | ─ | user:read |

---

## PHẦN 3: FILE SQL ĐÃ TẠO TỰ ĐỘNG SINH BẢNG

> File Flyway migration đã được tạo tại:
> `backend/src/main/resources/db/migration/V2__rbac_multi_tenant_schema.sql`
>
> Khi Spring Boot khởi động, Flyway sẽ tự động:
> 1. Phát hiện file V2 chưa chạy.
> 2. Thực thi file SQL.
> 3. Tạo đủ 11 bảng + indexes + seed dữ liệu mặc định.
> 4. Ghi vào bảng `flyway_schema_history` để không chạy lại.

---

## PHẦN 4: CHECKLIST IMPLEMENT

- [ ] **V2 SQL**: ✅ Đã tạo (tự chạy khi app khởi động)
- [ ] **Backend**: Tạo Entity/Repository cho 11 bảng mới (Sprint 1 – Tuần 1)
- [ ] **Backend**: `TenantService.hasFeature(featureName)` để check Feature Flag
- [ ] **Backend**: `SecurityService.hasPermission(permissionCode)` bổ sung vào Spring Security
- [ ] **Backend**: API `GET /api/me` trả về `user_info + roles + accessible_menus`
- [ ] **Frontend**: `AuthService` lưu và expose danh sách permissions từ JWT
- [ ] **Frontend**: `PermissionDirective` (`*appHasPermission="'invoice:create'"`) ẩn/hiện button
- [ ] **Frontend**: `FeatureGuard` trong Route Guard kiểm tra feature plan trước khi vào trang
- [ ] **Frontend**: Menu Sidebar tự render theo response từ `GET /api/me/accessible-menus`
