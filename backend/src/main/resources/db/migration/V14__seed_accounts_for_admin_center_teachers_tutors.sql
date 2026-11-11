-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║     V14__seed_accounts_for_admin_center_teachers_tutors.sql                  ║
-- ║     EduPay SaaS - Khởi tạo tài khoản SuperAdmin, Trung tâm,                  ║
-- ║     3 Giáo viên trực thuộc Trung tâm và 2 Gia sư tự do                       ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

-- Hash mật khẩu chuẩn Admin@123: $2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy

-- ==============================================================================
-- 1. ĐẢM BẢO TÀI KHOẢN SUPERADMIN ĐÃ SẴN SÀNG
-- ==============================================================================
UPDATE users 
SET password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_until = NULL
WHERE phone = '0937712338';

-- ==============================================================================
-- 2. TẠO 1 TRUNG TÂM & TÀI KHOẢN CHỦ TRUNG TÂM (CENTER OWNER)
-- ==============================================================================
INSERT INTO tenants (
    id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at
) VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'Trung tâm Bồi dưỡng Văn hóa Ánh Dương',
    '0901112222',
    'anhduong.edu@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    'MONTHLY',
    'ACTIVE',
    NOW() + INTERVAL '30 days',
    NOW() + INTERVAL '90 days'
) ON CONFLICT (phone) DO UPDATE 
SET name = EXCLUDED.name, subscription_status = 'ACTIVE';

-- Tạo tài khoản Chủ trung tâm Ánh Dương
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status
) VALUES (
    'c0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000002',
    'Trần Nhật Quang (Chủ TT Ánh Dương)',
    '0901112222',
    'anhduong.edu@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    TRUE,
    'ACTIVE'
) ON CONFLICT (tenant_id, phone) DO UPDATE
SET full_name = EXCLUDED.full_name, password_hash = EXCLUDED.password_hash, status = 'ACTIVE';

-- Gán vai trò OWNER cho Chủ trung tâm
INSERT INTO user_roles (tenant_id, user_id, role_id)
SELECT 
    'a0000000-0000-0000-0000-000000000002',
    'c0000000-0000-0000-0000-000000000002',
    r.id
FROM roles r
WHERE r.code = 'OWNER' AND r.tenant_id IS NULL
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Cấu hình nhận tiền VietQR cho Trung tâm Ánh Dương
INSERT INTO payment_settings (
    id, tenant_id, bank_bin, bank_name, account_number, account_name, transfer_prefix
) VALUES (
    'b0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000002',
    '970415', -- VietinBank
    'VietinBank - Ngân hàng Công thương Việt Nam',
    '108876543210',
    'TRUNG TAM ANH DUONG',
    'AD'
) ON CONFLICT (tenant_id) DO NOTHING;

-- ==============================================================================
-- 3. TẠO 3 TÀI KHOẢN GIÁO VIÊN TRỰC THUỘC TRUNG TÂM ÁNH DƯƠNG
-- ==============================================================================

-- Giáo viên 1: Nguyễn Thu Hà (Toán)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status
) VALUES (
    'c0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000002',
    'Nguyễn Thu Hà (GV Toán - TT Ánh Dương)',
    '0901112223',
    'ha.nguyen@anhduong.edu.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    FALSE,
    'ACTIVE'
) ON CONFLICT (tenant_id, phone) DO UPDATE
SET full_name = EXCLUDED.full_name, password_hash = EXCLUDED.password_hash, status = 'ACTIVE';

-- Gán vai trò TEACHER cho GV 1
INSERT INTO user_roles (tenant_id, user_id, role_id)
SELECT 
    'a0000000-0000-0000-0000-000000000002',
    'c0000000-0000-0000-0000-000000000003',
    r.id
FROM roles r
WHERE r.code = 'TEACHER' AND r.tenant_id IS NULL
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Giáo viên 2: Phạm Minh Đức (Tiếng Anh)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status
) VALUES (
    'c0000000-0000-0000-0000-000000000004',
    'a0000000-0000-0000-0000-000000000002',
    'Phạm Minh Đức (GV Tiếng Anh - TT Ánh Dương)',
    '0901112224',
    'duc.pham@anhduong.edu.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    FALSE,
    'ACTIVE'
) ON CONFLICT (tenant_id, phone) DO UPDATE
SET full_name = EXCLUDED.full_name, password_hash = EXCLUDED.password_hash, status = 'ACTIVE';

