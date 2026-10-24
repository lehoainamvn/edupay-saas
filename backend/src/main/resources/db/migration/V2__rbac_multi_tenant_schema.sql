-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║     V2__rbac_multi_tenant_schema.sql                                         ║
-- ║     EduPay SaaS - RBAC, Tenant Config, Feature Flags, Menu Permissions      ║
-- ║     Flyway Migration | PostgreSQL 16                                          ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

-- ==============================================================================
-- NHÓM 1: GÓI DỊCH VỤ (SUBSCRIPTION PLANS & FEATURE FLAGS)
-- ==============================================================================

-- 1.1. Bảng định nghĩa các Gói dịch vụ (Plans)
--      Gói nào mở tính năng gì được định nghĩa tập trung tại đây.
CREATE TABLE IF NOT EXISTS subscription_plans (
    id         VARCHAR(50) PRIMARY KEY,         -- Khóa dạng text: TRIAL, MONTHLY, ANNUAL
    name       VARCHAR(100) NOT NULL,           -- Tên hiển thị: "Dùng thử 14 ngày"
    price      NUMERIC(12, 2) DEFAULT 0,        -- Giá (VNĐ)
    duration_days INT DEFAULT 14,               -- Thời hạn (ngày)
    is_active  BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Seed dữ liệu các Gói mặc định
INSERT INTO subscription_plans (id, name, price, duration_days) VALUES
('TRIAL',   'Dùng thử miễn phí 14 ngày', 0,      14),
('MONTHLY', 'Thuê tháng',                 59000,  30),
('ANNUAL',  'Thuê năm (Tiết kiệm 14%)',   499000, 365)
ON CONFLICT (id) DO NOTHING;

-- 1.2. Bảng các Tính năng (Features) của hệ thống
--      Mỗi tính năng là 1 dòng, quản lý tập trung.
CREATE TABLE IF NOT EXISTS features (
    id          VARCHAR(100) PRIMARY KEY,        -- VD: 'ATTENDANCE', 'BILLING', 'AUTO_RECONCILE'
    name        VARCHAR(200) NOT NULL,           -- Tên tiếng Việt
    description TEXT,
    category    VARCHAR(50),                     -- Nhóm: CORE, ADDON, PREMIUM
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

INSERT INTO features (id, name, description, category) VALUES
('CLASS_MANAGEMENT',     'Quản lý Lớp học',                  'Tạo, sửa, xóa lớp học',                             'CORE'),
('STUDENT_MANAGEMENT',   'Quản lý Học sinh',                  'Thêm, sửa, xóa học sinh; import Excel',              'CORE'),
('ATTENDANCE',           'Điểm danh 1 chạm',                  'Điểm danh nhanh trên điện thoại',                    'CORE'),
('BILLING_MANUAL',       'Tính học phí thủ công',             'Tính và xuất phiếu thu từng lớp',                    'CORE'),
('VIETQR_STATIC',        'Mã VietQR tĩnh',                    'Sinh mã QR từ STK của giáo viên',                    'CORE'),
('VIETQR_DYNAMIC',       'Mã VietQR động định danh',          'Sinh QR đúng số tiền + mã nội dung CK',              'CORE'),
('PUBLIC_INVOICE',       'Link phiếu thu Phụ huynh',          'Link tra cứu không cần đăng nhập',                   'CORE'),
('ZALO_SHARE',           'Chia sẻ & Nhắc nợ Zalo',            'Tạo tin nhắn Zalo kèm link phiếu',                   'CORE'),
('MANUAL_RECONCILE',     'Gạch nợ thủ công',                  'Xác nhận đã thu học phí 1 chạm',                     'CORE'),
('REPORT_BASIC',         'Báo cáo cơ bản',                    'Tổng doanh thu, tỷ lệ thu',                          'CORE'),
('EXPORT_EXCEL',         'Xuất báo cáo Excel',                'Xuất file Excel học phí tháng',                      'CORE'),
('EXPORT_PDF',           'Xuất báo cáo PDF',                  'Xuất file PDF phiếu thu in ấn',                      'CORE'),
('AUTO_RECONCILE',       'Gạch nợ tự động (Webhook)',         'Tích hợp SePay/Casso gạch nợ tự động',              'ADDON'),
('ZALO_ZNS',             'Thông báo Zalo ZNS tự động',        'Gửi ZNS nhắc tiền tự động khi có biến động',         'ADDON'),
('MULTI_CLASS_REPORT',   'Báo cáo tổng hợp nhiều lớp',        'Dashboard doanh thu tổng hợp theo tháng/năm',        'PREMIUM'),
('UNLIMITED_CLASSES',    'Không giới hạn số lớp & học sinh',  'Gói Trial giới hạn 2 lớp / 30 học sinh',             'PREMIUM'),
('CUSTOM_BRANDING',      'Tùy chỉnh logo thương hiệu',        'Đổi logo trên phiếu thu theo trung tâm',             'PREMIUM')
ON CONFLICT (id) DO NOTHING;

-- 1.3. Bảng ánh xạ Gói <-> Tính năng (Plan Feature Matrix)
--      Gói nào được dùng tính năng gì, giới hạn như thế nào.
CREATE TABLE IF NOT EXISTS plan_features (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id      VARCHAR(50)  NOT NULL REFERENCES subscription_plans(id) ON DELETE CASCADE,
    feature_id   VARCHAR(100) NOT NULL REFERENCES features(id) ON DELETE CASCADE,
    is_enabled   BOOLEAN DEFAULT TRUE,
    -- Giới hạn số lượng (NULL = không giới hạn)
    max_classes  INT,                            -- Giới hạn số lớp được tạo
    max_students INT,                            -- Giới hạn tổng số học sinh
    CONSTRAINT uk_plan_feature UNIQUE (plan_id, feature_id)
);

-- Gói TRIAL: Giới hạn 2 lớp, 30 học sinh, không có Auto Reconcile / Zalo ZNS
INSERT INTO plan_features (plan_id, feature_id, is_enabled, max_classes, max_students) VALUES
('TRIAL', 'CLASS_MANAGEMENT',   TRUE, 2,    NULL),
('TRIAL', 'STUDENT_MANAGEMENT', TRUE, NULL, 30),
('TRIAL', 'ATTENDANCE',         TRUE, NULL, NULL),
('TRIAL', 'BILLING_MANUAL',     TRUE, NULL, NULL),
('TRIAL', 'VIETQR_DYNAMIC',     TRUE, NULL, NULL),
('TRIAL', 'PUBLIC_INVOICE',     TRUE, NULL, NULL),
('TRIAL', 'ZALO_SHARE',         TRUE, NULL, NULL),
('TRIAL', 'MANUAL_RECONCILE',   TRUE, NULL, NULL),
('TRIAL', 'REPORT_BASIC',       TRUE, NULL, NULL),
('TRIAL', 'AUTO_RECONCILE',    FALSE, NULL, NULL),
('TRIAL', 'ZALO_ZNS',          FALSE, NULL, NULL),
('TRIAL', 'EXPORT_EXCEL',      FALSE, NULL, NULL),
('TRIAL', 'EXPORT_PDF',        FALSE, NULL, NULL),
('TRIAL', 'UNLIMITED_CLASSES', FALSE, NULL, NULL)
ON CONFLICT (plan_id, feature_id) DO NOTHING;

-- Gói MONTHLY: Không giới hạn lớp/học sinh, có Export, không có Auto Reconcile mặc định
INSERT INTO plan_features (plan_id, feature_id, is_enabled, max_classes, max_students) VALUES
('MONTHLY', 'CLASS_MANAGEMENT',   TRUE, NULL, NULL),
('MONTHLY', 'STUDENT_MANAGEMENT', TRUE, NULL, NULL),
('MONTHLY', 'ATTENDANCE',         TRUE, NULL, NULL),
('MONTHLY', 'BILLING_MANUAL',     TRUE, NULL, NULL),
('MONTHLY', 'VIETQR_DYNAMIC',     TRUE, NULL, NULL),
('MONTHLY', 'PUBLIC_INVOICE',     TRUE, NULL, NULL),
('MONTHLY', 'ZALO_SHARE',         TRUE, NULL, NULL),
('MONTHLY', 'MANUAL_RECONCILE',   TRUE, NULL, NULL),
('MONTHLY', 'REPORT_BASIC',       TRUE, NULL, NULL),
('MONTHLY', 'EXPORT_EXCEL',       TRUE, NULL, NULL),
('MONTHLY', 'EXPORT_PDF',         TRUE, NULL, NULL),
('MONTHLY', 'UNLIMITED_CLASSES',  TRUE, NULL, NULL),
('MONTHLY', 'MULTI_CLASS_REPORT', TRUE, NULL, NULL),
('MONTHLY', 'AUTO_RECONCILE',    FALSE, NULL, NULL),
('MONTHLY', 'ZALO_ZNS',          FALSE, NULL, NULL)
ON CONFLICT (plan_id, feature_id) DO NOTHING;

-- Gói ANNUAL: Đầy đủ tất cả tính năng, bao gồm cả Auto Reconcile và Zalo ZNS
INSERT INTO plan_features (plan_id, feature_id, is_enabled, max_classes, max_students)
SELECT 'ANNUAL', id, TRUE, NULL, NULL FROM features
ON CONFLICT (plan_id, feature_id) DO NOTHING;


-- ==============================================================================
-- NHÓM 2: TENANT CONFIGURATION (CÀI ĐẶT RIÊNG TỪNG TENANT)
-- ==============================================================================

-- 2.1. Bảng Cấu hình tham số riêng của mỗi Tenant
--      Mỗi giáo viên có thể tùy chỉnh giao diện, tên thương hiệu, cú pháp CK...
CREATE TABLE IF NOT EXISTS tenant_configs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    config_key  VARCHAR(100) NOT NULL,           -- Tên tham số
    config_value TEXT,                           -- Giá trị
    description VARCHAR(255),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_tenant_config UNIQUE (tenant_id, config_key)
);

-- Ví dụ các config key được hỗ trợ:
-- 'center_name'        → Tên trung tâm/tên thương hiệu hiển thị trên phiếu thu
-- 'transfer_prefix'    → Tiền tố mã CK: mặc định 'HP', có thể đổi thành 'TC' hoặc 'AMATH'
-- 'invoice_footer'     → Lời cảm ơn cuối phiếu thu: "Cảm ơn quý phụ huynh..."
-- 'logo_url'           → URL logo trung tâm (cho gói Premium)
-- 'theme_color'        → Màu sắc chủ đạo giao diện phiếu thu (VD: '#1B365D')
-- 'zns_otp_template'   → Mã template Zalo ZNS đã đăng ký


-- ==============================================================================
-- NHÓM 3: PHÂN QUYỀN (RBAC - Role Based Access Control)
-- ==============================================================================

-- 3.1. Bảng Quyền hạn chi tiết (Permissions)
--      Mỗi quyền = 1 hành động cụ thể trong hệ thống.
--      Định dạng: <RESOURCE>:<ACTION> (chuẩn quốc tế)
CREATE TABLE IF NOT EXISTS permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) UNIQUE NOT NULL,    -- VD: 'class:create', 'invoice:delete'
    name        VARCHAR(200) NOT NULL,           -- Tên tiếng Việt
    resource    VARCHAR(50) NOT NULL,            -- Nhóm tài nguyên: class, student, invoice...
    action      VARCHAR(50) NOT NULL,            -- Hành động: create, read, update, delete
    feature_id  VARCHAR(100) REFERENCES features(id),  -- Liên kết tính năng (nếu có)
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

