-- ============================================================================
-- V2 (MySQL): Chuẩn hóa cột nâng cấp & tạo toàn bộ Database Indexes / Unique Indexes
-- An toàn (idempotent) cho cả DB mới hoàn toàn lẫn DB production đang hoạt động
-- ============================================================================

DELIMITER //

CREATE PROCEDURE sp_v2_apply_indexes_and_constraints()
BEGIN
    -- 1. Bảo đảm các cột nâng cấp tồn tại trên DB cũ (baseline từ V1)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'deleted'
    ) THEN
        ALTER TABLE products ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;

    UPDATE products SET deleted = FALSE WHERE deleted IS NULL;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'idempotency_key'
    ) THEN
        ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(128) NULL;
    END IF;

    -- 2. Dọn index non-unique cũ (nếu Hibernate ddl-auto=update từng tạo trước đây)
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'users' AND index_name = 'idx_users_email'
    ) THEN
        DROP INDEX idx_users_email ON users;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'coupons' AND index_name = 'idx_coupons_code'
    ) THEN
        DROP INDEX idx_coupons_code ON coupons;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'products' AND index_name = 'idx_products_category_deleted'
    ) THEN
        DROP INDEX idx_products_category_deleted ON products;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'idx_orders_user_id'
    ) THEN
        DROP INDEX idx_orders_user_id ON orders;
    END IF;

    -- 3. Unique Index trên Users(email) & Index trên Users(phone)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'users' AND index_name = 'uk_users_email'
    ) THEN
        CREATE UNIQUE INDEX uk_users_email ON users (email);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'users' AND index_name = 'idx_users_phone'
    ) THEN
        CREATE INDEX idx_users_phone ON users (phone);
    END IF;

    -- 4. Unique Index trên Coupons(code)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'coupons' AND index_name = 'uk_coupons_code'
    ) THEN
        CREATE UNIQUE INDEX uk_coupons_code ON coupons (code);
    END IF;

    -- 5. Index trên Categories(name)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'categories' AND index_name = 'idx_categories_name'
    ) THEN
        CREATE INDEX idx_categories_name ON categories (name);
    END IF;

    -- 6. Composite Indexes trên Products: (category_id, price), (deleted, category_id, price), (deleted, price), (name)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'products' AND index_name = 'idx_products_category_price'
    ) THEN
        CREATE INDEX idx_products_category_price ON products (category_id, price);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'products' AND index_name = 'idx_products_deleted_category_price'
    ) THEN
        CREATE INDEX idx_products_deleted_category_price ON products (deleted, category_id, price);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'products' AND index_name = 'idx_products_deleted_price'
    ) THEN
        CREATE INDEX idx_products_deleted_price ON products (deleted, price);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'products' AND index_name = 'idx_products_name'
    ) THEN
        CREATE INDEX idx_products_name ON products (name);
    END IF;

    -- 7. Composite Index trên Orders(user_id, order_date, status), (order_date), (status) & Unique Idempotency Key
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'idx_orders_user_date_status'
    ) THEN
        CREATE INDEX idx_orders_user_date_status ON orders (user_id, order_date, status);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'idx_orders_order_date'
    ) THEN
        CREATE INDEX idx_orders_order_date ON orders (order_date);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'idx_orders_status'
    ) THEN
        CREATE INDEX idx_orders_status ON orders (status);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'uk_orders_idempotency_key'
    ) THEN
        CREATE UNIQUE INDEX uk_orders_idempotency_key ON orders (idempotency_key);
    END IF;

    -- 8. Indexes khóa ngoại trên OrderItems(order_id) & OrderItems(product_id)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'order_items' AND index_name = 'idx_order_items_order_id'
    ) THEN
        CREATE INDEX idx_order_items_order_id ON order_items (order_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'order_items' AND index_name = 'idx_order_items_product_id'
    ) THEN
        CREATE INDEX idx_order_items_product_id ON order_items (product_id);
    END IF;
END //

DELIMITER ;

CALL sp_v2_apply_indexes_and_constraints();
DROP PROCEDURE sp_v2_apply_indexes_and_constraints;
