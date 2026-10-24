# ☕ EduPay Backend — Spring Boot 3 & Multi-Tenant Architecture

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Migration-Flyway-red.svg)](https://flywaydb.org/)
[![Architecture](https://img.shields.io/badge/SaaS-Multi--Tenant%20Shared--DB-purple.svg)](#)

> **EduPay Backend** cung cấp hệ thống RESTful API cho nền tảng SaaS quản lý học sinh, điểm danh và tự động gạch nợ học phí qua mã VietQR Napas247 động.
> Tài liệu này được biên soạn cho cả **Lập trình viên (Developer)** và **AI Coding Agent** (Antigravity, Cursor, Copilot) nhằm hiểu sâu kiến trúc và phát triển tính năng chuẩn xác.

---

## 📌 Mục Lục (Table of Contents)
1. [Kiến Trúc & Multi-Tenant SaaS](#1-kiến-trúc--multi-tenant-saas)
2. [Cấu Trúc Thư Mục Chuẩn](#2-cấu-trúc-thư-mục-chuẩn)
3. [Môi Trường Cấu Hình (Dev vs Prod)](#3-môi-trường-cấu-hình-dev-vs-prod)
4. [Hướng Dẫn Khởi Chạy Local (Quickstart)](#4-hướng-dẫn-khởi-chạy-local-quickstart)
5. [Cơ Sở Dữ Liệu & Flyway Migrations](#5-cơ-sở-dữ-liệu--flyway-migrations)
6. [Quy Chuẩn Code & Layered Architecture](#6-quy-chuẩn-code--layered-architecture)
7. [Bảo Mật, JWT & Tenant Context Filter](#7-bảo-mật-jwt--tenant-context-filter)
8. [Tích Hợp VietQR & Webhook Đối Soát](#8-tích-hợp-vietqr--webhook-đối-soát)
9. [🤖 Cẩm Nang Dành Riêng Cho AI Agent](#9--cẩm-nang-dành-riêng-cho-ai-agent)

---

## 1. Kiến Trúc & Multi-Tenant SaaS

EduPay sử dụng mô hình **Single Database, Shared Schema with Discriminator Column (`tenant_id`)**:
- Mỗi giáo viên hoặc trung tâm khi đăng ký là một **Tenant** riêng biệt (`UUID tenant_id`).
- Mọi bảng nghiệp vụ (`classes`, `students`, `attendance_sessions`, `invoices`, ...) đều kế thừa [`BaseEntity`](file:///Users/buiuyenthu/Documents/LeHoaiNam/GitHub/DuAn_tienti/backend/src/main/java/com/edupay/entity/BaseEntity.java) và bắt buộc có cột `tenant_id`.
- Danh tính Tenant được trích xuất từ JWT Token của người dùng qua [`TenantContext`](file:///Users/buiuyenthu/Documents/LeHoaiNam/GitHub/DuAn_tienti/backend/src/main/java/com/edupay/config/TenantContext.java) (`ThreadLocal<UUID>`), đảm bảo không có rò rỉ dữ liệu chéo giữa các giáo viên.

---

## 2. Cấu Trúc Thư Mục Chuẩn

```text
backend/
├── pom.xml                                # Maven build & dependencies (Spring Boot 3.3.4, Java 17)
└── src/
    └── main/
        ├── java/com/edupay/
        │   ├── EdupayApplication.java     # Main Entry Point
        │   ├── config/                    # SecurityConfig, WebMvcConfig, TenantContext
        │   ├── security/                  # JwtTokenProvider, JwtAuthFilter, UserDetailsServiceImpl
        │   ├── controller/                # REST Controllers (Auth, Class, Student, Attendance, Invoice...)
        │   ├── service/                   # Business Logic Layer & Interfaces
        │   │   └── impl/                  # Service Implementations (@Transactional)
        │   ├── repository/                # Spring Data JPA Repositories (findByTenantId...)
        │   ├── entity/                    # JPA Entities kế thừa BaseEntity
        │   ├── dto/                       # Request DTO, Response DTO, MapStruct Mappers
        │   │   ├── request/
        │   │   └── response/
        │   └── exception/                 # GlobalExceptionHandler, Custom Exceptions
        └── resources/
            ├── application.yml            # Base configuration chung
            ├── application-dev.yml        # Config môi trường DEV (show-sql, DEBUG log, local DB)
            ├── application-prod.yml       # Config môi trường PROD (bảo mật, secrets từ ENV)
            └── db/migration/              # Flyway SQL migrations
                ├── V1__init_schema.sql    # Schema khởi tạo (Tenant, Class, Student, Attendance, Invoice)
                └── V2__rbac_multi_tenant_schema.sql # RBAC, Feature Flags, Tenant Config & Menus
```

---

## 3. Môi Trường Cấu Hình (Dev vs Prod)

Backend hỗ trợ 2 profile tách bạch thông qua Spring Profiles:

| Tham số | Profile `dev` (Mặc định) | Profile `prod` |
| :--- | :--- | :--- |
| **File cấu hình** | [`application-dev.yml`](file:///Users/buiuyenthu/Documents/LeHoaiNam/GitHub/DuAn_tienti/backend/src/main/resources/application-dev.yml) | [`application-prod.yml`](file:///Users/buiuyenthu/Documents/LeHoaiNam/GitHub/DuAn_tienti/backend/src/main/resources/application-prod.yml) |
| **File biến môi trường** | `.env.dev` (local) | `.env.prod.example` → biến môi trường server |
| **Database Host** | `localhost:5432/edupay_dev_db` | Đọc từ `${DB_HOST}`, `${DB_NAME}` (Supabase/VPS) |
| **SQL Logging** | `show-sql: true`, log `org.hibernate.SQL: DEBUG` | `show-sql: false`, log level `INFO`/`WARN` |
| **Hikari Connection Pool** | Min 2 / Max 5 | Min 5 / Max 20 |
| **JWT Secret** | Khóa test mặc định tích hợp sẵn | Bắt buộc truyền qua `${JWT_SECRET}` (>= 256-bit) |

### Kích hoạt Profile:
- **Cách 1 (Biến môi trường):**
  ```bash
  export SPRING_PROFILES_ACTIVE=dev
  ```
- **Cách 2 (Maven CLI):**
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```

---

## 4. Hướng Dẫn Khởi Chạy Local (Quickstart)

### Yêu Cầu Tiên Quyết
- **JDK 17** trở lên (`java -version`)
- **Maven 3.8+** (hoặc dùng `./mvnw`)
- **Docker & Docker Compose** (để khởi chạy nhanh PostgreSQL)

### Bước 1: Khởi động Database PostgreSQL
Từ thư mục gốc dự án:
```bash
# Khởi động PostgreSQL 16 và pgAdmin cho môi trường Dev
docker compose -f docker/docker-compose.dev.yml up -d
```
> Thông tin DB Dev: `localhost:5432`, DB Name: `edupay_dev_db`, User: `postgres`, Pass: `postgrespassword`  
> pgAdmin GUI: `http://localhost:5050` (Tài khoản: `admin@edupay.vn` / `admin123`)

### Bước 2: Chạy Backend
Di chuyển vào thư mục `backend/`:
```bash
cd backend
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
```

Khi console hiển thị:
```text
Started EdupayApplication in X.XXX seconds (process running for X.XXX)
```
API đã sẵn sàng tại: `http://localhost:8080/api`

---

## 5. Cơ Sở Dữ Liệu & Flyway Migrations

Flyway tự động chạy các script SQL khi ứng dụng khởi động.

### Quy Tắc Đặt Tên File Migration:
- Đường dẫn: `backend/src/main/resources/db/migration/`
- Định dạng: `V{Phiên_Bản}__{Tên_Mô_Tả_Ngắn_Gọn}.sql` (chú ý **2 dấu gạch dưới `__`**)
- Ví dụ:
  - `V1__init_schema.sql`: Khởi tạo bảng cốt lõi (Tenants, Classes, Students, Enrollments, Invoices...)
  - `V2__rbac_multi_tenant_schema.sql`: Bổ sung RBAC, Subscription Plans, Feature Flags, Tenant Config, Menus
  - `V3__add_telegram_notification.sql`: Tính năng mới tiếp theo

> ⚠️ **LƯU Ý NGHIÊM NGẶT:** Tuyệt đối **KHÔNG SỬA ĐỔI** file migration đã commit vào `develop` hoặc `main`. Bất kỳ thay đổi schema nào phải tạo một file migration mới (`V3__...`, `V4__...`).

---

## 6. Quy Chuẩn Code & Layered Architecture

Mọi tính năng Backend đều phải tuân thủ nghiêm ngặt 4 tầng:

```
[HTTP Request]
      ↓
[Controller]  ← Chỉ validate (@Valid), điều phối, trả về ResponseEntity<ApiResponse<T>>
      ↓
[Service]     ← 100% Logic nghiệp vụ, @Transactional, TenantContext kiểm tra quyền
      ↓
[Repository]  ← Spring Data JPA, mọi query lọc theo tenant_id
      ↓
[Database]
```

### 1. Luôn kế thừa `BaseEntity`
```java
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### 2. Định dạng phản hồi chuẩn (`ApiResponse<T>`)
Mọi API trả về bắt buộc gói trong `ApiResponse`:
```json
{
  "success": true,
  "message": "Tạo phiếu thu thành công",
  "data": { ... },
  "timestamp": "2026-10-24T12:00:00Z"
}
```
Khi có lỗi:
```json
{
  "success": false,
  "errorCode": "STUDENT_NOT_FOUND",
  "message": "Không tìm thấy học sinh trong lớp",
  "errors": [],
  "timestamp": "2026-10-24T12:00:00Z"
}
```

### 3. Tách biệt DTO và Entity
- Dùng `RequestDTO` cho dữ liệu đầu vào (kèm `@NotBlank`, `@Positive`, `@Pattern`).
- Dùng `ResponseDTO` cho dữ liệu đầu ra. **Không bao giờ serialize trực tiếp JPA Entity ra JSON.**
- Dùng **MapStruct** để mapping giữa Entity và DTO.

---

## 7. Bảo Mật, JWT & Tenant Context Filter

1. **Public Endpoints (Không cần Auth):**
   - `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh-token`
   - `/api/public/p/**` (Cổng tra cứu học phí và quét VietQR dành cho Phụ huynh)
   - `/api/webhooks/**` (Webhook xác nhận biến động số dư ngân hàng)
2. **Authenticated Endpoints:**
   - Yêu cầu Header: `Authorization: Bearer <JWT_TOKEN>`
   - `JwtAuthFilter` giải mã token, lấy `userId`, `tenantId`, `roles` và nạp vào:
     ```java
     TenantContext.setTenantId(tenantId);
     ```
   - Sau khi request kết thúc, `TenantContext.clear()` được gọi trong khối `finally` để tránh rò rỉ bộ nhớ ThreadLocal.

---

## 8. Tích Hợp VietQR & Webhook Đối Soát

- **Chuẩn tạo link VietQR:**
  ```text
  https://img.vietqr.io/image/{BANK_BIN}-{ACCOUNT_NO}-compact2.png?amount={AMOUNT}&addInfo={TRANSFER_CODE}&accountName={ACCOUNT_NAME}
  ```
- `TRANSFER_CODE` phải là mã định danh không dấu, ví dụ: `HP9A00124` (Học phí Lớp 9A - Học sinh ID 001 - Tháng 24).
- **Webhook Nhận Tiền:** Webhook từ SePay/Casso/Ngân hàng gọi về `/api/webhooks/payment` với payload chứa số tiền và nội dung chuyển khoản -> Tìm kiếm invoice theo `TRANSFER_CODE` -> Chuyển trạng thái sang `PAID` và cập nhật ngày gạch nợ.

---

## 9. 🤖 Cẩm Nang Dành Riêng Cho AI Agent

Nếu bạn là một AI Agent (Antigravity, Cursor, Copilot, Windsurf):

1. **Quy tắc Multi-Tenant:**
   - Luôn gọi `TenantContext.getTenantId()` trong `Service` để lấy `tenant_id` của request hiện tại.
   - Khi tạo entity mới, luôn gán `entity.setTenantId(tenantId)`.
   - Trong `Repository`, phương thức truy vấn danh sách hoặc tìm theo ID luôn phải có điều kiện `tenant_id` (VD: `Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId)`).
2. **Quy tắc Transaction:**
   - Đánh dấu `@Transactional` ở mức Service implementation.
   - Với các hàm chỉ đọc dữ liệu, dùng `@Transactional(readOnly = true)`.
3. **Quy tắc Kiểm tra Quyền & Gói Dịch Vụ:**
   - Đọc bảng `plan_features` và `role_permissions` (từ file migration V2) trước khi sinh các logic phân quyền mới.
4. **Quy tắc Commit:**
   - Viết commit message theo chuẩn Conventional Commits: `feat(billing): add auto-reconciliation webhook handler`.
