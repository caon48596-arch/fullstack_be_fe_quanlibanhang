-- ============================================================================
-- V2 (H2 Test): Tạo toàn bộ Database Indexes & Unique Constraints tương đương MySQL
-- ============================================================================

-- Users: Unique email & phone lookup
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users (phone);

-- Coupons: Unique code
CREATE UNIQUE INDEX IF NOT EXISTS uk_coupons_code ON coupons (code);

-- Categories: Name lookup
CREATE INDEX IF NOT EXISTS idx_categories_name ON categories (name);

-- Products: Composite (category_id, price), Soft Delete + Filter/Sort
CREATE INDEX IF NOT EXISTS idx_products_category_price ON products (category_id, price);
CREATE INDEX IF NOT EXISTS idx_products_deleted_category_price ON products (deleted, category_id, price);
CREATE INDEX IF NOT EXISTS idx_products_deleted_price ON products (deleted, price);
CREATE INDEX IF NOT EXISTS idx_products_name ON products (name);

-- Orders: Composite (user_id, order_date, status), Dashboard date/status, Unique idempotency_key
CREATE INDEX IF NOT EXISTS idx_orders_user_date_status ON orders (user_id, order_date, status);
CREATE INDEX IF NOT EXISTS idx_orders_order_date ON orders (order_date);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_idempotency_key ON orders (idempotency_key);

-- OrderItems: Foreign key indexes
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items (product_id);
