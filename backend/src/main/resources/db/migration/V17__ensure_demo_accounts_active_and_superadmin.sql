-- ==============================================================================
-- V17__ensure_demo_accounts_active_and_superadmin.sql
-- Kích hoạt vĩnh viễn hạn dùng gói cho các tài khoản Quản trị & Demo
-- Gán role SUPER_ADMIN cho tài khoản 0937712338
-- ==============================================================================

-- 1. Đảm bảo toàn bộ tài khoản Admin, Trung tâm, Gia sư không bị chặn hết hạn
UPDATE tenants
SET subscription_status = 'ACTIVE',
    subscription_ends_at = NOW() + INTERVAL '3650 days',
    trial_ends_at = NOW() + INTERVAL '3650 days'
WHERE id IN (
    SELECT tenant_id FROM users 
    WHERE phone IN ('0937712338', '0937712337', '0912345678', '0901112222', '0901112223', '0901112224', '0901112225', '0903334441', '0903334442')
);

-- 2. Đảm bảo user 0937712338 có role SUPER_ADMIN trong user_roles
INSERT INTO user_roles (id, tenant_id, user_id, role_id)
SELECT gen_random_uuid(), u.tenant_id, u.id, r.id
FROM users u, roles r
WHERE u.phone = '0937712338' AND r.code = 'SUPER_ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- 3. Reset mật khẩu và trạng thái hoạt động cho 0937712338
UPDATE users
SET status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_until = NULL,
    password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy'
WHERE phone = '0937712338';

