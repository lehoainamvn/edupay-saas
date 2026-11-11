-- ==============================================================================
-- V13__assign_superadmin_to_demo_and_teacher_accounts.sql
-- Gán vai trò SUPER_ADMIN cho tài khoản giáo viên quản trị (0937712337 & 0912345678)
-- ==============================================================================

INSERT INTO user_roles (id, tenant_id, user_id, role_id)
SELECT gen_random_uuid(), u.tenant_id, u.id, r.id
FROM users u, roles r
WHERE u.phone IN ('0937712337', '0912345678') AND r.code = 'SUPER_ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);