INSERT INTO permissions (code, name, resource, action, feature_id) VALUES
-- Quản lý Lớp học
('class:read',           'Xem danh sách lớp học',          'class',    'read',   'CLASS_MANAGEMENT'),
('class:create',         'Tạo lớp học mới',                 'class',    'create', 'CLASS_MANAGEMENT'),
('class:update',         'Sửa thông tin lớp học',           'class',    'update', 'CLASS_MANAGEMENT'),
('class:delete',         'Xóa lớp học',                     'class',    'delete', 'CLASS_MANAGEMENT'),
-- Quản lý Học sinh
('student:read',         'Xem danh sách học sinh',          'student',  'read',   'STUDENT_MANAGEMENT'),
('student:create',       'Thêm học sinh',                   'student',  'create', 'STUDENT_MANAGEMENT'),
('student:update',       'Sửa thông tin học sinh',          'student',  'update', 'STUDENT_MANAGEMENT'),
('student:delete',       'Xóa học sinh',                    'student',  'delete', 'STUDENT_MANAGEMENT'),
('student:import',       'Import học sinh từ Excel',        'student',  'create', 'STUDENT_MANAGEMENT'),
-- Điểm danh
('attendance:read',      'Xem lịch sử điểm danh',          'attendance','read',   'ATTENDANCE'),
('attendance:create',    'Ghi nhận buổi điểm danh',        'attendance','create', 'ATTENDANCE'),
('attendance:update',    'Sửa điểm danh',                  'attendance','update', 'ATTENDANCE'),
-- Học phí & Phiếu thu
('invoice:read',         'Xem danh sách phiếu thu',        'invoice',  'read',   'BILLING_MANUAL'),
('invoice:create',       'Tạo phiếu thu học phí',          'invoice',  'create', 'BILLING_MANUAL'),
('invoice:update',       'Sửa phiếu thu',                  'invoice',  'update', 'BILLING_MANUAL'),
('invoice:delete',       'Xóa phiếu thu',                  'invoice',  'delete', 'BILLING_MANUAL'),
('invoice:reconcile',    'Gạch nợ / Xác nhận đã thu',     'invoice',  'update', 'MANUAL_RECONCILE'),
('invoice:export',       'Xuất báo cáo Excel/PDF',         'invoice',  'read',   'EXPORT_EXCEL'),
-- Báo cáo
('report:read',          'Xem báo cáo doanh thu',          'report',   'read',   'REPORT_BASIC'),
-- Cài đặt Tenant
('settings:read',        'Xem cài đặt hệ thống',           'settings', 'read',   NULL),
('settings:update',      'Sửa cài đặt ngân hàng & cấu hình','settings','update', NULL),
-- Quản lý người dùng trong Tenant
('user:read',            'Xem danh sách nhân sự trong trung tâm', 'user','read',  NULL),
('user:create',          'Thêm nhân sự (Thầy phụ/Trợ giảng)',     'user','create',NULL),
('user:update',          'Sửa quyền nhân sự',              'user',     'update', NULL),
('user:delete',          'Xóa nhân sự',                    'user',     'delete', NULL)
ON CONFLICT (code) DO NOTHING;

