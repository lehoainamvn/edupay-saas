-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║     V18__seed_complete_preconfigured_accounts.sql                            ║
-- ║     EduPay SaaS - Khởi tạo đầy đủ toàn bộ tài khoản demo, phân quyền & DB   ║
-- ║     Tất cả tài khoản dùng chung mật khẩu: Admin@123                          ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

-- BCrypt hash chuẩn cho mật khẩu 'Admin@123'
-- Hash: $2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy

-- ==============================================================================
-- 1. ĐẢM BẢO TẤT CẢ CÁC CƠ SỞ (TENANTS) ĐÃ SẴN SÀNG VỚI HẠN DÙNG DÀI HẠN
-- ==============================================================================

-- 1.1 Tenant cho Super Admin Platform (Lê Hoài Nam)
INSERT INTO tenants (
    id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at, tenant_type
) VALUES (
    'a0000000-0000-0000-0000-000000000099',
    'Hệ thống Quản trị EduPay Platform',
    '0937712338',
    'admin@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    'ANNUAL',
    'ACTIVE',
    NOW() + INTERVAL '3650 days',
    NOW() + INTERVAL '3650 days',
    'SOLO_TUTOR'
) ON CONFLICT (phone) DO UPDATE 
SET name = 'Hệ thống Quản trị EduPay Platform',
    subscription_status = 'ACTIVE',
    subscription_ends_at = NOW() + INTERVAL '3650 days';

-- 1.2 Tenant Quản trị Dự phòng (0937712337)
INSERT INTO tenants (
    id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at, tenant_type
) VALUES (
    'a0000000-0000-0000-0000-000000000098',
    'EduPay Co-Admin Dự phòng',
    '0937712337',
    'admin2@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    'ANNUAL',
    'ACTIVE',
    NOW() + INTERVAL '3650 days',
    NOW() + INTERVAL '3650 days',
    'SOLO_TUTOR'
) ON CONFLICT (phone) DO UPDATE 
SET subscription_status = 'ACTIVE',
    subscription_ends_at = NOW() + INTERVAL '3650 days';

-- 1.3 Tenant Trung tâm Ánh Dương (0901112222) - Mô hình CENTER
INSERT INTO tenants (
    id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at, tenant_type
) VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'Trung tâm Bồi dưỡng Văn hóa Ánh Dương',
    '0901112222',
    'anhduong.edu@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    'ANNUAL',
    'ACTIVE',
    NOW() + INTERVAL '3650 days',
    NOW() + INTERVAL '3650 days',
    'CENTER'
) ON CONFLICT (phone) DO UPDATE 
SET name = 'Trung tâm Bồi dưỡng Văn hóa Ánh Dương',
    tenant_type = 'CENTER',
    subscription_status = 'ACTIVE',
    subscription_ends_at = NOW() + INTERVAL '3650 days';

-- 1.4 Tenant Lớp Học Cô Mai (0912345678) - Mô hình SOLO_TUTOR
INSERT INTO tenants (
    id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at, tenant_type
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Lớp Học Cô Mai - Dạy Kèm Toán',
    '0912345678',
    'comai.toan@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    'ANNUAL',
    'ACTIVE',
    NOW() + INTERVAL '3650 days',
    NOW() + INTERVAL '3650 days',
    'SOLO_TUTOR'
) ON CONFLICT (phone) DO UPDATE 
SET tenant_type = 'SOLO_TUTOR',
    subscription_status = 'ACTIVE',
    subscription_ends_at = NOW() + INTERVAL '3650 days';

-- 1.5 Tenant Thầy Long Toán Tin (0903334441) - Mô hình SOLO_TUTOR
INSERT INTO tenants (
    id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at, tenant_type
) VALUES (
    'a0000000-0000-0000-0000-000000000003',
    'Lớp Gia Sư Thầy Long (Toán - Tin Tự Do)',
    '0903334441',
    'thaylong.toan@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    'ANNUAL',
    'ACTIVE',
    NOW() + INTERVAL '3650 days',
    NOW() + INTERVAL '3650 days',
    'SOLO_TUTOR'
) ON CONFLICT (phone) DO UPDATE 
SET tenant_type = 'SOLO_TUTOR',
    subscription_status = 'ACTIVE',
    subscription_ends_at = NOW() + INTERVAL '3650 days';

