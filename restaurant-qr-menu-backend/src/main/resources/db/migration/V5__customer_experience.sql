-- Migration V5: Customer Experience (Badges, Macros, Dietary Tags, Meal Timing, Combos & Customer Favorites)

-- 1. Add Badges, Macros, Dietary Tags, Meal Timing, and Combo columns to menu_items table
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS is_popular BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS is_chef_special BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS spice_level INT DEFAULT 0;

ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS protein_grams DECIMAL(6,2) DEFAULT 0.00;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS fat_grams DECIMAL(6,2) DEFAULT 0.00;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS carbs_grams DECIMAL(6,2) DEFAULT 0.00;

ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS allergens VARCHAR(255);
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS is_vegan BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS is_halal BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS is_jain BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS is_gluten_free BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS meal_type VARCHAR(20) NOT NULL DEFAULT 'ALL_DAY';
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS is_combo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS combo_description TEXT;

-- 2. Create customer_favorites table
CREATE TABLE IF NOT EXISTS customer_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_token VARCHAR(150) NOT NULL,
    restaurant_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_fav_device_restaurant (device_token, restaurant_id),
    UNIQUE KEY uk_device_item (device_token, menu_item_id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
