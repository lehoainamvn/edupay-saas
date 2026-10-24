-- V1__init_schema.sql: Multi-tenant schema for EduPay SaaS

-- 1. Bảng Quản lý Tenant (Mỗi giáo viên/gia sư là 1 Tenant)
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    subscription_plan VARCHAR(50) DEFAULT 'TRIAL', -- TRIAL, MONTHLY, ANNUAL
    subscription_status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, SUSPENDED
    trial_ends_at TIMESTAMP WITH TIME ZONE DEFAULT (NOW() + INTERVAL '14 days'),
    subscription_ends_at TIMESTAMP WITH TIME ZONE DEFAULT (NOW() + INTERVAL '14 days'),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Bảng Cấu hình Ngân hàng của Giáo viên (Dùng sinh VietQR)
CREATE TABLE IF NOT EXISTS payment_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bank_bin VARCHAR(20) NOT NULL, -- Mã định danh ngân hàng (VD: 970422 cho MB, 970415 cho Vietinbank)
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    transfer_prefix VARCHAR(50) DEFAULT 'HP', -- Cú pháp mặc định
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_tenant_payment UNIQUE (tenant_id)
);

-- 3. Bảng Lớp học
CREATE TABLE IF NOT EXISTS classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    fee_type VARCHAR(50) DEFAULT 'PER_LESSON', -- PER_LESSON (theo buổi) hoặc MONTHLY_PACKAGE (trọn gói)
    default_fee NUMERIC(12, 2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. Bảng Học sinh
CREATE TABLE IF NOT EXISTS students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    nickname VARCHAR(100), -- Biệt danh ở nhà (rất quan trọng khi đối soát CK)
    parent_name VARCHAR(255),
    parent_phone VARCHAR(20),
    custom_fee NUMERIC(12, 2), -- Nếu có học phí riêng
    status VARCHAR(50) DEFAULT 'STUDYING', -- STUDYING, PAUSED, DROPPED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. Bảng Điểm danh từng buổi
CREATE TABLE IF NOT EXISTS attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    lesson_date DATE NOT NULL,
    status VARCHAR(50) DEFAULT 'PRESENT', -- PRESENT (Có mặt), ABSENT_PERMITTED (Có phép), ABSENT (Không phép)
    note VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_student_lesson_date UNIQUE (student_id, lesson_date)
);

-- 6. Bảng Phiếu thu học phí (Invoices)
CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    billing_month VARCHAR(7) NOT NULL, -- Định dạng 'YYYY-MM' (VD: '2026-10')
    attended_count INT DEFAULT 0,
    unit_price NUMERIC(12, 2) NOT NULL,
    extra_fee NUMERIC(12, 2) DEFAULT 0,
    discount NUMERIC(12, 2) DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL,
    transfer_code VARCHAR(100) UNIQUE NOT NULL, -- Mã đối soát định danh (VD: HP-T10-TOAN9A-001)
    public_hash VARCHAR(64) UNIQUE NOT NULL, -- Mã hash bảo mật tra cứu public (VD: 8a9f2...)
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING (Chờ nộp), PAID (Đã nộp), CANCELLED
    paid_at TIMESTAMP WITH TIME ZONE,
    paid_method VARCHAR(50), -- VIETQR, CASH, MANUAL
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 7. Tạo Index tối ưu hóa truy vấn Multi-tenant
CREATE INDEX idx_classes_tenant ON classes(tenant_id);
CREATE INDEX idx_students_tenant ON students(tenant_id);
CREATE INDEX idx_students_class ON students(class_id);
CREATE INDEX idx_attendance_tenant ON attendance(tenant_id);
CREATE INDEX idx_attendance_date ON attendance(lesson_date);
CREATE INDEX idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX idx_invoices_hash ON invoices(public_hash);
CREATE INDEX idx_invoices_code ON invoices(transfer_code);
