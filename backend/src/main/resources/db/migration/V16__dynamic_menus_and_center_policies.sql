-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║     V16__dynamic_menus_and_center_policies.sql                               ║
-- ║     EduPay SaaS - Phân quyền Menu động & Quản lý Trung tâm / Gia sư           ║
-- ║     Flyway Migration | PostgreSQL 16                                         ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

-- ==============================================================================
-- 1. BẢNG LIÊN KẾT ROLE VÀ MENUS (DYNAMIC ROLE-BASED MENUS)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS role_menus (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    menu_id UUID NOT NULL REFERENCES menus(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (role_id, menu_id)
);

CREATE INDEX IF NOT EXISTS idx_role_menus_role ON role_menus(role_id);
CREATE INDEX IF NOT EXISTS idx_role_menus_menu ON role_menus(menu_id);

-- ==============================================================================
-- 2. BỔ SUNG ROLES CHUYÊN BIỆT: CENTER_ADMIN & SOLO_TUTOR (NẾU CHƯA CÓ)
-- ==============================================================================
INSERT INTO roles (id, tenant_id, code, name, description, is_system, is_active)
SELECT 'e0000000-0000-0000-0000-000000000002', NULL, 'CENTER_ADMIN', 'Quản lý Trung tâm', 'Toàn quyền điều hành cơ sở trung tâm, quản lý giáo viên và phân công lớp.', TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'CENTER_ADMIN' AND tenant_id IS NULL);

INSERT INTO roles (id, tenant_id, code, name, description, is_system, is_active)
SELECT 'e0000000-0000-0000-0000-000000000003', NULL, 'SOLO_TUTOR', 'Gia sư Tự do', 'Dạy kèm độc lập tại nhà, tự chủ lớp học, học sinh và tài khoản VietQR cá nhân.', TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'SOLO_TUTOR' AND tenant_id IS NULL);

-- Cập nhật tên hiển thị của vai trò OWNER cho thân thiện
UPDATE roles 
SET name = 'Chủ cơ sở / Quản lý chính', 
    description = 'Toàn quyền điều hành trong phạm vi cơ sở trung tâm hoặc gia sư cá nhân'
WHERE code = 'OWNER' AND tenant_id IS NULL;

-- Gán thêm role CENTER_ADMIN cho các User Owner của tenant kiểu CENTER
INSERT INTO user_roles (id, tenant_id, user_id, role_id)
SELECT gen_random_uuid(), u.tenant_id, u.id, r.id
FROM users u
JOIN tenants t ON u.tenant_id = t.id
CROSS JOIN roles r
WHERE t.tenant_type = 'CENTER' 
  AND u.is_owner = TRUE 
  AND r.code = 'CENTER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- Gán thêm role SOLO_TUTOR cho các User Owner của tenant kiểu SOLO_TUTOR
INSERT INTO user_roles (id, tenant_id, user_id, role_id)
SELECT gen_random_uuid(), u.tenant_id, u.id, r.id
FROM users u
JOIN tenants t ON u.tenant_id = t.id
CROSS JOIN roles r
WHERE t.tenant_type = 'SOLO_TUTOR' 
  AND u.is_owner = TRUE 
  AND r.code = 'SOLO_TUTOR'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- Gán các permissions cần thiết cho CENTER_ADMIN tương đương OWNER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r_target.id, rp.permission_id
FROM roles r_owner
JOIN role_permissions rp ON r_owner.id = rp.role_id
CROSS JOIN roles r_target
WHERE r_owner.code = 'OWNER' 
  AND r_target.code = 'CENTER_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Gán các permissions cho SOLO_TUTOR tương đương OWNER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r_target.id, rp.permission_id
FROM roles r_owner
JOIN role_permissions rp ON r_owner.id = rp.role_id
CROSS JOIN roles r_target
WHERE r_owner.code = 'OWNER' 
  AND r_target.code = 'SOLO_TUTOR'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ==============================================================================
-- 3. BẢNG CHÍNH SÁCH PHÂN QUYỀN NỘI BỘ GIÁO VIÊN TRUNG TÂM (TENANT_TEACHER_POLICIES)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS tenant_teacher_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    can_edit_student_fee BOOLEAN NOT NULL DEFAULT FALSE,
    can_create_class BOOLEAN NOT NULL DEFAULT FALSE,
    can_create_student BOOLEAN NOT NULL DEFAULT FALSE,
    can_view_billing BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Khởi tạo policy mặc định cho Trung tâm Ánh Dương và tất cả các tenant CENTER hiện có
INSERT INTO tenant_teacher_policies (tenant_id, can_edit_student_fee, can_create_class, can_create_student, can_view_billing)
SELECT t.id, FALSE, FALSE, FALSE, FALSE
FROM tenants t
WHERE t.tenant_type = 'CENTER'
ON CONFLICT (tenant_id) DO NOTHING;