-- 1.6 Tenant Cô Mai Phương IELTS (0903334442) - Mô hình SOLO_TUTOR
INSERT INTO tenants (
    id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at, tenant_type
) VALUES (
    'a0000000-0000-0000-0000-000000000004',
    'Gia Sư Tiếng Anh Cô Mai Phương (Tự Do)',
    '0903334442',
    'maiphuong.ielts@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    'ANNUAL',
    'ACTIVE',
    NOW() + INTERVAL '3650 days',
    NOW() + INTERVAL '3650 days',
    'SOLO_TUTOR'
) ON CONFLICT (phone) DO UPDATE 
SET tenant_type = 'SOLO_TUTOR',
    subscription_status = 'ACTIVE',
    subscription_ends_at = NOW() + INTERVAL '3650 days';


-- ==============================================================================
-- 2. ĐẢM BẢO TẤT CẢ TÀI KHOẢN NGƯỜI DÙNG (USERS)
-- ==============================================================================

-- 2.1 Super Admin: Lê Hoài Nam (0937712338)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status, failed_login_attempts, locked_until
)
SELECT 
    'c0000000-0000-0000-0000-000000000099',
    t.id,
    'Lê Hoài Nam (Platform SuperAdmin)',
    '0937712338',
    'admin@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    TRUE,
    'ACTIVE',
    0,
    NULL
FROM tenants t WHERE t.phone = '0937712338'
ON CONFLICT (tenant_id, phone) DO UPDATE 
SET full_name = 'Lê Hoài Nam (Platform SuperAdmin)',
    password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_until = NULL;

-- 2.2 Admin Dự phòng: Lê Hoài Nam 2 (0937712337)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status, failed_login_attempts, locked_until
)
SELECT 
    'c0000000-0000-0000-0000-000000000098',
    t.id,
    'Lê Hoài Nam 2 (Admin Dự phòng)',
    '0937712337',
    'admin2@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    TRUE,
    'ACTIVE',
    0,
    NULL
FROM tenants t WHERE t.phone = '0937712337'
ON CONFLICT (tenant_id, phone) DO UPDATE 
SET password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_until = NULL;

-- 2.3 Chủ Trung tâm Ánh Dương: Trần Nhật Quang (0901112222)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status, failed_login_attempts, locked_until
)
SELECT 
    'c0000000-0000-0000-0000-000000000002',
    t.id,
    'Trần Nhật Quang (Chủ TT Ánh Dương)',
    '0901112222',
    'anhduong.edu@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    TRUE,
    'ACTIVE',
    0,
    NULL
FROM tenants t WHERE t.phone = '0901112222'
ON CONFLICT (tenant_id, phone) DO UPDATE 
SET full_name = 'Trần Nhật Quang (Chủ TT Ánh Dương)',
    password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_until = NULL;

-- 2.4 Giáo viên 1 (Toán): Nguyễn Thu Hà (0901112223)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status, failed_login_attempts, locked_until
)
SELECT 
    'c0000000-0000-0000-0000-000000000003',
    t.id,
    'Nguyễn Thu Hà (GV Toán - TT Ánh Dương)',
    '0901112223',
    'ha.nguyen@anhduong.edu.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    FALSE,
    'ACTIVE',
    0,
    NULL
FROM tenants t WHERE t.phone = '0901112222'
ON CONFLICT (tenant_id, phone) DO UPDATE 
SET full_name = 'Nguyễn Thu Hà (GV Toán - TT Ánh Dương)',
    password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_until = NULL;

-- 2.5 Giáo viên 2 (Tiếng Anh): Phạm Minh Đức (0901112224)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status, failed_login_attempts, locked_until
)
SELECT 
    'c0000000-0000-0000-0000-000000000004',
    t.id,
    'Phạm Minh Đức (GV Tiếng Anh - TT Ánh Dương)',
    '0901112224',
    'duc.pham@anhduong.edu.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    FALSE,
    'ACTIVE',
    0,
    NULL
FROM tenants t WHERE t.phone = '0901112222'
ON CONFLICT (tenant_id, phone) DO UPDATE 
SET full_name = 'Phạm Minh Đức (GV Tiếng Anh - TT Ánh Dương)',
    password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_until = NULL;

-- 2.6 Giáo viên 3 (Vật Lý): Đặng Quỳnh Nga (0901112225)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status, failed_login_attempts, locked_until
)
SELECT 
    'c0000000-0000-0000-0000-000000000005',
    t.id,
    'Đặng Quỳnh Nga (GV Vật Lý - TT Ánh Dương)',
    '0901112225',
    'nga.dang@anhduong.edu.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    FALSE,
    'ACTIVE',
    0,
    NULL
FROM tenants t WHERE t.phone = '0901112222'
ON CONFLICT (tenant_id, phone) DO UPDATE 
SET full_name = 'Đặng Quỳnh Nga (GV Vật Lý - TT Ánh Dương)',
    password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_until = NULL;

