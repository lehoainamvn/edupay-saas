-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║     V15__task3_center_multi_user_and_solo_tutor.sql                         ║
-- ║     EduPay SaaS - TASK 3: Mô hình phân quyền "Trung tâm" vs "Gia sư tự do"   ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

-- ==============================================================================
-- 1. BỔ SUNG TRƯỜNG PHÂN LOẠI TENANT (CENTER vs SOLO_TUTOR)
-- ==============================================================================
ALTER TABLE tenants 
ADD COLUMN IF NOT EXISTS tenant_type VARCHAR(20) DEFAULT 'SOLO_TUTOR';

-- Cập nhật Trung tâm Bồi dưỡng Văn hóa Ánh Dương là loại CENTER (Trung tâm nhiều GV)
UPDATE tenants 
SET tenant_type = 'CENTER' 
WHERE id = 'a0000000-0000-0000-0000-000000000002';

-- Đảm bảo các tài khoản gia sư tự do là SOLO_TUTOR
UPDATE tenants 
SET tenant_type = 'SOLO_TUTOR' 
WHERE id != 'a0000000-0000-0000-0000-000000000002';

-- ==============================================================================
-- 2. BỔ SUNG TRƯỜNG PHÂN CÔNG GIÁO VIÊN VÀO LỚP HỌC (teacher_id)
-- ==============================================================================
ALTER TABLE classes 
ADD COLUMN IF NOT EXISTS teacher_id UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_classes_teacher ON classes(tenant_id, teacher_id);

-- ==============================================================================
-- 3. BỔ SUNG DANH MỤC PERMISSIONS CHO QUẢN LÝ GIÁO VIÊN TRUNG TÂM
-- ==============================================================================
INSERT INTO permissions (code, name, resource, action, feature_id) VALUES
('teacher:read',   'Xem danh sách giáo viên trung tâm', 'teacher', 'read',   NULL),
('teacher:create', 'Thêm giáo viên mới vào trung tâm',  'teacher', 'create', NULL),
('teacher:update', 'Cập nhật giáo viên trung tâm',      'teacher', 'update', NULL),
('teacher:delete', 'Xóa hoặc khóa giáo viên trung tâm', 'teacher', 'delete', NULL)
ON CONFLICT (code) DO UPDATE 
SET name = EXCLUDED.name, resource = EXCLUDED.resource, action = EXCLUDED.action;

-- Gán toàn bộ quyền teacher:* cho Role OWNER (Chủ trung tâm)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'OWNER' 
  AND p.code IN ('teacher:read', 'teacher:create', 'teacher:update', 'teacher:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Gán toàn bộ quyền teacher:* cho Role SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN' 
  AND p.code IN ('teacher:read', 'teacher:create', 'teacher:update', 'teacher:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ==============================================================================
-- 4. BỔ SUNG CÂY MENU "QUẢN LÝ GIÁO VIÊN" (/center/teachers)
-- ==============================================================================
INSERT INTO menus (code, name, icon, route, parent_id, sort_order, feature_id) VALUES
('CENTER_TEACHERS', 'Quản lý giáo viên', 'groups', '/center/teachers', NULL, 8, NULL)
ON CONFLICT (code) DO UPDATE 
SET name = EXCLUDED.name, icon = EXCLUDED.icon, route = EXCLUDED.route;

-- Phân quyền cho menu CENTER_TEACHERS
INSERT INTO menu_permissions (menu_id, permission_id)
SELECT m.id, p.id 
FROM menus m
JOIN permissions p ON p.code = 'teacher:read'
WHERE m.code = 'CENTER_TEACHERS'
ON CONFLICT (menu_id, permission_id) DO NOTHING;

-- ==============================================================================
-- 5. KHỞI TẠO CÁC LỚP HỌC MẪU CHO TRUNG TÂM ÁNH DƯƠNG VÀ PHÂN CÔNG GIÁO VIÊN
-- ==============================================================================

-- Lớp 1: Toán Nâng Cao 10 - Phân công cho Cô Nguyễn Thu Hà
INSERT INTO classes (
    id, tenant_id, name, description, fee_type, default_fee, schedule_days, start_time, end_time, is_active, teacher_id
) VALUES (
    'd0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000002',
    'Toán Nâng Cao 10 - AD01',
    'Lớp chuyên đề Toán 10 phân công cho Cô Thu Hà phụ trách.',
    'PER_LESSON',
    120000.00,
    '2,4,6',
    '18:00',
    '19:30',
    TRUE,
    'c0000000-0000-0000-0000-000000000003'
) ON CONFLICT (id) DO UPDATE 
SET name = EXCLUDED.name, teacher_id = EXCLUDED.teacher_id, default_fee = EXCLUDED.default_fee;

-- Lớp 2: Ngữ Văn Luyện Thi THPT - Phân công cho Thầy Trần Văn Nam
INSERT INTO classes (
    id, tenant_id, name, description, fee_type, default_fee, schedule_days, start_time, end_time, is_active, teacher_id
) VALUES (
    'd0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000002',
    'Ngữ Văn Luyện Thi Đại Học - AD02',
    'Lớp Văn chuyên đề phân công cho Thầy Nam phụ trách.',
    'PER_LESSON',
    100000.00,
    '3,5,7',
    '19:30',
    '21:00',
    TRUE,
    'c0000000-0000-0000-0000-000000000004'
) ON CONFLICT (id) DO UPDATE 
SET name = EXCLUDED.name, teacher_id = EXCLUDED.teacher_id, default_fee = EXCLUDED.default_fee;

-- Lớp 3: Tiếng Anh IELTS 6.5+ - Phân công cho Cô Lê Thị Mai
INSERT INTO classes (
    id, tenant_id, name, description, fee_type, default_fee, schedule_days, start_time, end_time, is_active, teacher_id
) VALUES (
    'd0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000002',
    'Tiếng Anh IELTS 6.5+ - AD03',
    'Lớp Tiếng Anh chuyên sâu phân công cho Cô Mai phụ trách.',
    'PER_LESSON',
    150000.00,
    'CN',
    '08:30',
    '11:00',
    TRUE,
    'c0000000-0000-0000-0000-000000000005'
) ON CONFLICT (id) DO UPDATE 
SET name = EXCLUDED.name, teacher_id = EXCLUDED.teacher_id, default_fee = EXCLUDED.default_fee;

-- Thêm học sinh mẫu cho Lớp 1 (Cô Hà)
INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
) VALUES 
('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001', 'Nguyễn Hoàng Long', 'Long Nhỏ', 'Nguyễn Văn Hùng', '0981234567', NULL, 'STUDYING'),
('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001', 'Trần Bảo Ngọc', 'Bảo Ngọc', 'Lê Thị Thu', '0987654321', NULL, 'STUDYING')
ON CONFLICT (id) DO NOTHING;

-- Thêm học sinh mẫu cho Lớp 2 (Thầy Nam)
INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
) VALUES 
('e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'Lê Tuấn Kiệt', 'Kiệt', 'Lê Văn Tuấn', '0912345678', NULL, 'STUDYING')
ON CONFLICT (id) DO NOTHING;

-- Thêm học sinh mẫu cho Lớp 3 (Cô Mai)
INSERT INTO students (
    id, tenant_id, class_id, full_name, nickname, parent_name, parent_phone, custom_fee, status
) VALUES 
('e0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003', 'Phạm Minh Anh', 'Minh Anh', 'Phạm Văn Hòa', '0933338888', NULL, 'STUDYING')
ON CONFLICT (id) DO NOTHING;

