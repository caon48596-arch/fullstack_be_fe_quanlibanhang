-- ============================================================================
-- V1: Khởi tạo lược đồ cơ sở dữ liệu cốt lõi (tương thích MySQL 8 & H2 MySQL Mode)
-- ============================================================================

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255),
    password VARCHAR(255),
    email VARCHAR(255),
    online BOOLEAN NOT NULL DEFAULT FALSE,
    role VARCHAR(255),
    dob VARCHAR(255),
    phone VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    price DOUBLE,
    stock INT,
    description VARCHAR(255),
    image_url VARCHAR(255),
    video_url VARCHAR(255),
    category_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_date DATETIME(6),
    user_id BIGINT,
    total_amount DOUBLE NOT NULL,
    status VARCHAR(255),
    idempotency_key VARCHAR(128),
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quantity INT,
    price DOUBLE,
    order_id BIGINT,
    product_id BIGINT,
    status VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(255),
    discount_amount DOUBLE,
    quantity INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    expiration_date DATETIME(6),
    PRIMARY KEY (id)
);
