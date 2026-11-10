-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║     V7__create_payment_transactions.sql                                      ║
-- ║     EduPay SaaS - Bảng Log giao dịch thanh toán & Đối soát tự động Webhook    ║
-- ║     Flyway Migration | PostgreSQL 16                                          ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

-- 1. Bảng lưu log chi tiết từng giao dịch thanh toán (Audit Trail)
CREATE TABLE IF NOT EXISTS payment_transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id        UUID REFERENCES invoices(id) ON DELETE SET NULL,
    reference_number  VARCHAR(100) UNIQUE NOT NULL, -- Mã giao dịch duy nhất từ Ngân hàng / SePay / Casso
    amount            NUMERIC(12, 2) NOT NULL,
    bank_brand_name   VARCHAR(50),                   -- VD: MBBank, VCB, ACB
    account_number    VARCHAR(50),
    transaction_content TEXT,                        -- Nội dung chuyển khoản gốc từ biến động số dư
    payment_source    VARCHAR(50) DEFAULT 'WEBHOOK', -- WEBHOOK, MANUAL_CASH, MANUAL_TRANSFER
    confirmed_by      UUID REFERENCES users(id),     -- NULL nếu webhook tự động gạch nợ
    transaction_time  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Thêm cột audit người xác nhận gạch nợ vào bảng invoices
ALTER TABLE invoices 
ADD COLUMN IF NOT EXISTS confirmed_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP WITH TIME ZONE;

-- 3. Tạo index tối ưu tra cứu đối soát
CREATE INDEX IF NOT EXISTS idx_payment_tx_tenant ON payment_transactions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_tx_invoice ON payment_transactions(invoice_id);
CREATE INDEX IF NOT EXISTS idx_payment_tx_ref ON payment_transactions(reference_number);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);
