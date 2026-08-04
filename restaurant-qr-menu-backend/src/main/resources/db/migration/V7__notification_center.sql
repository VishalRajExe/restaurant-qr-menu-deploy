-- Migration V7: Notification Center (Multi-Channel Notifications, Event Triggers & Inbox Management)

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- SUBSCRIPTION_EXPIRING | OFFER_ENDING | NEW_STAFF_JOINED | QR_GENERATED | PAYMENT_RECEIVED
    channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP', -- EMAIL | IN_APP | SMS | PUSH
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_notif_user_read (user_id, is_read),
    INDEX idx_notif_restaurant (restaurant_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
