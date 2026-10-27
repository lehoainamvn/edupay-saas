-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║     V4__add_refresh_tokens.sql                                               ║
-- ║     EduPay SaaS - Bảng Refresh Tokens (Phương án B: Lưu DB, hỗ trợ revoke) ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

-- Bảng lưu Refresh Token (JWT Authentication - Option B)
-- Cho phép: logout thật, revoke token, đăng xuất tất cả thiết bị
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id  UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    token      VARCHAR(512) UNIQUE NOT NULL,    -- UUID v4 ngẫu nhiên (không phải JWT)
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    user_agent VARCHAR(512),                    -- Tùy chọn: nhận biết thiết bị
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index tối ưu hóa lookup
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token    ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id  ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_tenant   ON refresh_tokens(tenant_id);
