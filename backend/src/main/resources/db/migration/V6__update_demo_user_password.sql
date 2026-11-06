-- Cập nhật mật khẩu chuẩn Admin@123 cho các tài khoản seed demo
UPDATE users 
SET password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy' 
WHERE phone IN ('0912345678', '0988888888');

UPDATE tenants 
SET password_hash = '$2a$10$p79GckpyaL0UClYJwheEEOFEPbb6pvM0EBEqkTzFQOkJR8e66bkpy' 
WHERE phone IN ('0912345678', '0988888888');
