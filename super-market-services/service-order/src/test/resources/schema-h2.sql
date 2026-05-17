DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;

CREATE TABLE orders (
    id              BIGINT PRIMARY KEY,
    order_no        VARCHAR(64)  NOT NULL UNIQUE,
    user_id         BIGINT       NOT NULL,
    shop_id         BIGINT,
    total_amount    DECIMAL(10,2) DEFAULT 0.00,
    discount_amount DECIMAL(10,2) DEFAULT 0.00,
    freight_amount  DECIMAL(10,2) DEFAULT 0.00,
    actual_amount   DECIMAL(10,2) DEFAULT 0.00,
    pay_method      INT           DEFAULT 0,
    pay_no          VARCHAR(64),
    order_status    INT           DEFAULT 1,
    address_snapshot VARCHAR(500),
    expire_time     TIMESTAMP,
    paid_at         TIMESTAMP,
    shipped_at      TIMESTAMP,
    received_at     TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no    VARCHAR(64)   NOT NULL,
    sku_id      BIGINT        NOT NULL,
    sku_name    VARCHAR(255),
    sku_image   VARCHAR(500),
    sku_price   DECIMAL(10,2) DEFAULT 0.00,
    quantity    INT           DEFAULT 0,
    total_price DECIMAL(10,2) DEFAULT 0.00
);