-- 2.7 Gia sư độc lập Cô Mai (0912345678)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status, failed_login_attempts, locked_until
)
SELECT 
    'c0000000-0000-0000-0000-000000000001',
    t.id,
    'Nguyễn Thị Mai (Cô Mai Dạy Toán)',
    '0912345678',
    'comai.toan@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    TRUE,
    'ACTIVE',
    0,
    NULL
FROM tenants t WHERE t.phone = '0912345678'
ON CONFLICT (tenant_id, phone) DO UPDATE 
SET full_name = 'Nguyễn Thị Mai (Cô Mai Dạy Toán)',
    password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_until = NULL;

-- 2.8 Gia sư Tự Do Thầy Long (0903334441)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status, failed_login_attempts, locked_until
)
SELECT 
    'c0000000-0000-0000-0000-000000000006',
    t.id,
    'Lê Hoàng Long (Gia sư Tự Do)',
    '0903334441',
    'thaylong.toan@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    TRUE,
    'ACTIVE',
    0,
    NULL
FROM tenants t WHERE t.phone = '0903334441'
ON CONFLICT (tenant_id, phone) DO UPDATE 
SET full_name = 'Lê Hoàng Long (Gia sư Tự Do)',
    password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_until = NULL;

-- 2.9 Gia sư Tự Do Cô Mai Phương (0903334442)
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status, failed_login_attempts, locked_until
)
SELECT 
    'c0000000-0000-0000-0000-000000000007',
    t.id,
    'Vũ Mai Phương (Gia sư IELTS Tự Do)',
    '0903334442',
    'maiphuong.ielts@edupay.vn',
    '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    TRUE,
    'ACTIVE',
    0,
    NULL
FROM tenants t WHERE t.phone = '0903334442'
ON CONFLICT (tenant_id, phone) DO UPDATE 
SET full_name = 'Vũ Mai Phương (Gia sư IELTS Tự Do)',
    password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_until = NULL;


-- ==============================================================================
-- 3. PHÂN VAI TRÒ (USER_ROLES) CHUẨN XÁC CHO TỪNG TÀI KHOẢN
-- ==============================================================================

-- 3.1 SUPER ADMIN (0937712338) có quyền SUPER_ADMIN, OWNER, CENTER_ADMIN, SOLO_TUTOR
INSERT INTO user_roles (id, tenant_id, user_id, role_id)
SELECT gen_random_uuid(), u.tenant_id, u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.phone = '0937712338'
  AND r.tenant_id IS NULL
  AND r.code IN ('SUPER_ADMIN', 'OWNER', 'CENTER_ADMIN', 'SOLO_TUTOR')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 3.2 SUPER ADMIN 2 (0937712337)
INSERT INTO user_roles (id, tenant_id, user_id, role_id)
SELECT gen_random_uuid(), u.tenant_id, u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.phone = '0937712337'
  AND r.tenant_id IS NULL
  AND r.code IN ('SUPER_ADMIN', 'OWNER')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 3.3 CHỦ TRUNG TÂM ÁNH DƯƠNG (0901112222)
INSERT INTO user_roles (id, tenant_id, user_id, role_id)
SELECT gen_random_uuid(), u.tenant_id, u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.phone = '0901112222'
  AND r.tenant_id IS NULL
  AND r.code IN ('OWNER', 'CENTER_ADMIN')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 3.4 BA GIÁO VIÊN TRỰC THUỘC ÁNH DƯƠNG (0901112223, 0901112224, 0901112225)
INSERT INTO user_roles (id, tenant_id, user_id, role_id)
SELECT gen_random_uuid(), u.tenant_id, u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.phone IN ('0901112223', '0901112224', '0901112225')
  AND r.tenant_id IS NULL
  AND r.code = 'TEACHER'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 3.5 GIA SƯ ĐỘC LẬP (0912345678, 0903334441, 0903334442)
INSERT INTO user_roles (id, tenant_id, user_id, role_id)
SELECT gen_random_uuid(), u.tenant_id, u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.phone IN ('0912345678', '0903334441', '0903334442')
  AND r.tenant_id IS NULL
  AND r.code IN ('OWNER', 'SOLO_TUTOR')
ON CONFLICT (user_id, role_id) DO NOTHING;


-- ==============================================================================
-- 4. CẤU HÌNH TÀI KHOẢN NGÂN HÀNG VIETQR (PAYMENT_SETTINGS)
-- ==============================================================================

-- 4.1 VietQR cho SuperAdmin
INSERT INTO payment_settings (
    id, tenant_id, bank_bin, bank_name, account_number, account_name, transfer_prefix
)
SELECT 
    'b0000000-0000-0000-0000-000000000099',
    t.id,
    '970436', -- Vietcombank
    'Vietcombank - Ngân hàng Ngoại Thương Việt Nam',
    '9937712338',
    'LE HOAI NAM',
    'EDUPAY'
