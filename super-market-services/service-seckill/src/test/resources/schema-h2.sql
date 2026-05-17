DROP TABLE IF EXISTS seckill_products;
DROP TABLE IF EXISTS seckill_sessions;

CREATE TABLE IF NOT EXISTS seckill_sessions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    start_time      TIMESTAMP NOT NULL,
    end_time        TIMESTAMP NOT NULL,
    status          TINYINT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS seckill_products (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id      BIGINT NOT NULL,
    spu_id          BIGINT NOT NULL,
    sku_id          BIGINT NOT NULL,
    seckill_price   DECIMAL(12,2) NOT NULL,
    seckill_stock   INT NOT NULL,
    limit_per_user  INT DEFAULT 1,
    status          TINYINT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
