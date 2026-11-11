-- V8: Bổ sung các trường thời khóa biểu có cấu trúc cho lớp học (Weekly Schedule)
ALTER TABLE classes
ADD COLUMN IF NOT EXISTS schedule_days VARCHAR(100),
ADD COLUMN IF NOT EXISTS start_time VARCHAR(10),
ADD COLUMN IF NOT EXISTS end_time VARCHAR(10);

-- Cập nhật dữ liệu mẫu cho lớp Toán nâng cao 8A
UPDATE classes
SET schedule_days = 'TUESDAY,SATURDAY',
    start_time = '18:00',
    end_time = '19:30'
WHERE id = 'd0000000-0000-0000-0000-000000000001';