FROM tenants t WHERE t.phone = '0937712338'
ON CONFLICT (tenant_id) DO UPDATE 
SET bank_bin = EXCLUDED.bank_bin, bank_name = EXCLUDED.bank_name, account_number = EXCLUDED.account_number, account_name = EXCLUDED.account_name;

-- 4.2 VietQR cho Trung tâm Ánh Dương
INSERT INTO payment_settings (
    id, tenant_id, bank_bin, bank_name, account_number, account_name, transfer_prefix
)
SELECT 
    'b0000000-0000-0000-0000-000000000002',
    t.id,
    '970415', -- VietinBank
    'VietinBank - Ngân hàng Công thương Việt Nam',
    '108876543210',
    'TRUNG TAM ANH DUONG',
    'AD'
FROM tenants t WHERE t.phone = '0901112222'
ON CONFLICT (tenant_id) DO UPDATE 
SET bank_bin = EXCLUDED.bank_bin, bank_name = EXCLUDED.bank_name, account_number = EXCLUDED.account_number, account_name = EXCLUDED.account_name;

-- 4.3 VietQR cho Cô Mai
INSERT INTO payment_settings (
    id, tenant_id, bank_bin, bank_name, account_number, account_name, transfer_prefix
)
SELECT 
    'b0000000-0000-0000-0000-000000000001',
    t.id,
    '970422', -- MB Bank
    'MBBank - Ngân hàng Quân Đội',
    '0987654321',
    'NGUYEN THI MAI',
    'HP'
FROM tenants t WHERE t.phone = '0912345678'
ON CONFLICT (tenant_id) DO UPDATE 
SET bank_bin = EXCLUDED.bank_bin, bank_name = EXCLUDED.bank_name, account_number = EXCLUDED.account_number, account_name = EXCLUDED.account_name;

-- 4.4 VietQR cho Thầy Long (Toán Tin)
INSERT INTO payment_settings (
    id, tenant_id, bank_bin, bank_name, account_number, account_name, transfer_prefix
)
SELECT 
    'b0000000-0000-0000-0000-000000000003',
    t.id,
    '970407', -- Techcombank
    'Techcombank - Ngân hàng Kỹ thương Việt Nam',
    '190333444122',
    'LE HOANG LONG',
    'LONGTOAN'
FROM tenants t WHERE t.phone = '0903334441'
ON CONFLICT (tenant_id) DO UPDATE 
SET bank_bin = EXCLUDED.bank_bin, bank_name = EXCLUDED.bank_name, account_number = EXCLUDED.account_number, account_name = EXCLUDED.account_name;

-- 4.5 VietQR cho Cô Mai Phương (IELTS)
INSERT INTO payment_settings (
    id, tenant_id, bank_bin, bank_name, account_number, account_name, transfer_prefix
)
SELECT 
    'b0000000-0000-0000-0000-000000000004',
    t.id,
    '970416', -- ACB
    'ACB - Ngân hàng TMCP Á Châu',
    '8899334422',
    'VU MAI PHUONG',
    'IELTSMP'
FROM tenants t WHERE t.phone = '0903334442'
ON CONFLICT (tenant_id) DO UPDATE 
SET bank_bin = EXCLUDED.bank_bin, bank_name = EXCLUDED.bank_name, account_number = EXCLUDED.account_number, account_name = EXCLUDED.account_name;


-- ==============================================================================
-- 5. CHÍNH SÁCH PHÂN QUYỀN GIÁO VIÊN CHO CÁC TRUNG TÂM (TENANT_TEACHER_POLICIES)
-- ==============================================================================
INSERT INTO tenant_teacher_policies (tenant_id, can_edit_student_fee, can_create_class, can_create_student, can_view_billing)
SELECT t.id, FALSE, FALSE, FALSE, FALSE
FROM tenants t
WHERE t.tenant_type = 'CENTER'
ON CONFLICT (tenant_id) DO NOTHING;


-- ==============================================================================
-- 6. KHỞI TẠO LỚP HỌC MẪU CHO TỪNG GIÁO VIÊN VÀ GIA SƯ
-- ==============================================================================

-- 6.1 Lớp của Trung tâm Ánh Dương (3 lớp tương ứng 3 giáo viên)
INSERT INTO classes (
    id, tenant_id, name, description, fee_type, default_fee, schedule_days, start_time, end_time, is_active, teacher_id
)
SELECT 
    'd0000000-0000-0000-0000-000000000001',
    t.id,
    'Toán Nâng Cao 10 - AD01',
    'Lớp chuyên đề Toán 10 do Cô Thu Hà trực tiếp phụ trách.',
    'PER_LESSON',
    120000.00,
    '2,4,6',
    '18:00',
    '19:30',
    TRUE,
    u.id
