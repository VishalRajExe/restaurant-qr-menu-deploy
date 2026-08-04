-- Migration V2: SaaS Foundation (Audit Logs, Staff Invitations, Soft Delete Restoration metadata)

-- 1. Add deleted_by to BaseEntity tables
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE offers ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE qr_codes ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);

-- 2. Staff Invitations table
CREATE TABLE IF NOT EXISTS staff_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    branch_id BIGINT,
    email VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL,
    token VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME NOT NULL,
    created_by VARCHAR(100),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_invitation_token (token),
    INDEX idx_invitation_restaurant (restaurant_id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Audit Logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT,
    user_id BIGINT,
    user_name VARCHAR(100),
    user_role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    timestamp DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_audit_restaurant (restaurant_id),
    INDEX idx_audit_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
