-- ============================================================================
-- V3 (H2 Test): Chuẩn hóa tiền tệ DECIMAL(15,2), Order State Machine & Payment,
--               và bảng Transactional Outbox tương đương MySQL
-- ============================================================================

ALTER TABLE products ALTER COLUMN price DECIMAL(15, 2);
ALTER TABLE orders ALTER COLUMN total_amount DECIMAL(15, 2) NOT NULL;
ALTER TABLE order_items ALTER COLUMN price DECIMAL(15, 2);
ALTER TABLE coupons ALTER COLUMN discount_amount DECIMAL(15, 2);

ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_transaction_id VARCHAR(128);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS expires_at DATETIME(6);

CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_payment_tx_id ON orders (payment_transaction_id);
CREATE INDEX IF NOT EXISTS idx_orders_status_expires_at ON orders (status, expires_at);

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
    last_error VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_retry ON outbox_events (status, next_retry_at);