FROM tenants t
JOIN users u ON u.tenant_id = t.id AND u.phone = '0901112223'
WHERE t.phone = '0901112222'
ON CONFLICT (id) DO UPDATE 
SET name = EXCLUDED.name, teacher_id = EXCLUDED.teacher_id, default_fee = EXCLUDED.default_fee;

INSERT INTO classes (
    id, tenant_id, name, description, fee_type, default_fee, schedule_days, start_time, end_time, is_active, teacher_id
)
SELECT 
    'd0000000-0000-0000-0000-000000000002',
    t.id,
    'Tiếng Anh THPT Quốc Gia - AD02',
    'Lớp Luyện đề Anh văn THPT do Thầy Minh Đức phụ trách.',
    'PER_LESSON',
    140000.00,
    '3,5,7',
    '19:30',
    '21:00',
    TRUE,
    u.id
FROM tenants t
JOIN users u ON u.tenant_id = t.id AND u.phone = '0901112224'
WHERE t.phone = '0901112222'
ON CONFLICT (id) DO UPDATE 
SET name = EXCLUDED.name, teacher_id = EXCLUDED.teacher_id, default_fee = EXCLUDED.default_fee;

INSERT INTO classes (
    id, tenant_id, name, description, fee_type, default_fee, schedule_days, start_time, end_time, is_active, teacher_id
)
SELECT 
    'd0000000-0000-0000-0000-000000000003',
    t.id,
    'Vật Lý Chuyên Sâu 12 - AD03',
    'Lớp Vật lý nâng cao lớp 12 do Cô Quỳnh Nga phụ trách.',
    'PER_LESSON',
    150000.00,
    'CN',
    '08:30',
    '11:00',
    TRUE,
    u.id
FROM tenants t
JOIN users u ON u.tenant_id = t.id AND u.phone = '0901112225'
WHERE t.phone = '0901112222'
ON CONFLICT (id) DO UPDATE 
SET name = EXCLUDED.name, teacher_id = EXCLUDED.teacher_id, default_fee = EXCLUDED.default_fee;

-- 6.2 Lớp của Thầy Long (Toán Tin Tự Do)
INSERT INTO classes (
    id, tenant_id, name, description, fee_type, default_fee, schedule_days, start_time, end_time, is_active, teacher_id
)
SELECT 
    'd0000000-0000-0000-0000-000000000004',
    t.id,
    'Toán & Lập trình Pascal/Python 9',
    'Lớp bồi dưỡng học sinh thi vào lớp 10 chuyên Tin.',
    'PER_LESSON',
    130000.00,
    '2,6',
    '18:30',
    '20:30',
    TRUE,
    u.id
FROM tenants t
JOIN users u ON u.tenant_id = t.id AND u.phone = '0903334441'
WHERE t.phone = '0903334441'
ON CONFLICT (id) DO UPDATE 
SET name = EXCLUDED.name, teacher_id = EXCLUDED.teacher_id;

-- 6.3 Lớp của Cô Mai Phương (IELTS Tự Do)
INSERT INTO classes (
    id, tenant_id, name, description, fee_type, default_fee, schedule_days, start_time, end_time, is_active, teacher_id
)
SELECT 
    'd0000000-0000-0000-0000-000000000005',
    t.id,
    'IELTS Master Band 6.5+ Cấp Tốc',
    'Lớp gia sư kèm 1-4 học viên tăng band điểm Writing & Speaking.',
    'PER_LESSON',
    180000.00,
    '3,5,7',
    '19:00',
    '20:30',
    TRUE,
    u.id
FROM tenants t
JOIN users u ON u.tenant_id = t.id AND u.phone = '0903334442'
WHERE t.phone = '0903334442'
ON CONFLICT (id) DO UPDATE 
SET name = EXCLUDED.name, teacher_id = EXCLUDED.teacher_id;


-- ==============================================================================
-- 7. KHỞI TẠO HỌC SINH MẪU CHO TỪNG LỚP HỌC (STUDENTS)
-- ==============================================================================

-- 7.1 Học sinh Trung tâm Ánh Dương - Lớp Toán (Cô Thu Hà phụ trách)
INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
)
SELECT 
    'e0000000-0000-0000-0000-000000000011',
    t.id,
    'd0000000-0000-0000-0000-000000000001',
    'Nguyễn Hoàng Long',
    'Long Nhỏ',
    'Nguyễn Văn Hùng',
    '0981234567',
    120000.00,
    'STUDYING'
