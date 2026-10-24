# 🅰️ EduPay Frontend — Angular 17+ Mobile-First SaaS

[![Angular](https://img.shields.io/badge/Angular-17.3-red.svg)](https://angular.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.4-blue.svg)](https://www.typescriptlang.org/)
[![UI Style](https://img.shields.io/badge/Design-Mobile--First%20Responsive-emerald.svg)](#)
[![Chart.js](https://img.shields.io/badge/Charts-Chart.js%204.4-yellow.svg)](https://www.chartjs.org/)

> **EduPay Frontend** là giao diện người dùng của nền tảng EduPay SaaS, thiết kế theo triết lý **Mobile-First** tối ưu 100% cho giáo viên thao tác điểm danh và quản lý học phí trên điện thoại thông minh, đồng thời cung cấp cổng tra cứu hóa đơn không cần đăng nhập dành riêng cho phụ huynh.
> Tài liệu này được biên soạn cho cả **Lập trình viên (Developer)** và **AI Coding Agent** (Antigravity, Cursor, Copilot) nhằm hiểu sâu luồng giao diện và triển khai chuẩn mực.

---

## 📌 Mục Lục (Table of Contents)
1. [Triết Lý Thiết Kế Mobile-First](#1-triết-lý-thiết-kế-mobile-first)
2. [Cấu Trúc Thư Mục Chuẩn](#2-cấu-trúc-thư-mục-chuẩn)
3. [Cấu Hình Môi Trường (Dev vs Prod)](#3-cấu-hình-môi-trường-dev-vs-prod)
4. [Hướng Dẫn Khởi Chạy Local (Quickstart)](#4-hướng-dẫn-khởi-chạy-local-quickstart)
5. [Cổng Tra Cứu Phụ Huynh (Public Portal)](#5-cổng-tra-cứu-phụ-huynh-public-portal)
6. [Quản Lý State & HTTP Interceptor](#6-quản-lý-state--http-interceptor)
7. [Hệ Thống Phân Quyền Menu & RBAC UI](#7-hệ-thống-phân-quyền-menu--rbac-ui)
8. [🤖 Cẩm Nang Dành Riêng Cho AI Agent](#8--cẩm-nang-dành-riêng-cho-ai-agent)

---

## 1. Triết Lý Thiết Kế Mobile-First

90% người dùng của EduPay là giáo viên, gia sư mở ứng dụng trên điện thoại di động ngay tại lớp học để điểm danh hoặc gửi link học phí qua ứng dụng Zalo.

### Quy Chuẩn Bắt Buộc:
- **Kích thước nút bấm (Touch Target):** Tối thiểu **44px × 44px**, kích thước chuẩn khuyến nghị là **48px**.
- **Mã màu trạng thái điểm danh trực quan:**
  - 🟢 **Có mặt (Present):** Xanh lục (`#22c55e` / `bg-emerald-500`)
  - 🟡 **Nghỉ có phép (Excused):** Vàng cam (`#eab308` / `bg-amber-500`)
  - 🔴 **Nghỉ không phép (Absent):** Đỏ tươi (`#ef4444` / `bg-rose-500`)
- **Tốc độ phản hồi:** Đổi trạng thái ngay lập tức trên UI (Optimistic UI update), sau đó đồng bộ ngầm với Backend.

---

## 2. Cấu Trúc Thư Mục Chuẩn

Frontend tuân theo kiến trúc module hóa tính năng (Feature-based structure) với **Standalone Components**:

```text
frontend/
├── package.json                           # Dependencies (Angular 17, RxJS, Chart.js)
├── angular.json                           # Build configurations & asset pathways
└── src/
    ├── index.html                         # Root HTML (viewport meta tối ưu mobile cảm ứng)
    ├── styles.css                         # CSS toàn cục, font chữ, biến màu sắc
    ├── environments/                      # Cấu hình môi trường
    │   ├── environment.ts                 # DEV: trỏ về http://localhost:8080/api
    │   └── environment.prod.ts            # PROD: trỏ về domain production (VD: api.edupay.vn)
    └── app/
        ├── app.routes.ts                  # Routing chính (Lazy-loaded feature routes)
        │
        ├── core/                          # Dùng toàn ứng dụng (Singleton services)
        │   ├── guards/                    # AuthGuard, RoleGuard, TenantGuard
        │   ├── interceptors/              # AuthInterceptor (Gắn JWT Bearer & bắt 401)
        │   └── services/                  # ApiService, AuthService, TokenService, ToastService
        │
        ├── shared/                        # Tái sử dụng ở nhiều màn hình
        │   ├── components/                # LoadingSpinner, ConfirmModal, BottomSheet, StatCard
        │   ├── pipes/                     # CurrencyVndPipe, VietDateFormatPipe
        │   └── models/                    # Interface dùng chung (ApiResponse, User, Tenant, Class...)
        │
        └── features/                      # Các module nghiệp vụ chính (Lazy loading)
            ├── auth/                      # Login, Register, Forgot Password
            ├── dashboard/                 # Thống kê số buổi dạy, số tiền thu, biểu đồ nợ
            ├── classes/                   # Quản lý lớp học, lịch học trong tuần
            ├── students/                  # Danh sách học sinh, thêm/sửa, import Excel
            ├── attendance/                # Màn hình điểm danh 1 chạm (Mobile-First)
            ├── billing/                   # Tính học phí, phát hành phiếu thu, gạch nợ
            └── public-invoice/            # Trang phụ huynh tra cứu hóa đơn & quét VietQR (/p/:hash)
```

---

## 3. Cấu Hình Môi Trường (Dev vs Prod)

Các thông số API endpoint và tính năng được cấu hình trong thư mục `src/environments/`:

### 1. `environment.ts` (Môi trường Development)
Áp dụng khi chạy lệnh `npm start` hoặc `ng serve`:
```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
  vietQrApiUrl: 'https://img.vietqr.io/image',
  enableDebugTools: true
};
```

### 2. `environment.prod.ts` (Môi trường Production)
Áp dụng khi chạy lệnh `npm run build` hoặc build Docker:
```typescript
export const environment = {
  production: true,
  apiBaseUrl: 'https://api.edupay.vn/api',
  vietQrApiUrl: 'https://img.vietqr.io/image',
  enableDebugTools: false
};
```

---

## 4. Hướng Dẫn Khởi Chạy Local (Quickstart)

### Yêu Cầu Tiên Quyết
- **Node.js:** Phiên bản 18 LTS hoặc 20 LTS (`node -v`)
- **NPM:** Phiên bản 9 trở lên (`npm -v`)
- **Angular CLI:** `npm install -g @angular/cli@17` (tùy chọn)

### Bước 1: Cài đặt Dependencies
Từ thư mục `frontend/`:
```bash
cd frontend
npm install
```

### Bước 2: Khởi động Dev Server
```bash
npm start
# hoặc
ng serve --open
```

Mở trình duyệt tại: `http://localhost:4200`  
Giao diện sẽ tự động reload khi bạn thay đổi code.

### Bước 3: Build Bản Đóng Gói (Production)
```bash
npm run build
```
Bản build tĩnh tối ưu hóa sẽ được lưu tại thư mục `dist/edupay-frontend`.

---

## 5. Cổng Tra Cứu Phụ Huynh (Public Portal)

Route đặc biệt: `/p/:hash` (VD: `http://localhost:4200/p/INV-9A-001-A9F2`)

### Đặc Điểm Nghiệp Vụ:
- **Hoàn toàn không yêu cầu đăng nhập:** Phụ huynh chỉ cần nhấp vào link gửi qua Zalo là xem được ngay.
- **Tốc độ tải siêu tốc:** Tải dưới 1.5 giây trên mạng di động 4G.
- **Tiện ích 1 chạm tích hợp:**
  - 📋 **Sao chép Số Tài Khoản:** Copy số tài khoản giáo viên vào Clipboard + hiện toast thông báo.
  - 📋 **Sao chép Nội Dung Chuyển Khoản:** Đảm bảo chính xác mã đối soát.
  - 📥 **Tải Ảnh QR:** Tải trực tiếp ảnh QR Napas247 về album ảnh để quét qua app ngân hàng.
  - 📱 **Mở App Ngân Hàng:** Deeplink chuyển hướng phụ huynh vào ứng dụng ngân hàng di động.

---

## 6. Quản Lý State & HTTP Interceptor

### 1. `AuthInterceptor` (Tự động gắn JWT)
Tất cả các HTTP request gửi đến `apiBaseUrl` (ngoại trừ các endpoint public) đều tự động được đính kèm:
```http
Authorization: Bearer <access_token>
```
Nếu Backend phản hồi mã lỗi `401 Unauthorized`, Interceptor sẽ tự động xóa token lưu trữ và điều hướng người dùng về trang `/auth/login`.

### 2. State & Data Flow
- Sử dụng **Angular Signals** và **RxJS `takeUntilDestroyed()`** để quản lý trạng thái và tránh rò rỉ bộ nhớ (Memory Leak).
- Mọi form tương tác người dùng đều sử dụng **Reactive Forms** (`FormGroup`, `FormControl`, `Validators`).

---

## 7. Hệ Thống Phân Quyền Menu & RBAC UI

Theo thiết kế RBAC V2 của hệ thống:
- Sau khi đăng nhập, hệ thống tải danh sách quyền (`permissions`) và menu được phép (`menus`) của người dùng theo gói dịch vụ (Plan).
- `MenuService` tự động lọc các mục điều hướng (Sidebar / Bottom Navigation) phù hợp với quyền hạn của người dùng (`OWNER`, `TEACHER`, `VIEWER`).

---

## 8. 🤖 Cẩm Nang Dành Riêng Cho AI Agent

Nếu bạn là một AI Agent (Antigravity, Cursor, Copilot, Windsurf):

1. **Chuẩn Standalone Components:**
   - Luôn sử dụng cú pháp Angular 17+: `standalone: true`, khai báo các dependency trực tiếp trong mảng `imports: [CommonModule, ReactiveFormsModule, ...]`.
2. **Tuân Thủ Mobile-First Trong CSS:**
   - Không thiết kế giao diện dạng bảng ngang (table) quá nhiều cột trên màn hình điện thoại. Trên màn hình nhỏ (`< 640px`), chuyển danh sách học sinh thành dạng thẻ (Card/List Item) có nút bấm to.
   - Luôn đặt chiều cao nút bấm tối thiểu `h-11` (44px) hoặc `h-12` (48px).
3. **Quản Lý Bất Đồng Bộ (Async):**
   - Không subscribe trực tiếp trong component mà không hủy (`unsubscribe`). Ưu tiên sử dụng `toSignal()` hoặc `async` pipe trên template HTML.
4. **Không Tạo Dữ Liệu Ảo Cố Định (No Hardcoded Data):**
   - Luôn định nghĩa Interface TypeScript rõ ràng trong thư mục `src/app/shared/models/` trước khi viết mock hoặc gọi service.
