-- ============================================================
-- V7: Brute-force login protection + Audit Log table
-- Phiên bản: 7 | Tác giả: EduPay Security Team
-- ============================================================

-- ─── 1. Brute-force tracking columns on users table ─────────
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITH TIME ZONE;

-- ─── 2. Mở rộng độ dài cột để lưu trữ dữ liệu ngân hàng mã hóa AES-256 ────
ALTER TABLE payment_settings ALTER COLUMN account_number TYPE VARCHAR(500);
ALTER TABLE payment_settings ALTER COLUMN account_name TYPE VARCHAR(600);

-- ─── 3. Audit Log table ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id               UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID,
    actor_user_id    UUID,
    action           VARCHAR(100)             NOT NULL,
    resource_type    VARCHAR(100),
    resource_id      VARCHAR(255),
    before_value     JSONB,
    after_value      JSONB,
    ip_address       VARCHAR(45),
    user_agent       TEXT,
    severity         VARCHAR(20)              NOT NULL DEFAULT 'INFO',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_created
    ON audit_logs (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor
    ON audit_logs (actor_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action
    ON audit_logs (action, created_at DESC);

