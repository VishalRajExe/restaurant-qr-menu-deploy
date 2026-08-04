-- Initial schema for Restaurant QR Menu SaaS
-- Phase 1: Database Design

-- restaurants table
CREATE TABLE IF NOT EXISTS restaurants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    logo_url VARCHAR(500),
    banner_url VARCHAR(500),
    phone VARCHAR(20),
    email VARCHAR(150),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100),
    website_url VARCHAR(500),
    primary_color VARCHAR(7) DEFAULT '#FF6B35',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    subscription_plan VARCHAR(20) NOT NULL DEFAULT 'BASIC',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_restaurant_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- branches table
CREATE TABLE IF NOT EXISTS branches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    address TEXT,
    phone VARCHAR(20),
    opening_hours VARCHAR(200),
    latitude DOUBLE,
    longitude DOUBLE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_branch_restaurant (restaurant_id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_category_restaurant (restaurant_id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- menu_items table
CREATE TABLE IF NOT EXISTS menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    image_url VARCHAR(500),
    veg_nonveg VARCHAR(20) NOT NULL DEFAULT 'NON_VEG',
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    calories INT,
    prep_time_minutes INT,
    display_order INT NOT NULL DEFAULT 0,
    tags VARCHAR(300),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_menuitem_category (category_id),
    INDEX idx_menuitem_restaurant (restaurant_id),
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- offers table
CREATE TABLE IF NOT EXISTS offers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    banner_url VARCHAR(500),
    discount_percentage DECIMAL(5,2),
    discount_amount DECIMAL(10,2),
    discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_offer_restaurant (restaurant_id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- qr_codes table
CREATE TABLE IF NOT EXISTS qr_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    table_number VARCHAR(20),
    label VARCHAR(100),
    token VARCHAR(64) NOT NULL UNIQUE,
    qr_image_url TEXT,
    scan_count BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_qr_branch (branch_id),
    INDEX idx_qr_token (token),
    FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- users table (from users module)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    phone VARCHAR(20),
    profile_image_url VARCHAR(500),
    last_login_at DATETIME,
    reset_token VARCHAR(255),
    reset_token_expiry DATETIME,
    restaurant_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_users_email (email),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- optional: scan_events table (analytics)
CREATE TABLE IF NOT EXISTS scan_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    qr_code_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_type VARCHAR(20),
    country VARCHAR(60),
    INDEX idx_scan_restaurant (restaurant_id),
    INDEX idx_scan_created_at (created_at),
    INDEX idx_scan_qr (qr_code_id),
    FOREIGN KEY (qr_code_id) REFERENCES qr_codes(id) ON DELETE CASCADE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;