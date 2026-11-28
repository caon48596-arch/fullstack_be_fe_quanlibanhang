-- ============================================================================
-- V3 (MySQL): Chuẩn hóa tiền tệ DECIMAL(15,2), Order State Machine & Payment,
--             Transactional Outbox Pattern, và MySQL FULLTEXT Search Index
-- ============================================================================

DELIMITER //

CREATE PROCEDURE sp_v3_upgrade_ecommerce_schema()
BEGIN
    -- 1. Chuyển toàn bộ cột tiền tệ từ DOUBLE sang DECIMAL(15,2) để tránh sai số dấu phẩy động
    ALTER TABLE products MODIFY COLUMN price DECIMAL(15, 2);
    ALTER TABLE orders MODIFY COLUMN total_amount DECIMAL(15, 2) NOT NULL;
    ALTER TABLE order_items MODIFY COLUMN price DECIMAL(15, 2);
    ALTER TABLE coupons MODIFY COLUMN discount_amount DECIMAL(15, 2);

    -- 2. Bổ sung các cột phục vụ State Machine, hoàn coupon, Payment Webhook & hết hạn thanh toán trên orders
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'coupon_code'
    ) THEN
        ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(255) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'payment_status'
    ) THEN
        ALTER TABLE orders ADD COLUMN payment_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'payment_transaction_id'
    ) THEN
        ALTER TABLE orders ADD COLUMN payment_transaction_id VARCHAR(128) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'expires_at'
    ) THEN
        ALTER TABLE orders ADD COLUMN expires_at DATETIME(6) NULL;
    END IF;

    -- 3. Indexes cho Payment Webhook idempotency và Scheduler quét đơn quá hạn
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'uk_orders_payment_tx_id'
    ) THEN
        CREATE UNIQUE INDEX uk_orders_payment_tx_id ON orders (payment_transaction_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'idx_orders_status_expires_at'
    ) THEN
        CREATE INDEX idx_orders_status_expires_at ON orders (status, expires_at);
    END IF;

    -- 4. Bảng Transactional Outbox đảm bảo sự kiện (Email hóa đơn, Payment...) không mất khi crash
    CREATE TABLE IF NOT EXISTS outbox_events (
        id BIGINT NOT NULL AUTO_INCREMENT,
        aggregate_type VARCHAR(64) NOT NULL,
        aggregate_id BIGINT NOT NULL,
        event_type VARCHAR(64) NOT NULL,
        payload TEXT NOT NULL,
        status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
        retry_count INT NOT NULL DEFAULT 0,
        max_retries INT NOT NULL DEFAULT 5,
        next_retry_at DATETIME(6) NOT NULL,
        last_error VARCHAR(1000) NULL,
        created_at DATETIME(6) NOT NULL,
        processed_at DATETIME(6) NULL,
        PRIMARY KEY (id)
    );

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'outbox_events' AND index_name = 'idx_outbox_status_next_retry'
    ) THEN
        CREATE INDEX idx_outbox_status_next_retry ON outbox_events (status, next_retry_at);
    END IF;

    -- 5. MySQL FULLTEXT Index cho tìm kiếm sản phẩm (thay thế LIKE '%keyword%' gây full table scan)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'products' AND index_name = 'ft_products_name_desc'
    ) THEN
        CREATE FULLTEXT INDEX ft_products_name_desc ON products (name, description);
    END IF;
END //

DELIMITER ;

CALL sp_v3_upgrade_ecommerce_schema();
DROP PROCEDURE sp_v3_upgrade_ecommerce_schema;
