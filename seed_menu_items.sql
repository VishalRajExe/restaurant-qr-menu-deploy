USE restaurant_qr_db;

-- 1. Ensure Restaurant 1 is active, verified and properly named
UPDATE restaurants 
SET name = 'RestQR Gourmet Bistro', 
    phone = '+1 (555) 345-6789', 
    address = '123 Gourmet Blvd, New York, NY 10001', 
    city = 'New York',
    country = 'USA',
    email = 'contact@restqr.com',
    status = 'ACTIVE', 
    verification_status = 'VERIFIED', 
    is_deleted = b'0',
    primary_color = '#AB3500'
WHERE id = 1;

-- 2. Clear old deleted or misassigned records for clean state
DELETE FROM menu_items WHERE restaurant_id = 1;
DELETE FROM categories WHERE restaurant_id = 1;

-- 3. Insert Categories for Restaurant 1
INSERT INTO categories (id, restaurant_id, name, description, display_order, status, is_deleted, created_at, updated_at) VALUES
(101, 1, 'Breakfast & Bakery', 'Artisanal pastries, brioche toasts, and specialty morning coffee.', 1, 'ACTIVE', b'0', NOW(), NOW()),
(102, 1, 'Lunch & Gourmet Burgers', 'Prime dry-aged burgers, fresh wild seafood, and organic greens.', 2, 'ACTIVE', b'0', NOW(), NOW()),
(103, 1, 'Dinner & Chef Steaks', 'Signature Wagyu ribeyes, truffle pastas, and slow-cooked roasts.', 3, 'ACTIVE', b'0', NOW(), NOW()),
(104, 1, 'Artisanal Desserts', 'Handcrafted dark chocolate lava cakes, sorbets, and pastries.', 4, 'ACTIVE', b'0', NOW(), NOW()),
(105, 1, 'Beverages & Wine', 'Curated reserve wines, crafted mocktails, and specialty brews.', 5, 'ACTIVE', b'0', NOW(), NOW());

-- 4. Insert Menu Items for Restaurant 1
INSERT INTO menu_items (
    restaurant_id, category_id, name, description, price, image_url,
    veg_nonveg, is_available, is_popular, is_chef_special, is_featured,
    display_order, is_deleted, meal_type, status, availability,
    is_combo, is_gluten_free, is_halal, is_jain, is_vegan, spice_level,
    created_at, updated_at
) VALUES
-- Breakfast
(1, 101, 'Classic French Toast', 'Golden brioche toast served with Vermont maple syrup, whipped butter, and fresh seasonal berries', 8.50, '/img/menu-1.jpg', 'VEG', b'1', b'1', b'0', b'1', 1, b'0', 'BREAKFAST', 'ACTIVE', 1, b'0', b'0', b'1', b'0', b'0', 0, NOW(), NOW()),
(1, 101, 'Avocado Tartine & Poached Eggs', 'Artisanal sourdough topped with smashed Hass avocado, heirloom tomatoes, and organic poached eggs', 11.00, '/img/menu-2.jpg', 'VEG', b'1', b'0', b'0', b'0', 2, b'0', 'BREAKFAST', 'ACTIVE', 1, b'0', b'0', b'1', b'0', b'0', 0, NOW(), NOW()),
(1, 101, 'Smoked Salmon Croissant', 'Flaky buttery croissant with Norwegian smoked salmon, dill cream cheese, and capers', 12.50, '/img/menu-3.jpg', 'NON_VEG', b'1', b'1', b'0', b'1', 3, b'0', 'BREAKFAST', 'ACTIVE', 1, b'0', b'0', b'0', b'0', b'0', 0, NOW(), NOW()),
(1, 101, 'Matcha Green Tea Pancakes', 'Fluffy Japanese souffle pancakes with organic Uji matcha infused maple syrup and strawberries', 9.50, '/img/menu-8.jpg', 'VEG', b'1', b'0', b'0', b'0', 4, b'0', 'BREAKFAST', 'ACTIVE', 1, b'0', b'0', b'1', b'0', b'0', 0, NOW(), NOW()),

-- Lunch
(1, 102, 'Truffle Mushroom Burger', 'Prime Angus beef patty with black truffle aioli, melted aged gruyere, caramelized onions, on toasted brioche', 16.00, '/img/menu-5.jpg', 'NON_VEG', b'1', b'1', b'1', b'1', 5, b'0', 'LUNCH', 'ACTIVE', 1, b'0', b'0', b'1', b'0', b'0', 0, NOW(), NOW()),
(1, 102, 'Grilled Atlantic Salmon', 'Wild Atlantic salmon fillet with grilled asparagus, roasted fingerling potatoes, and lemon herb butter', 24.50, '/img/menu-4.jpg', 'NON_VEG', b'1', b'1', b'1', b'1', 6, b'0', 'LUNCH', 'ACTIVE', 1, b'0', b'1', b'1', b'0', b'0', 0, NOW(), NOW()),
(1, 102, 'Mediterranean Shakshuka', 'Slow-simmered tomato bell pepper ragout with poached eggs, feta crumble, and warm zaatar pita', 10.00, '/img/menu-2.jpg', 'VEG', b'1', b'0', b'0', b'0', 7, b'0', 'LUNCH', 'ACTIVE', 1, b'0', b'0', b'1', b'0', b'0', 1, NOW(), NOW()),

