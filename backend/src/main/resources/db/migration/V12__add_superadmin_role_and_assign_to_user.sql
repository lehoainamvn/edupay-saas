-- ==============================================================================
-- V12__add_superadmin_role_and_assign_to_user.sql
-- Thêm vai trò SUPER_ADMIN và nâng cấp tài khoản quản trị viên thành SuperAdmin
-- ==============================================================================

-- 1. Tạo Role SUPER_ADMIN nếu chưa tồn tại
INSERT INTO roles (id, tenant_id, code, name, description, is_system, is_active)
SELECT 'e0000000-0000-0000-0000-000000000001', NULL, 'SUPER_ADMIN', 'Quan tri vien nen tang', 'Quan tri toi cao toan bo he thong EduPay', TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'SUPER_ADMIN' AND tenant_id IS NULL);

-- 2. Gán toàn bộ quyền (Permissions) trong hệ thống cho SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 3. Gán vai trò SUPER_ADMIN cho các tài khoản quản trị
INSERT INTO user_roles (id, tenant_id, user_id, role_id)
SELECT gen_random_uuid(), u.tenant_id, u.id, r.id
FROM users u, roles r
WHERE u.phone IN ('0937712338', '0937712337', '0912345678') AND r.code = 'SUPER_ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- 4. Đảm bảo trạng thái tài khoản hoạt động bình thường
UPDATE users 
SET status = 'ACTIVE' 
WHERE phone IN ('0937712338', '0937712337', '0912345678');