-- Gán vai trò TEACHER cho GV 2
INSERT INTO user_roles (tenant_id, user_id, role_id)
SELECT 
    'a0000000-0000-0000-0000-000000000002',
    'c0000000-0000-0000-0000-000000000004',
    r.id
FROM roles r
WHERE r.code = 'TEACHER' AND r.tenant_id IS NULL
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Giáo viên 3: Đặng Quỳnh Nga (Vật Lý)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status
) VALUES (
    'c0000000-0000-0000-0000-000000000005',
    'a0000000-0000-0000-0000-000000000002',
    'Đặng Quỳnh Nga (GV Vật Lý - TT Ánh Dương)',
    '0901112225',
    'nga.dang@anhduong.edu.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    FALSE,
    'ACTIVE'
) ON CONFLICT (tenant_id, phone) DO UPDATE
SET full_name = EXCLUDED.full_name, password_hash = EXCLUDED.password_hash, status = 'ACTIVE';

-- Gán vai trò TEACHER cho GV 3
INSERT INTO user_roles (tenant_id, user_id, role_id)
SELECT 
    'a0000000-0000-0000-0000-000000000002',
    'c0000000-0000-0000-0000-000000000005',
    r.id
FROM roles r
WHERE r.code = 'TEACHER' AND r.tenant_id IS NULL
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ==============================================================================
-- 4. TẠO 2 TÀI KHOẢN GIA SƯ TỰ DO (ĐỘC LẬP / RIÊNG BIỆT)
-- ==============================================================================

-- Gia sư tự do 1: Lê Hoàng Long (Toán - Tin)
INSERT INTO tenants (
    id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at
) VALUES (
    'a0000000-0000-0000-0000-000000000003',
    'Lớp Gia Sư Thầy Long (Toán - Tin Tự Do)',
    '0903334441',
    'thaylong.toan@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    'MONTHLY',
    'ACTIVE',
    NOW() + INTERVAL '30 days',
    NOW() + INTERVAL '60 days'
) ON CONFLICT (phone) DO UPDATE 
SET name = EXCLUDED.name, subscription_status = 'ACTIVE';

INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status
) VALUES (
    'c0000000-0000-0000-0000-000000000006',
    'a0000000-0000-0000-0000-000000000003',
    'Lê Hoàng Long (Gia sư Tự Do)',
    '0903334441',
    'thaylong.toan@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    TRUE,
    'ACTIVE'
) ON CONFLICT (tenant_id, phone) DO UPDATE
SET full_name = EXCLUDED.full_name, password_hash = EXCLUDED.password_hash, status = 'ACTIVE';

INSERT INTO user_roles (tenant_id, user_id, role_id)
SELECT 
    'a0000000-0000-0000-0000-000000000003',
    'c0000000-0000-0000-0000-000000000006',
    r.id
FROM roles r
WHERE r.code = 'OWNER' AND r.tenant_id IS NULL
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Gia sư tự do 2: Vũ Mai Phương (IELTS)
INSERT INTO tenants (
    id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at
) VALUES (
    'a0000000-0000-0000-0000-000000000004',
    'Gia Sư Tiếng Anh Cô Mai Phương (Tự Do)',
    '0903334442',
    'maiphuong.ielts@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    'TRIAL',
    'ACTIVE',
    NOW() + INTERVAL '14 days',
    NOW() + INTERVAL '14 days'
) ON CONFLICT (phone) DO UPDATE 
SET name = EXCLUDED.name, subscription_status = 'ACTIVE';

INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status
) VALUES (
    'c0000000-0000-0000-0000-000000000007',
    'a0000000-0000-0000-0000-000000000004',
    'Vũ Mai Phương (Gia sư IELTS Tự Do)',
    '0903334442',
    'maiphuong.ielts@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    TRUE,
    'ACTIVE'
) ON CONFLICT (tenant_id, phone) DO UPDATE
SET full_name = EXCLUDED.full_name, password_hash = EXCLUDED.password_hash, status = 'ACTIVE';

INSERT INTO user_roles (tenant_id, user_id, role_id)
SELECT 
    'a0000000-0000-0000-0000-000000000004',
    'c0000000-0000-0000-0000-000000000007',
    r.id
FROM roles r
WHERE r.code = 'OWNER' AND r.tenant_id IS NULL
ON CONFLICT (user_id, role_id) DO NOTHING;
