-- Migration V4: Dashboard Analytics (Search Logs & Scan Events Indexing)

-- 1. Search logs table for search analytics
CREATE TABLE IF NOT EXISTS search_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    search_term VARCHAR(150) NOT NULL,
    search_count INT NOT NULL DEFAULT 1,
    last_searched_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_search_restaurant_term (restaurant_id, search_term),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Indexes for scan_events table optimization
ALTER TABLE scan_events ADD INDEX IF NOT EXISTS idx_scan_restaurant_time (restaurant_id, created_at);