-- 3.2. Bảng Vai trò trong từng Tenant (Roles)
--      Mỗi Tenant có thể tạo vai trò riêng. Có sẵn 3 vai trò mặc định (hệ thống).
CREATE TABLE IF NOT EXISTS roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID REFERENCES tenants(id) ON DELETE CASCADE,  -- NULL = Role hệ thống mặc định
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    is_system   BOOLEAN DEFAULT FALSE,           -- TRUE = Không thể xóa/sửa tên
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_role_tenant UNIQUE (tenant_id, code)
);

-- Seed các Role mặc định hệ thống (tenant_id = NULL)
INSERT INTO roles (tenant_id, code, name, description, is_system) VALUES
(NULL, 'OWNER',   'Chủ tài khoản / Giáo viên chính', 'Toàn quyền trong phạm vi Tenant. Không thể bị xóa.', TRUE),
(NULL, 'TEACHER', 'Giáo viên phụ / Trợ giảng',        'Điểm danh và xem học sinh trong lớp được phân công.',  TRUE),
(NULL, 'VIEWER',  'Xem báo cáo',                       'Chỉ được xem báo cáo, không thể sửa dữ liệu.',          TRUE)
ON CONFLICT DO NOTHING;

-- 3.3. Bảng ánh xạ Role <-> Permission (Mặc định hệ thống)
CREATE TABLE IF NOT EXISTS role_permissions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