-- ==============================================================================
-- 4. BỔ SUNG & ĐỒNG BỘ DANH MỤC MENUS HỆ THỐNG
-- ==============================================================================
-- Cập nhật hoặc chèn các menu cốt lõi
INSERT INTO menus (id, code, name, icon, route, parent_id, sort_order, is_active) VALUES
('b0000000-0000-0000-0000-000000000001', 'DASHBOARD',        'Tổng quan',               'chart-bar',              '/dashboard',            NULL, 1,  TRUE),
('b0000000-0000-0000-0000-000000000002', 'CLASSES',          'Lớp học',                 'academic-cap',           '/classes',              NULL, 2,  TRUE),
('b0000000-0000-0000-0000-000000000003', 'STUDENTS',         'Học sinh',                'user-group',             '/students',             NULL, 3,  TRUE),
('b0000000-0000-0000-0000-000000000004', 'ATTENDANCE',       'Điểm danh',               'calendar-check',         '/attendance',           NULL, 4,  TRUE),
('b0000000-0000-0000-0000-000000000005', 'BILLING',          'Học phí VietQR',          'banknotes',              '/billing',              NULL, 5,  TRUE),
('b0000000-0000-0000-0000-000000000006', 'SETTINGS',         'Cài đặt QR & Tài khoản',  'cog-6-tooth',            '/settings',             NULL, 6,  TRUE),
('b0000000-0000-0000-0000-000000000007', 'CENTER_TEACHERS',  'Đội ngũ Giáo viên',       'users-profile',          '/center/teachers',      NULL, 7,  TRUE),
('b0000000-0000-0000-0000-000000000008', 'ADMIN_PLATFORM',   'Tổng quan Nền tảng',      'shield-check',           '/admin/platform',       NULL, 8,  TRUE),
('b0000000-0000-0000-0000-000000000009', 'ADMIN_CENTERS',    'Quản lý Trung tâm',       'building-office',        '/admin/centers',        NULL, 9,  TRUE),
('b0000000-0000-0000-0000-000000000010', 'ADMIN_TUTORS',     'Quản lý Gia sư',          'user-badge',             '/admin/tutors',         NULL, 10, TRUE),
('b0000000-0000-0000-0000-000000000011', 'ADMIN_USERS',      'Tất cả Người dùng',       'users',                  '/admin/users',          NULL, 11, TRUE),
('b0000000-0000-0000-0000-000000000012', 'ADMIN_ROLES',      'Phân quyền RBAC',         'key',                    '/admin/roles',          NULL, 12, TRUE),
('b0000000-0000-0000-0000-000000000013', 'ADMIN_MENUS',      'Quản lý Menu Động',       'bars-3',                 '/admin/menus',          NULL, 13, TRUE),
('b0000000-0000-0000-0000-000000000014', 'ADMIN_CONFIG',     'Cấu hình Hệ thống',       'adjustments-horizontal', '/admin/system-config',  NULL, 14, TRUE)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    icon = EXCLUDED.icon,
    route = EXCLUDED.route,
    sort_order = EXCLUDED.sort_order,
    is_active = EXCLUDED.is_active;

-- ==============================================================================
-- 5. SEED ROLE_MENUS BAN ĐẦU THEO THIẾT KẾ PHÂN QUYỀN
-- ==============================================================================

-- 5.1. SUPER_ADMIN: Toàn quyền truy cập mọi menu quản trị nền tảng và chức năng dạy học
INSERT INTO role_menus (role_id, menu_id)
SELECT r.id, m.id
FROM roles r
CROSS JOIN menus m
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 5.2. CENTER_ADMIN & OWNER (Trung tâm): Thấy Dashboard, Lớp học, Học sinh, Điểm danh, Học phí VietQR, Cài đặt và Quản lý Giáo viên
INSERT INTO role_menus (role_id, menu_id)
SELECT r.id, m.id
FROM roles r
CROSS JOIN menus m
WHERE r.code IN ('CENTER_ADMIN', 'OWNER')
  AND m.code IN ('DASHBOARD', 'CLASSES', 'STUDENTS', 'ATTENDANCE', 'BILLING', 'SETTINGS', 'CENTER_TEACHERS')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 5.3. TEACHER (Giáo viên thuộc trung tâm): Tinh gọn - Chỉ thấy Dashboard, Lớp học, Học sinh, Điểm danh
INSERT INTO role_menus (role_id, menu_id)
SELECT r.id, m.id
FROM roles r
CROSS JOIN menus m
WHERE r.code = 'TEACHER'
  AND m.code IN ('DASHBOARD', 'CLASSES', 'STUDENTS', 'ATTENDANCE')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 5.4. SOLO_TUTOR (Gia sư tự do): Độc lập - Có toàn quyền lớp học, học sinh, điểm danh, hóa đơn và cài đặt riêng
INSERT INTO role_menus (role_id, menu_id)
SELECT r.id, m.id
FROM roles r
CROSS JOIN menus m
WHERE r.code = 'SOLO_TUTOR'
  AND m.code IN ('DASHBOARD', 'CLASSES', 'STUDENTS', 'ATTENDANCE', 'BILLING', 'SETTINGS')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ==============================================================================
-- 6. ĐẢM BẢO TẤT CẢ TÀI KHOẢN DEV / SUPERADMIN / DEMO KHÔNG BỊ HẾT HẠN
-- ==============================================================================
UPDATE tenants
SET subscription_status = 'ACTIVE',
    subscription_ends_at = NOW() + INTERVAL '365 days',
    trial_ends_at = NOW() + INTERVAL '365 days'
WHERE id IN (
    SELECT tenant_id FROM users WHERE phone IN ('0937712338', '0937712337', '0912345678', '0901112222', '0901112223', '0901112224', '0901112225', '0903334441', '0903334442')
);

INSERT INTO user_roles (id, tenant_id, user_id, role_id)
SELECT gen_random_uuid(), u.tenant_id, u.id, r.id
FROM users u, roles r
WHERE u.phone = '0937712338' AND r.code = 'SUPER_ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

UPDATE users
SET status = 'ACTIVE',
    password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy'
WHERE phone = '0937712338';


