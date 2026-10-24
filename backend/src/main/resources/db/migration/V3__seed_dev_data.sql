-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║     V3__seed_dev_data.sql                                                    ║
-- ║     EduPay SaaS - Dữ liệu mẫu khởi tạo cho môi trường Development           ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

-- 1. Tạo Tenant mẫu: Lớp Học Cô Mai
INSERT INTO tenants (
    id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Lớp Học Cô Mai - Dạy Kèm Toán',
    '0912345678',
    'comai.toan@edupay.vn',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8H7H6HjF4p1O8jB5hYJt8J8s5u7G6e', -- BCrypt hash cho 'Admin@123'
    'MONTHLY',
    'ACTIVE',
    NOW() + INTERVAL '30 days',
    NOW() + INTERVAL '30 days'
) ON CONFLICT (phone) DO NOTHING;

-- 2. Cấu hình Ngân hàng nhận tiền VietQR cho Cô Mai
INSERT INTO payment_settings (
    id, tenant_id, bank_bin, bank_name, account_number, account_name, transfer_prefix
) VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    '970422', -- MB Bank
    'MBBank - Ngân hàng Quân Đội',
    '0987654321',
    'NGUYEN THI MAI',
    'HP'
) ON CONFLICT (tenant_id) DO NOTHING;

-- 3. Tạo User Owner cho Cô Mai
INSERT INTO users (
    id, tenant_id, full_name, phone, email, password_hash, is_owner, status
) VALUES (
    'c0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'Nguyễn Thị Mai',
    '0912345678',
    'comai.toan@edupay.vn',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8H7H6HjF4p1O8jB5hYJt8J8s5u7G6e',
    TRUE,
    'ACTIVE'
) ON CONFLICT (tenant_id, phone) DO NOTHING;

-- Gán vai trò OWNER
INSERT INTO user_roles (tenant_id, user_id, role_id)
SELECT 
    'a0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000001',
    r.id
FROM roles r
WHERE r.code = 'OWNER' AND r.tenant_id IS NULL
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 4. Tạo Lớp học mẫu: Toán 8A Nâng cao
INSERT INTO classes (
    id, tenant_id, name, description, fee_type, default_fee, is_active
) VALUES (
    'd0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'Toán nâng cao 8A',
    'Lớp bồi dưỡng học sinh giỏi Toán lớp 8 (Thứ 3 - Thứ 7)',
    'PER_LESSON',
    150000.00,
    TRUE
) ON CONFLICT DO NOTHING;

-- 5. Tạo 4 học sinh mẫu đúng như giao diện Landing Page
INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
) VALUES 
(
    'e0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'Nguyễn Minh Anh',
    'Bé Bống',
    'Nguyễn Văn Tuấn',
    '0912111222',
    150000.00,
    'STUDYING'
),
(
    'e0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'Trần Gia Huy',
    'Cún',
    'Trần Văn Hùng',
    '0913222333',
    150000.00,
    'STUDYING'
),
(
    'e0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'Lê Bảo Ngọc',
    'Nhím',
    'Lê Thị Hương',
    '0914333444',
    150000.00,
    'STUDYING'
),
(
    'e0000000-0000-0000-0000-000000000004',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'Trần Minh Quân',
    'Bé Tít',
    'Trần Văn Long',
    '0915444555',
    150000.00,
    'STUDYING'
) ON CONFLICT DO NOTHING;

-- 6. Tạo một số lượt điểm danh mẫu
INSERT INTO attendance (
    tenant_id, class_id, student_id, lesson_date, status, note
) VALUES
('a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', CURRENT_DATE, 'PRESENT', NULL),
('a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', CURRENT_DATE, 'ABSENT_PERMITTED', 'Bị sốt nhẹ'),
('a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', CURRENT_DATE, 'PRESENT', NULL),
('a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000004', CURRENT_DATE, 'PRESENT', NULL)
ON CONFLICT (student_id, lesson_date) DO NOTHING;

-- 7. Tạo Phiếu thu mẫu tháng hiện tại cho học sinh Trần Minh Quân (Bé Tít)
INSERT INTO invoices (
    id, tenant_id, class_id, student_id, billing_month, attended_count, unit_price, extra_fee, discount, total_amount, transfer_code, public_hash, status, paid_method, note
) VALUES (
    'f0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000004',
    TO_CHAR(CURRENT_DATE, 'YYYY-MM'),
    8,
    150000.00,
    0.00,
    0.00,
    1200000.00,
    'HP-TOAN8A-TIT-004',
    'a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef0',
    'PENDING',
    'VIETQR',
    'Phiếu thu học phí tháng tự động'
) ON CONFLICT (transfer_code) DO NOTHING;