-- ==============================================================================
-- NHÓM 4: QUẢN LÝ NGƯỜI DÙNG TRONG TENANT (MULTI-USER PER TENANT)
-- ==============================================================================

-- 4.1. Bảng người dùng (Users) - Mỗi tenant có thể có nhiều User
--      VD: 1 Trung tâm Toán có thể có 1 owner + 3 giáo viên phụ + 1 kế toán
CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    phone         VARCHAR(20) NOT NULL,
    email         VARCHAR(255),
    full_name     VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url    VARCHAR(500),
    status        VARCHAR(50) DEFAULT 'ACTIVE',  -- ACTIVE, SUSPENDED, PENDING_VERIFY
    is_owner      BOOLEAN DEFAULT FALSE,          -- TRUE = Chủ tài khoản duy nhất của Tenant
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_user_phone_tenant UNIQUE (tenant_id, phone)
);

-- 4.2. Bảng ánh xạ User <-> Role trong Tenant
CREATE TABLE IF NOT EXISTS user_roles (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id   UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    assigned_by UUID REFERENCES users(id),
    CONSTRAINT uk_user_role_tenant UNIQUE (user_id, role_id)
);

-- 4.3. Bảng ghi đè quyền riêng biệt cho User (Optional Overrides)
--      VD: User A có role TEACHER nhưng cần thêm quyền 'invoice:export' đặc biệt
CREATE TABLE IF NOT EXISTS user_permission_overrides (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    is_granted    BOOLEAN NOT NULL DEFAULT TRUE,  -- TRUE = cấp thêm, FALSE = thu hồi
    reason        VARCHAR(255),
    granted_by    UUID REFERENCES users(id),
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_user_permission_override UNIQUE (user_id, permission_id)
);