-- Dinner
(1, 103, 'Wagyu Beef Ribeye (250g)', '250g grilled A5 Wagyu ribeye with roasted garlic potato puree, grilled king oyster mushroom, and green peppercorn jus', 34.00, '/img/menu-6.jpg', 'NON_VEG', b'1', b'1', b'1', b'1', 8, b'0', 'DINNER', 'ACTIVE', 1, b'0', b'1', b'1', b'0', b'0', 0, NOW(), NOW()),
(1, 103, 'Black Truffle Fettuccine', 'Handmade egg fettuccine with shaved Norcia black winter truffles, cultured butter, and 24-month aged Parmigiano-Reggiano', 24.00, '/img/menu-7.jpg', 'VEG', b'1', b'1', b'0', b'1', 9, b'0', 'DINNER', 'ACTIVE', 1, b'0', b'0', b'1', b'0', b'0', 0, NOW(), NOW()),
(1, 103, 'Crispy Duck Confit', 'Slow-braised Moulard duck leg with golden potato rosti, braised red cabbage, and sour cherry port wine reduction', 22.00, '/img/menu-3.jpg', 'NON_VEG', b'1', b'0', b'1', b'0', 10, b'0', 'DINNER', 'ACTIVE', 1, b'0', b'1', b'1', b'0', b'0', 0, NOW(), NOW()),
(1, 103, 'Lobster Tagliolini', 'Handmade thin tagliolini with butter-poached Maine lobster claw, sweet San Marzano tomatoes, and fresh tarragon', 26.00, '/img/menu-4.jpg', 'NON_VEG', b'1', b'1', b'0', b'1', 11, b'0', 'DINNER', 'ACTIVE', 1, b'0', b'0', b'1', b'0', b'0', 0, NOW(), NOW()),

-- Desserts
(1, 104, 'Artisanal Chocolate Fondant', 'Warm Valrhona 70% dark chocolate lava cake with molten center, served with Madagascar vanilla bean gelato', 14.00, '/img/menu-8.jpg', 'VEG', b'1', b'1', b'1', b'1', 12, b'0', 'DINNER', 'ACTIVE', 1, b'0', b'0', b'1', b'0', b'0', 0, NOW(), NOW()),
(1, 104, 'Madagascar Vanilla Bean Panna Cotta', 'Silky infused cream with wild berry coulis and candied citrus zest', 11.50, '/img/menu-1.jpg', 'VEG', b'1', b'0', b'0', b'0', 13, b'0', 'DINNER', 'ACTIVE', 1, b'0', b'1', b'1', b'0', b'0', 0, NOW(), NOW());

-- 5. Ensure Table QR codes exist for Restaurant 1
DELETE FROM qr_codes WHERE restaurant_id = 1;

INSERT INTO qr_codes (restaurant_id, branch_id, table_number, label, token, scan_count, status, is_deleted, created_at, updated_at) VALUES
(1, 1, '01', 'Table 01 (Indoor Window)', 'qr-table-01-main', 42, 'ACTIVE', b'0', NOW(), NOW()),
(1, 1, '02', 'Table 02 (Main Dining)', 'qr-table-02-main', 28, 'ACTIVE', b'0', NOW(), NOW()),
(1, 1, '03', 'Table 03 (Chef Counter)', 'qr-table-03-main', 35, 'ACTIVE', b'0', NOW(), NOW()),
(1, 1, '04', 'Table 04 (Booth 1)', 'qr-table-04-main', 51, 'ACTIVE', b'0', NOW(), NOW()),
(1, 1, '05', 'Table 05 (Booth 2)', 'qr-table-05-main', 19, 'ACTIVE', b'0', NOW(), NOW()),
(1, 1, '06', 'Table 06 (Patio Garden)', 'qr-table-06-main', 63, 'ACTIVE', b'0', NOW(), NOW()),
(1, 1, '07', 'Table 07 (Patio Garden)', 'qr-table-07-main', 22, 'ACTIVE', b'0', NOW(), NOW()),
(1, 1, '08', 'Table 08 (VIP Private)', 'qr-table-08-main', 15, 'ACTIVE', b'0', NOW(), NOW());

