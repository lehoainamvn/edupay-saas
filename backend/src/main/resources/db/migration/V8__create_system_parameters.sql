-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║     V8__create_system_parameters.sql                                         ║
-- ║     EduPay SaaS - Bảng tham số cấu hình động hệ thống (System Parameters)   ║
-- ║     Flyway Migration | PostgreSQL 16                                         ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

CREATE TABLE IF NOT EXISTS system_parameters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    param_key VARCHAR(100) NOT NULL,
    param_value TEXT NOT NULL,
    param_group VARCHAR(50) NOT NULL,
    data_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_system_parameters_tenant_key UNIQUE (tenant_id, param_key)
);

CREATE INDEX IF NOT EXISTS idx_sys_params_tenant ON system_parameters(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_params_tenant_group ON system_parameters(tenant_id, param_group);

-- Seed bộ tham số cấu hình mặc định ban đầu cho tất cả các tenant hiện hữu
INSERT INTO system_parameters (tenant_id, param_key, param_value, param_group, data_type, display_name, description, is_system)
SELECT t.id, p.param_key, p.param_value, p.param_group, p.data_type, p.display_name, p.description, p.is_system
FROM tenants t
CROSS JOIN (
    VALUES
        -- ─── NHÓM 1: HỆ THỐNG (SYSTEM) ───────────────────────────────────────────
        ('SYSTEM_CENTER_NAME', 'Trung tâm Dạy kèm EduPay', 'SYSTEM', 'STRING', 'Tên trung tâm / Lớp học', 'Tên hiển thị trên biên nhận học phí và cổng tra cứu công khai của phụ huynh', TRUE),
        ('SYSTEM_AUTO_LOGOUT_HOURS', '8', 'SYSTEM', 'NUMBER', 'Thời gian tự động đăng xuất (giờ)', 'Thời lượng phiên làm việc của tài khoản trước khi cần đăng nhập lại (1 - 72 giờ)', FALSE),
        ('SYSTEM_ALLOW_TEACHER_EDIT_FEE', 'true', 'SYSTEM', 'BOOLEAN', 'Cho phép giáo viên sửa mức học phí', 'Cho phép tài khoản vai trò giáo viên điều chỉnh mức học phí buổi khi điểm danh', FALSE),
        
        -- ─── NHÓM 2: HỌC PHÍ (TUITION) ───────────────────────────────────────────
        ('TUITION_DUE_DAYS', '7', 'TUITION', 'NUMBER', 'Hạn thanh toán học phí (ngày)', 'Số ngày phụ huynh được phép thanh toán kể từ ngày phát hành phiếu thu', FALSE),
        ('TUITION_AUTO_CREATE_DAY', '28', 'TUITION', 'NUMBER', 'Ngày tự động chốt học phí trong tháng', 'Ngày hệ thống tự động tổng hợp số buổi điểm danh và sinh phiếu thu hàng tháng (1 - 31)', FALSE),
        ('TUITION_OVERDUE_POLICY', 'WARN_ONLY', 'TUITION', 'STRING', 'Chính sách xử lý khi quá hạn học phí', 'Quy định hình thức xử lý khi học sinh chưa đóng học phí đúng hạn', FALSE),

        -- ─── NHÓM 3: THÔNG BÁO (NOTIFICATION) ────────────────────────────────────
        ('NOTIFICATION_AUTO_REMINDER', 'true', 'NOTIFICATION', 'BOOLEAN', 'Tự động nhắc nợ trước ngày hết hạn', 'Tự động kích hoạt tin nhắn nhắc nhở phụ huynh khi sắp đến hạn thanh toán', FALSE),
        ('NOTIFICATION_REMINDER_BEFORE_DAYS', '2', 'NOTIFICATION', 'NUMBER', 'Số ngày nhắc trước hạn thanh toán', 'Số ngày hệ thống gửi thông báo nhắc nhở trước khi phiếu thu hết hạn', FALSE),
        ('NOTIFICATION_TEMPLATE_INVOICE', 'Kính gửi Quý Phụ huynh, học phí tháng {month} của em {studentName} là {amount}đ. Vui lòng thanh toán qua link VietQR: {invoiceUrl}. Trân trọng cảm ơn!', 'NOTIFICATION', 'STRING', 'Mẫu tin nhắn gửi phiếu thu học phí', 'Nội dung tin nhắn tự động khi gửi thông báo phiếu thu qua Zalo hoặc SMS cho phụ huynh', FALSE)
) AS p(param_key, param_value, param_group, data_type, display_name, description, is_system)
ON CONFLICT (tenant_id, param_key) DO NOTHING;

