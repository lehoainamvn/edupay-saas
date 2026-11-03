-- Migration V5: Thêm chỉ mục tối ưu cho điểm danh và đối soát hóa đơn

-- Cập nhật dữ liệu cũ nếu có giá trị ABSENT_PERMITTED sang EXCUSED chuẩn hóa
UPDATE attendance SET status = 'EXCUSED' WHERE status = 'ABSENT_PERMITTED';

-- Tạo Index hỗ trợ batch query điểm danh theo lớp và ngày học
CREATE INDEX IF NOT EXISTS idx_attendance_class_date ON attendance(tenant_id, class_id, lesson_date);

-- Tạo Index hỗ trợ kiểm tra khóa điểm danh theo học sinh và kỳ thu
CREATE INDEX IF NOT EXISTS idx_invoices_student_month ON invoices(tenant_id, student_id, billing_month);

