DROP TABLE IF EXISTS user_coupons;
DROP TABLE IF EXISTS coupon_batches;
DROP TABLE IF EXISTS coupon_templates;

CREATE TABLE IF NOT EXISTS coupon_templates (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    type            TINYINT NOT NULL,
    discount_value  DECIMAL(12,2) NOT NULL,
    min_amount      DECIMAL(12,2) DEFAULT 0,
    total_stock     INT NOT NULL DEFAULT 0,
    remaining_stock INT NOT NULL DEFAULT 0,
    per_user_limit  INT DEFAULT 1,
    valid_days      INT NOT NULL,
    status          TINYINT DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coupon_batches (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id     BIGINT NOT NULL,
    batch_name      VARCHAR(100),
    quantity        INT NOT NULL,
    distribute_type TINYINT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_coupons (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    template_id     BIGINT NOT NULL,
    batch_id        BIGINT,
    coupon_code     VARCHAR(32) NOT NULL UNIQUE,
    status          TINYINT NOT NULL DEFAULT 1,
    order_no        VARCHAR(32),
    used_at         TIMESTAMP,
    expire_time     TIMESTAMP NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
