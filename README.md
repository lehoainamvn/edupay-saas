# 🎓 EduPay SaaS - Quản Lý Học Sinh & Đối Soát Học Phí VietQR

[![Tech Stack](https://img.shields.io/badge/Stack-Java%20%7C%20Angular%20%7C%20PostgreSQL-blue.svg)](#-công-nghệ-sử-dụng)
[![License](https://img.shields.io/badge/License-Commercial-green.svg)](#)
[![Architecture](https://img.shields.io/badge/Architecture-Multi--Tenant%20SaaS-orange.svg)](#)

> **EduPay** là nền tảng SaaS (Software-as-a-Service) chuyên biệt dành cho **giáo viên dạy kèm, gia sư độc lập và các trung tâm đào tạo vi mô**. Hệ thống giải quyết trọn vẹn bài toán: Điểm danh 1 chạm trên di động, tự động tính học phí theo số buổi thực tế, sinh link phiếu thu kèm **mã VietQR động** chuẩn Napas247, và gạch nợ đối soát tự động qua Zalo.

---

## 🚀 Tính Năng Cốt Lõi (Key Features)

* **🏢 Mô hình Multi-Tenant:** Mỗi giáo viên đăng ký là 1 Tenant độc lập, dữ liệu học sinh, lớp học và tài khoản ngân hàng nhận tiền được phân tách bảo mật 100%.
* **📱 Điểm danh 1 chạm (Mobile-First):** Giao diện thiết kế riêng cho màn hình cảm ứng điện thoại, chạm đổi màu trạng thái (Có mặt / Có phép / Vắng) cực nhanh trong 30 giây.
* **🧮 Tính học phí tự động cuối kỳ:** Tự động nhân số buổi thực học × đơn giá, hỗ trợ thêm phụ phí (sách vở) hoặc giảm trừ học bổng 1-click.
* **⚡ VietQR Động Định Danh:** Tự động sinh mã VietQR Napas247 chính xác số tiền và mã nội dung chuyển khoản chuẩn (tránh tình trạng phụ huynh ghi tên cưng ở nhà không biết ai).
* **🔗 Cổng Tra Cứu Phụ Huynh (Public):** Gửi link Zalo riêng cho từng phụ huynh xem chi tiết nhật ký các buổi con đã học và quét mã nộp tiền mà không cần tạo tài khoản.
* **✅ Đối Soát & Gạch Nợ Tức Thì:** Hỗ trợ gạch nợ thủ công 1 chạm và webhook kết nối SePay/Casso tự động xác nhận khi có biến động số dư ngân hàng.
* **👑 SuperAdmin Platform:** Quản lý danh sách giáo viên thuê, gói dịch vụ (59.000đ/tháng hoặc 499.000đ/năm), tự động khóa/mở hạn dùng thử 14 ngày.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

| Thành phần | Công nghệ | Chi tiết |
| :--- | :--- | :--- |
| **Backend** | **Java 17 / Spring Boot 3.3** | Spring Data JPA, Spring Security (JWT), Flyway, Lombok, MapStruct, Apache POI |
| **Frontend** | **Angular 17+** | TypeScript, TailwindCSS, Reactive Forms, Chart.js, Mobile-first Responsive |
| **Database** | **PostgreSQL 16** | Row-Level Security, B-Tree Index tối ưu hóa truy vấn `tenant_id` |
| **Thanh toán** | **VietQR API** | Chuẩn Napas247 liên ngân hàng |
| **DevOps** | **Docker & Compose** | Multi-stage build, Nginx Alpine, Railway / Render deployment |

---

## 📁 Cấu Trúc Thư Mục Dự Án (Repository Structure)

* 👉 **Backend Documentation:** Chi tiết kiến trúc Spring Boot, Multi-Tenant và Flyway xem tại [backend/README.md](backend/README.md)
* 👉 **Frontend Documentation:** Chi tiết kiến trúc Angular 17+ Mobile-First và Public Portal xem tại [frontend/README.md](frontend/README.md)
* 👉 **Thiết kế RBAC & Phân Quyền:** Xem chi tiết tại [docs/THIET_KE_RBAC_VA_PHAN_QUYEN.md](docs/THIET_KE_RBAC_VA_PHAN_QUYEN.md)

```text
edupay-saas/
├── backend/                             # Java Spring Boot 3 Backend (Xem backend/README.md)
│   ├── pom.xml
│   ├── README.md                        # Hướng dẫn chi tiết cho BE Dev & AI Agent
│   └── src/
│       ├── main/java/com/edupay/
│       │   ├── config/                  # Security, WebMvc, TenantContext
│       │   ├── security/                # JWT Token Provider, Auth Filter
│       │   ├── controller/              # Auth, Class, Student, Attendance, Billing...
│       │   ├── service/                 # Business Logic
│       │   ├── repository/              # Spring Data JPA Repositories
│       │   ├── entity/                  # BaseEntity (tenant_id), Tenant, Class, Student...
│       │   ├── dto/                     # Request/Response DTOs
│       │   └── exception/               # Global Exception Handler
│       └── main/resources/
│           ├── application.yml
│           ├── application-dev.yml      # Cấu hình môi trường DEV local
│           ├── application-prod.yml     # Cấu hình môi trường PRODUCTION
│           └── db/migration/            # Flyway SQL Migrations (V1, V2...)
│
├── frontend/                            # Angular 17+ Frontend (Xem frontend/README.md)
│   ├── package.json
│   ├── README.md                        # Hướng dẫn chi tiết cho FE Dev & AI Agent
│   ├── angular.json
│   └── src/
│       ├── index.html
│       ├── styles.css                   # Mobile-First CSS
│       ├── environments/                # Cấu hình API Dev vs Prod
│       └── app/
│           ├── core/                    # Guards, HTTP Interceptors, Core Services
│           ├── shared/                  # Reusable Components, Pipes, Models
│           └── features/                # Auth, Dashboard, Classes, Attendance, Billing, Public...
│
├── docker/                              # Containerization & Deployment
│   ├── docker-compose.yml               # Khởi chạy full stack Local (Postgres + BE + FE)
│   ├── backend.Dockerfile
│   └── frontend.Dockerfile
│
├── docs/                                # Tài liệu kinh doanh, khảo sát & phân rã task
│   ├── KE_HOACH_DU_AN_JAVA_ANGULAR_POSTGRES.xlsx
│   └── PHAN_TICH_TOAN_DIEN_SAAS_HOC_PHI.xlsx
│
├── .gitignore
└── README.md
```

---

## 🚦 Hướng Dẫn Khởi Chạy Ứng Dụng (Getting Started)

Dự án hỗ trợ 2 hình thức khởi chạy tùy theo nhu cầu:
1. **Cách 1: Khởi chạy trọn gói bằng Docker** (Phù hợp test nhanh toàn bộ hệ thống).
2. **Cách 2: Khởi chạy thủ công từng phần** (Khuyến nghị cho Developer viết code với tính năng Live Reload & Debug).

---

### 🐳 Cách 1: Khởi Chạy Trọn Gói Bằng Docker (All-in-One)

Chỉ với **1 câu lệnh duy nhất** để khởi chạy toàn bộ (PostgreSQL 16 + Backend Spring Boot + Frontend Angular):

```bash
docker compose -f docker/docker-compose.yml up --build
```

---

### 💻 Cách 2: Khởi Chạy Thủ Công Từng Phần (Manual Local Run — Cho Dev)

Dành cho lập trình viên phát triển code cần hot-reload, debug breakpoint và xem log chi tiết.

#### 📋 Yêu cầu môi trường cài đặt sẵn:
* **Java:** JDK 17 trở lên (`java -version`)
* **Maven:** 3.8+ (`mvn -version`) hoặc dùng script `./mvnw`
* **Node.js:** Phiên bản 18 LTS hoặc 20 LTS (`node -v`)
* **NPM:** Phiên bản 9 trở lên (`npm -v`)
* **Docker:** (Tùy chọn, dùng để bật nhanh PostgreSQL dev mà không cần cài trực tiếp)

---

#### 🔹 Bước 1: Khởi động Cơ Sở Dữ Liệu PostgreSQL

Chọn **1 trong 2 lựa chọn**:

* **Lựa chọn A (Khuyên dùng - Dùng Docker Dev siêu nhẹ):**
  Chỉ khởi chạy container PostgreSQL 16 và pgAdmin cho Dev:
  ```bash
  docker compose -f docker/docker-compose.dev.yml up -d
  ```
  *(Dữ liệu mẫu và kết nối đã được map sẵn tới port `5432`)*

* **Lựa chọn B (Dùng PostgreSQL đã cài sẵn trên máy):**
  Đăng nhập vào PostgreSQL cục bộ và tạo database `edupay_dev_db`:
  ```sql
  CREATE DATABASE edupay_dev_db;
  ```

---

#### 🔹 Bước 2: Khởi động Backend (Spring Boot 3)

Mở **Terminal thứ 1**, di chuyển vào thư mục `backend/` và khởi chạy với profile `dev`:

```bash
cd backend

# Cách chạy với Maven:
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev

# Hoặc nếu dùng Maven Wrapper:
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=dev
```

> 💡 **Cơ chế tự động:** Khi Backend khởi động, **Flyway** sẽ tự động quét thư mục `db/migration` và chạy các file `V1__init_schema.sql`, `V2__rbac_multi_tenant_schema.sql` để khởi tạo toàn bộ 16 bảng CSDL và nạp dữ liệu mẫu ban đầu!

Khi console xuất hiện dòng:
```text
Tomcat started on port 8080 (http) with context path '/api'
Started EdupayApplication in X.XXX seconds
```
Backend API đã sẵn sàng tại: `http://localhost:8080/api`

---

#### 🔹 Bước 3: Khởi động Frontend (Angular 17+)

Mở **Terminal thứ 2**, di chuyển vào thư mục `frontend/`:

```bash
cd frontend

# 1. Cài đặt các thư viện phụ thuộc:
npm install

# 2. Khởi chạy dev server (tự động bật trình duyệt):
npm start
# Hoặc:
ng serve --open
```

Giao diện ứng dụng Angular sẽ khởi chạy tại: `http://localhost:4200`

---

### 🌐 Bảng Tổng Hợp Cổng & Đường Dẫn Dịch Vụ (Service Map)

| Dịch vụ | Chạy Docker trọn gói | Chạy thủ công Local (Dev) | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Frontend Web** | `http://localhost:80` | `http://localhost:4200` | Giao diện giáo viên & Mobile-first |
| **Backend REST API** | `http://localhost:8080/api` | `http://localhost:8080/api` | API Spring Boot 3.3 |
| **PostgreSQL DB** | `localhost:5432` (`edupay_db`) | `localhost:5432` (`edupay_dev_db`) | User: `postgres` / Pass: `postgres123` |
| **pgAdmin (GUI DB)** | *(Không bật mặc định)* | `http://localhost:5050` | `admin@edupay.dev` / `admin123` |
| **Public Invoice (Mock)**| `http://localhost:80/p/:hash` | `http://localhost:4200/p/:hash` | Cổng phụ huynh quét VietQR không cần login |

---

## 🤖 Hệ Thống Kỹ Năng & Luật Cho AI Agent (Agent Skills & Rules)

Dự án tích hợp sẵn bộ **Custom Skills & Rules** chuẩn hóa cho các AI Coding Agent (Antigravity, Cursor, Windsurf, Copilot). Khi Agent tham gia dự án, các tệp này tự động định hướng Agent tuân thủ 100% chuẩn kỹ thuật của dự án.

### 1. Luật Lập Trình Cho AI Agent:
* 📜 **Quy tắc cốt lõi (P0 Rules):** Xem tại [AGENTS.md](AGENTS.md) và [.agents/rules/ai-agent-rules.md](.agents/rules/ai-agent-rules.md).
* 📜 **Quy chuẩn mã nguồn dự án:** Xem tại [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md).

### 2. Danh Sách Skills Tích Hợp Sẵn:
| Tên Skill | Đường dẫn | Chức năng hướng dẫn cho Agent / Dev |
| :--- | :--- | :--- |
| **`edupay-workflow`** | [.agents/skills/edupay-workflow/](.agents/skills/edupay-workflow/SKILL.md) | Quy trình phân nhánh Git (`develop`, `feature/sprint...`), pull request standards |
| **`angular-mobile-ui`** | [.agents/skills/angular-mobile-ui/](.agents/skills/angular-mobile-ui/SKILL.md) | Xây dựng giao diện Angular 17+ Standalone, Signals, Mobile Touch targets (>= 48px), Form validation |
| **`spring-boot-multitenant`** | [.agents/skills/spring-boot-multitenant/](.agents/skills/spring-boot-multitenant/SKILL.md) | Kiến trúc Spring Boot 3 Multi-tenant, `TenantContext`, `BaseEntity`, JPA repository lọc `tenant_id` |
| **`vietqr-payment-reconciliation`** | [.agents/skills/vietqr-payment-reconciliation/](.agents/skills/vietqr-payment-reconciliation/SKILL.md) | Sinh mã VietQR Napas247 động, xử lý Webhook ngân hàng, đảm bảo Idempotency gạch nợ |
| **`edupay-testing-quality`** | [.agents/skills/edupay-testing-quality/](.agents/skills/edupay-testing-quality/SKILL.md) | Viết Unit Test Mockito, MockMvc slice tests, Angular component tests |