-- ==============================================================================
-- NHÓM 5: PHÂN QUYỀN MENU (MENU VISIBILITY & ACCESS CONTROL)
-- ==============================================================================

-- 5.1. Bảng định nghĩa cây Menu hệ thống
CREATE TABLE IF NOT EXISTS menus (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) UNIQUE NOT NULL,    -- 'DASHBOARD', 'CLASSES', 'BILLING'
    name        VARCHAR(200) NOT NULL,           -- Tên hiển thị trên sidebar
    icon        VARCHAR(100),                    -- Tên icon (Material Icon / FontAwesome)
    route       VARCHAR(200),                    -- Angular route path: '/dashboard'
    parent_id   UUID REFERENCES menus(id),       -- NULL = Menu cha (cấp 1)
    sort_order  INT DEFAULT 0,                   -- Thứ tự hiển thị
    feature_id  VARCHAR(100) REFERENCES features(id),  -- Menu này thuộc tính năng nào?
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

INSERT INTO menus (code, name, icon, route, parent_id, sort_order, feature_id) VALUES
-- Menu cấp 1
('DASHBOARD',    'Tổng quan',           'dashboard',        '/dashboard',   NULL, 1,  'REPORT_BASIC'),
('CLASSES',      'Lớp học',             'school',           '/classes',     NULL, 2,  'CLASS_MANAGEMENT'),
('STUDENTS',     'Học sinh',            'groups',           '/students',    NULL, 3,  'STUDENT_MANAGEMENT'),
('ATTENDANCE',   'Điểm danh',           'how_to_reg',       '/attendance',  NULL, 4,  'ATTENDANCE'),
('BILLING',      'Học phí & Phiếu thu', 'receipt_long',     '/billing',     NULL, 5,  'BILLING_MANUAL'),
('REPORTS',      'Báo cáo',             'bar_chart',        '/reports',     NULL, 6,  'REPORT_BASIC'),
('SETTINGS',     'Cài đặt',             'settings',         '/settings',    NULL, 7,  NULL),
('ADMIN_USERS',  'Quản lý nhân sự',     'manage_accounts',  '/admin/users', NULL, 8,  NULL)
ON CONFLICT (code) DO NOTHING;

-- 5.2. Bảng ánh xạ Permission nào kiểm soát Menu nào
--      Angular sẽ dựa vào đây để ẩn/hiện item trong sidebar
CREATE TABLE IF NOT EXISTS menu_permissions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_id       UUID NOT NULL REFERENCES menus(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_menu_permission UNIQUE (menu_id, permission_id)
);


-- ==============================================================================
-- NHÓM 6: INDEX TỐI ƯU HÓA HIỆU NĂNG
-- ==============================================================================

CREATE INDEX IF NOT EXISTS idx_tenant_configs_tenant   ON tenant_configs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_tenant            ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_phone             ON users(phone);
CREATE INDEX IF NOT EXISTS idx_user_roles_user         ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_tenant       ON user_roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role   ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_plan_features_plan      ON plan_features(plan_id);
CREATE INDEX IF NOT EXISTS idx_menus_parent            ON menus(parent_id);
CREATE INDEX IF NOT EXISTS idx_menus_code              ON menus(code);
