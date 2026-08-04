-- Migration V6: Media Center (Cloud Storage Assets, Multi-Image Gallery, WebP Metadata & Cropping)

CREATE TABLE IF NOT EXISTS media_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    public_id VARCHAR(200) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    width INT DEFAULT 0,
    height INT DEFAULT 0,
    provider VARCHAR(30) NOT NULL DEFAULT 'CLOUDINARY', -- CLOUDINARY | S3 | LOCAL
    crop_data VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_media_restaurant (restaurant_id),
    INDEX idx_media_public_id (public_id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
