-- Migration V3: Subscription SaaS (Multi-Tier Plans, 14-Day Trial, Billing/Invoices, Coupons)

-- 1. Add Trial columns to restaurants table
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS trial_ends_at DATETIME;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS is_trial BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Add Invoice, GST, Coupon columns to subscriptions table
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS invoice_number VARCHAR(100);
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS gst_number VARCHAR(50);
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS tax_amount DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(50);
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS auto_renew BOOLEAN NOT NULL DEFAULT TRUE;

-- 3. Create coupons table
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_type VARCHAR(20) NOT NULL, -- PERCENTAGE | FLAT
    discount_value DECIMAL(10,2) NOT NULL,
    max_usage INT DEFAULT 1000,
    times_used INT NOT NULL DEFAULT 0,
    expires_at DATETIME,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_coupon_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