FROM tenants t WHERE t.phone = '0901112222'
ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name;

INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
)
SELECT 
    'e0000000-0000-0000-0000-000000000012',
    t.id,
    'd0000000-0000-0000-0000-000000000001',
    'Trần Bảo Ngọc',
    'Bảo Ngọc',
    'Lê Thị Thu',
    '0987654321',
    120000.00,
    'STUDYING'
FROM tenants t WHERE t.phone = '0901112222'
ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name;

-- 7.2 Học sinh Trung tâm Ánh Dương - Lớp Tiếng Anh (Thầy Minh Đức phụ trách)
INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
)
SELECT 
    'e0000000-0000-0000-0000-000000000013',
    t.id,
    'd0000000-0000-0000-0000-000000000002',
    'Lê Tuấn Kiệt',
    'Kiệt',
    'Lê Văn Tuấn',
    '0912345678',
    140000.00,
    'STUDYING'
FROM tenants t WHERE t.phone = '0901112222'
ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name;

INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
)
SELECT 
    'e0000000-0000-0000-0000-000000000014',
    t.id,
    'd0000000-0000-0000-0000-000000000002',
    'Hoàng Minh Triết',
    'Triết',
    'Hoàng Thị Mai',
    '0977112233',
    140000.00,
    'STUDYING'
FROM tenants t WHERE t.phone = '0901112222'
ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name;

-- 7.3 Học sinh Trung tâm Ánh Dương - Lớp Vật Lý (Cô Quỳnh Nga phụ trách)
INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
)
SELECT 
    'e0000000-0000-0000-0000-000000000015',
    t.id,
    'd0000000-0000-0000-0000-000000000003',
    'Phạm Minh Anh',
    'Minh Anh',
    'Phạm Văn Hòa',
    '0933338888',
    150000.00,
    'STUDYING'
FROM tenants t WHERE t.phone = '0901112222'
ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name;

-- 7.4 Học sinh Lớp Thầy Long (Gia sư Toán Tin)
INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
)
SELECT 
    'e0000000-0000-0000-0000-000000000021',
    t.id,
    'd0000000-0000-0000-0000-000000000004',
    'Bùi Gia Huy',
    'Huy Béo',
    'Bùi Văn Bình',
    '0918776655',
    130000.00,
    'STUDYING'
FROM tenants t WHERE t.phone = '0903334441'
ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name;

INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
)
SELECT 
    'e0000000-0000-0000-0000-000000000022',
    t.id,
    'd0000000-0000-0000-0000-000000000004',
    'Phan Hải Đăng',
    'Hải Đăng',
    'Phan Thanh Tú',
    '0919223344',
    130000.00,
    'STUDYING'
FROM tenants t WHERE t.phone = '0903334441'
ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name;

-- 7.5 Học sinh Lớp Cô Mai Phương (Gia sư IELTS)
INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
)
SELECT 
    'e0000000-0000-0000-0000-000000000031',
    t.id,
    'd0000000-0000-0000-0000-000000000005',
    'Trịnh Mỹ Linh',
    'Linh Miu',
    'Trịnh Quốc Toàn',
    '0988665544',
    180000.00,
    'STUDYING'
FROM tenants t WHERE t.phone = '0903334442'
ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name;

INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
)
SELECT 
    'e0000000-0000-0000-0000-000000000032',
    t.id,
    'd0000000-0000-0000-0000-000000000005',
    'Đỗ Anh Khoa',
    'Khoa Tí',
    'Đỗ Trọng Tấn',
    '0977889900',
    180000.00,
    'STUDYING'
FROM tenants t WHERE t.phone = '0903334442'
ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name;


-- ==============================================================================
-- 8. KHỞI TẠO ĐIỂM DANH MẪU (ATTENDANCE) ĐỂ CÓ THỐNG KÊ LỊCH HỌC
-- ==============================================================================
INSERT INTO attendance (tenant_id, class_id, student_id, lesson_date, status, note)
SELECT s.tenant_id, s.class_id, s.id, CURRENT_DATE - INTERVAL '1 day', 'PRESENT', 'Đúng giờ'
FROM students s
WHERE s.id IN (
    'e0000000-0000-0000-0000-000000000011',
    'e0000000-0000-0000-0000-000000000013',
    'e0000000-0000-0000-0000-000000000015',
    'e0000000-0000-0000-0000-000000000021',
    'e0000000-0000-0000-0000-000000000031'
)
ON CONFLICT (student_id, lesson_date) DO NOTHING;

