-- Migration V8: Support Ticket System (Tickets, WhatsApp-Style Chat, Internal Notes, SLA, Knowledge Base & Saved Replies)

-- 1. support_tickets table
CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(50) UNIQUE NOT NULL,
    restaurant_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    assigned_to_user_id BIGINT,
    assigned_team VARCHAR(50) DEFAULT 'SUPPORT_AGENT', -- DEVELOPER | SUPPORT_AGENT | BILLING_TEAM | SALES_TEAM
    escalation_level VARCHAR(30) DEFAULT 'LEVEL_1',    -- LEVEL_1 | LEVEL_2 | DEVELOPER | MANAGER
    subject VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,                    -- BILLING | SUBSCRIPTION | TECHNICAL_ISSUE | QR_PROBLEM | MENU_ISSUE | FEATURE_REQUEST | BUG_REPORT | OTHER
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',   -- LOW | MEDIUM | HIGH | CRITICAL
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',       -- OPEN | ASSIGNED | IN_PROGRESS | WAITING_FOR_CUSTOMER | RESOLVED | CLOSED
    sla_response_deadline DATETIME,
    sla_resolution_deadline DATETIME,
    is_sla_violated BOOLEAN NOT NULL DEFAULT FALSE,
    rating INT DEFAULT 0,
    feedback TEXT,
    tags VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_ticket_restaurant (restaurant_id),
    INDEX idx_ticket_status (status),
    INDEX idx_ticket_number (ticket_number),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. ticket_messages table
CREATE TABLE IF NOT EXISTS ticket_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    sender_role VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    attachments TEXT,
    is_internal_note BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_msg_ticket (ticket_id),
    FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. knowledge_articles table
CREATE TABLE IF NOT EXISTS knowledge_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    category VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_kb_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. saved_replies table
CREATE TABLE IF NOT EXISTS saved_replies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
