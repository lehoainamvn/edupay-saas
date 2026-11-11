-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║     V9__rbac_feature_flags_and_dynamic_menu.sql                              ║
-- ║     EduPay SaaS - Multi-tier RBAC, Tenant/User Feature Flags & Dynamic Menus ║
-- ║     Flyway Migration | PostgreSQL 16                                         ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

-- ==============================================================================
-- 1. TẠO CÁC BẢNG FEATURE FLAGS RIÊNG THEO TENANT & USER
-- ==============================================================================

-- 1.1. Bảng Feature Flags ghi đè theo từng Tenant (Tenant Feature Overrides / Addons)
CREATE TABLE IF NOT EXISTS tenant_feature_flags (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    feature_id   VARCHAR(100) NOT NULL REFERENCES features(id) ON DELETE CASCADE,
    is_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    reason       VARCHAR(255),                            -- Lý do: 'MUA_ADDON_SEPAY', 'THU_NGHIEM_BETA'
    expires_at   TIMESTAMP WITH TIME ZONE,                -- NULL = Vĩnh viễn (theo chu kỳ thuê bao)
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_tenant_feature UNIQUE (tenant_id, feature_id)
);

CREATE INDEX IF NOT EXISTS idx_tenant_feature_flags_tenant ON tenant_feature_flags(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_feature_flags_feature ON tenant_feature_flags(feature_id);

-- 1.2. Bảng Feature Flags ghi đè theo từng User trong Tenant
CREATE TABLE IF NOT EXISTS user_feature_flags (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature_id   VARCHAR(100) NOT NULL REFERENCES features(id) ON DELETE CASCADE,
    is_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    reason       VARCHAR(255),
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_user_feature UNIQUE (user_id, feature_id)
);

CREATE INDEX IF NOT EXISTS idx_user_feature_flags_user ON user_feature_flags(user_id);
CREATE INDEX IF NOT EXISTS idx_user_feature_flags_tenant ON user_feature_flags(tenant_id);

-- ==============================================================================
-- 2. BỔ SUNG PERMISSIONS CÒN THIẾU
-- ==============================================================================

INSERT INTO permissions (code, name, resource, action, feature_id) VALUES
('system:config:read',   'Xem tham số hệ thống',            'system_config', 'read',   NULL),
('system:config:update', 'Sửa tham số hệ thống',            'system_config', 'update', NULL),
('feature:manage',       'Quản lý tính năng trung tâm',      'feature',       'update', NULL)
ON CONFLICT (code) DO NOTHING;

-- ==============================================================================
-- 3. BỔ SUNG & CẬP NHẬT DANH MỤC MENUS
-- ==============================================================================

-- Đảm bảo menu Tham số hệ thống có trong danh mục
INSERT INTO menus (code, name, icon, route, parent_id, sort_order, feature_id) VALUES
('SYSTEM_CONFIG', 'Tham số hệ thống', 'tune', '/system-config', NULL, 7, NULL)
ON CONFLICT (code) DO UPDATE SET 
    name = EXCLUDED.name,
    icon = EXCLUDED.icon,
    route = EXCLUDED.route,
    sort_order = EXCLUDED.sort_order;

-- Cập nhật thứ tự hiển thị chuẩn của các menu khác
UPDATE menus SET sort_order = 8, name = 'Cài đặt VietQR' WHERE code = 'SETTINGS';
UPDATE menus SET sort_order = 9 WHERE code = 'ADMIN_USERS';

-- ==============================================================================
-- 4. SEED MA TRẬN ROLE_PERMISSIONS CHO 3 VAI TRÒ HỆ THỐNG
-- ==============================================================================

-- 4.1. Vai trò OWNER: Toàn quyền trong phạm vi Tenant
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'OWNER' AND r.tenant_id IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 4.2. Vai trò TEACHER: Điểm danh, xem lớp, xem học sinh, xem học phí cơ bản
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'class:read',
    'student:read',
    'attendance:read',
    'attendance:create',
    'attendance:update',
    'invoice:read',
    'report:read'
)
WHERE r.code = 'TEACHER' AND r.tenant_id IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 4.3. Vai trò VIEWER: Chỉ có quyền đọc (Read-only)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'class:read',
    'student:read',
    'attendance:read',
    'invoice:read',
    'report:read'
)
WHERE r.code = 'VIEWER' AND r.tenant_id IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ==============================================================================
-- 5. SEED MENU_PERMISSIONS (ÁNH XẠ QUYỀN NÀO ĐƯỢC XEM MENU NÀO)
-- ==============================================================================

-- Menu DASHBOARD -> yêu cầu report:read
INSERT INTO menu_permissions (menu_id, permission_id)
SELECT m.id, p.id FROM menus m, permissions p
WHERE m.code = 'DASHBOARD' AND p.code = 'report:read'
ON CONFLICT (menu_id, permission_id) DO NOTHING;

-- Menu CLASSES -> yêu cầu class:read
INSERT INTO menu_permissions (menu_id, permission_id)
SELECT m.id, p.id FROM menus m, permissions p
WHERE m.code = 'CLASSES' AND p.code = 'class:read'
ON CONFLICT (menu_id, permission_id) DO NOTHING;

-- Menu STUDENTS -> yêu cầu student:read
INSERT INTO menu_permissions (menu_id, permission_id)
SELECT m.id, p.id FROM menus m, permissions p
WHERE m.code = 'STUDENTS' AND p.code = 'student:read'
ON CONFLICT (menu_id, permission_id) DO NOTHING;

-- Menu ATTENDANCE -> yêu cầu attendance:read
INSERT INTO menu_permissions (menu_id, permission_id)
SELECT m.id, p.id FROM menus m, permissions p
WHERE m.code = 'ATTENDANCE' AND p.code = 'attendance:read'
ON CONFLICT (menu_id, permission_id) DO NOTHING;

-- Menu BILLING -> yêu cầu invoice:read
INSERT INTO menu_permissions (menu_id, permission_id)
SELECT m.id, p.id FROM menus m, permissions p
WHERE m.code = 'BILLING' AND p.code = 'invoice:read'
ON CONFLICT (menu_id, permission_id) DO NOTHING;

-- Menu SETTINGS -> yêu cầu settings:read
INSERT INTO menu_permissions (menu_id, permission_id)
SELECT m.id, p.id FROM menus m, permissions p
WHERE m.code = 'SETTINGS' AND p.code = 'settings:read'
ON CONFLICT (menu_id, permission_id) DO NOTHING;

-- Menu SYSTEM_CONFIG -> yêu cầu system:config:read
INSERT INTO menu_permissions (menu_id, permission_id)
SELECT m.id, p.id FROM menus m, permissions p
WHERE m.code = 'SYSTEM_CONFIG' AND p.code = 'system:config:read'
ON CONFLICT (menu_id, permission_id) DO NOTHING;

-- Menu ADMIN_USERS -> yêu cầu user:read
INSERT INTO menu_permissions (menu_id, permission_id)
SELECT m.id, p.id FROM menus m, permissions p
WHERE m.code = 'ADMIN_USERS' AND p.code = 'user:read'
ON CONFLICT (menu_id, permission_id) DO NOTHING;