INSERT INTO attendance (tenant_id, class_id, student_id, lesson_date, status, note)
SELECT s.tenant_id, s.class_id, s.id, CURRENT_DATE - INTERVAL '1 day', 'ABSENT_PERMITTED', 'Xin nghỉ ốm'
FROM students s
WHERE s.id IN (
    'e0000000-0000-0000-0000-000000000012',
    'e0000000-0000-0000-0000-000000000022'
)
ON CONFLICT (student_id, lesson_date) DO NOTHING;


-- ==============================================================================
-- 9. KHỞI TẠO PHIẾU THU HỌC PHÍ MẪU (INVOICES) CHO CÁC CƠ SỞ
-- ==============================================================================

-- 9.1 Phiếu thu cho học sinh Lớp Toán - Ánh Dương (Đã thanh toán)
INSERT INTO invoices (
    id, tenant_id, class_id, student_id, billing_month, attended_count, unit_price, extra_fee, discount, total_amount, transfer_code, public_hash, status, paid_method, paid_at, note
)
SELECT 
    'f0000000-0000-0000-0000-000000000011',
    s.tenant_id,
    s.class_id,
    s.id,
    TO_CHAR(CURRENT_DATE, 'YYYY-MM'),
    8,
    120000.00,
    0.00,
    0.00,
    960000.00,
    'AD-TOAN10-LONG-011',
    'a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef1',
    'PAID',
    'VIETQR',
    NOW() - INTERVAL '2 days',
    'Đã thanh toán học phí qua Napas247 VietQR'
FROM students s WHERE s.id = 'e0000000-0000-0000-0000-000000000011'
ON CONFLICT (transfer_code) DO NOTHING;

-- 9.2 Phiếu thu cho học sinh Lớp Toán - Ánh Dương (Chờ thanh toán)
INSERT INTO invoices (
    id, tenant_id, class_id, student_id, billing_month, attended_count, unit_price, extra_fee, discount, total_amount, transfer_code, public_hash, status, paid_method, note
)
SELECT 
    'f0000000-0000-0000-0000-000000000012',
    s.tenant_id,
    s.class_id,
    s.id,
    TO_CHAR(CURRENT_DATE, 'YYYY-MM'),
    8,
    120000.00,
    0.00,
    0.00,
    960000.00,
    'AD-TOAN10-NGOC-012',
    'a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef2',
    'PENDING',
    'VIETQR',
    'Học phí tháng tự động chốt'
FROM students s WHERE s.id = 'e0000000-0000-0000-0000-000000000012'
ON CONFLICT (transfer_code) DO NOTHING;

-- 9.3 Phiếu thu cho Thầy Long (Gia sư Tin) - Chờ thanh toán
INSERT INTO invoices (
    id, tenant_id, class_id, student_id, billing_month, attended_count, unit_price, extra_fee, discount, total_amount, transfer_code, public_hash, status, paid_method, note
)
SELECT 
    'f0000000-0000-0000-0000-000000000021',
    s.tenant_id,
    s.class_id,
    s.id,
    TO_CHAR(CURRENT_DATE, 'YYYY-MM'),
    6,
    130000.00,
    0.00,
    0.00,
    780000.00,
    'LONGTOAN-HUY-021',
    'a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef3',
    'PENDING',
    'VIETQR',
    'Phiếu thu kèm 1-1 ôn thi Chuyên Tin'
FROM students s WHERE s.id = 'e0000000-0000-0000-0000-000000000021'
ON CONFLICT (transfer_code) DO NOTHING;

-- 9.4 Phiếu thu cho Cô Mai Phương (IELTS) - Đã thanh toán
INSERT INTO invoices (
    id, tenant_id, class_id, student_id, billing_month, attended_count, unit_price, extra_fee, discount, total_amount, transfer_code, public_hash, status, paid_method, paid_at, note
)
SELECT 
    'f0000000-0000-0000-0000-000000000031',
    s.tenant_id,
    s.class_id,
    s.id,
    TO_CHAR(CURRENT_DATE, 'YYYY-MM'),
    8,
    180000.00,
    0.00,
    0.00,
    1440000.00,
    'IELTSMP-LINH-031',
    'a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef4',
    'PAID',
    'VIETQR',
    NOW() - INTERVAL '1 day',
    'Khóa ôn IELTS Writing & Speaking'
FROM students s WHERE s.id = 'e0000000-0000-0000-0000-000000000031'
ON CONFLICT (transfer_code) DO NOTHING;


-- ==============================================================================
-- 10. SEED ĐẦY ĐỦ 13 THAM SỐ CẤU HÌNH ĐỘNG CHO TẤT CẢ CÁC TENANT
-- ==============================================================================
INSERT INTO system_parameters (tenant_id, param_key, param_value, param_group, data_type, display_name, description, is_system)
SELECT t.id, p.param_key, p.param_value, p.param_group, p.data_type, p.display_name, p.description, p.is_system
FROM tenants t
CROSS JOIN (
    VALUES
        -- NHÓM 1: HỆ THỐNG (SYSTEM)
        ('SYSTEM_CENTER_NAME', 'Hệ thống Quản lý EduPay', 'SYSTEM', 'STRING', 'Tên cơ sở / Trung tâm hiển thị', 'Tên trung tâm hoặc thương hiệu gia sư hiển thị trên biên nhận học phí và cổng phụ huynh', TRUE),
        ('SYSTEM_AUTO_LOGOUT_HOURS', '8', 'SYSTEM', 'NUMBER', 'Thời gian tự động đăng xuất (giờ)', 'Thời lượng phiên làm việc trước khi tài khoản tự động đăng xuất để bảo mật (1 - 72 giờ)', FALSE),
        ('SYSTEM_ALLOW_TEACHER_EDIT_FEE', 'true', 'SYSTEM', 'BOOLEAN', 'Cho phép giáo viên sửa mức học phí', 'Cho phép tài khoản giáo viên điều chỉnh mức học phí buổi học khi điểm danh', FALSE),
        ('DEFAULT_TRANSFER_PREFIX', 'EDUPAY', 'SYSTEM', 'STRING', 'Tiền tố mã chuyển khoản VietQR', 'Tiền tố sinh tự động trên cú pháp chuyển khoản của phiếu thu học phí', TRUE),
        ('MAX_STUDENTS_PER_CLASS', '50', 'SYSTEM', 'NUMBER', 'Sĩ số học sinh tối đa mỗi lớp', 'Giới hạn số lượng học sinh tối đa được phép thêm vào một lớp học', FALSE),
        
        -- NHÓM 2: HỌC PHÍ (TUITION)
        ('TUITION_DUE_DAYS', '7', 'TUITION', 'NUMBER', 'Hạn thanh toán học phí (ngày)', 'Số ngày phụ huynh được phép hoàn tất thanh toán kể từ ngày phát hành phiếu thu', FALSE),
        ('TUITION_AUTO_CREATE_DAY', '28', 'TUITION', 'NUMBER', 'Ngày tự động chốt học phí trong tháng', 'Ngày hệ thống tự động tổng hợp số buổi điểm danh và phát hành phiếu thu hàng tháng (1 - 31)', FALSE),
        ('TUITION_OVERDUE_POLICY', 'WARN_ONLY', 'TUITION', 'STRING', 'Chính sách xử lý khi quá hạn học phí', 'Quy định hình thức cảnh báo khi học sinh chưa đóng học phí đúng hạn', FALSE),

        -- NHÓM 3: THÔNG BÁO (NOTIFICATION)
        ('NOTIFICATION_AUTO_REMINDER', 'true', 'NOTIFICATION', 'BOOLEAN', 'Tự động nhắc nợ trước ngày hết hạn', 'Tự động kích hoạt tin nhắn nhắc nhở phụ huynh khi sắp đến hạn thanh toán', FALSE),
        ('NOTIFICATION_REMINDER_BEFORE_DAYS', '2', 'NOTIFICATION', 'NUMBER', 'Số ngày nhắc trước hạn thanh toán', 'Số ngày gửi thông báo nhắc nhở trước khi phiếu thu học phí đến hạn', FALSE),
        ('NOTIFICATION_TEMPLATE_INVOICE', 'Kính gửi Quý Phụ huynh, học phí tháng {month} của em {studentName} là {amount}đ. Vui lòng thanh toán qua link VietQR: {invoiceUrl}. Trân trọng cảm ơn!', 'NOTIFICATION', 'STRING', 'Mẫu tin nhắn gửi phiếu thu học phí', 'Nội dung tin nhắn tự động khi gửi thông báo phiếu thu qua Zalo hoặc SMS cho phụ huynh', FALSE),

        -- NHÓM 4: BẢO MẬT (SECURITY)
        ('SECURITY_MAX_LOGIN_ATTEMPTS', '5', 'SECURITY', 'NUMBER', 'Số lần đăng nhập sai tối đa', 'Số lần nhập sai mật khẩu liên tiếp trước khi tài khoản bị tạm khóa để chống tấn công brute-force', TRUE),
        ('SECURITY_LOCKOUT_MINUTES', '15', 'SECURITY', 'NUMBER', 'Thời gian khóa tài khoản (phút)', 'Thời gian tạm khóa tài khoản sau khi vượt quá số lần đăng nhập sai', TRUE)
) AS p(param_key, param_value, param_group, data_type, display_name, description, is_system)
ON CONFLICT (tenant_id, param_key) DO NOTHING;
