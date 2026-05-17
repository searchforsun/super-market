DROP TABLE IF EXISTS payment_idempotent;
DROP TABLE IF EXISTS payment_refunds;
DROP TABLE IF EXISTS payments;

CREATE TABLE payments (
    id          BIGINT PRIMARY KEY,
    pay_no      VARCHAR(64)   NOT NULL UNIQUE,
    order_no    VARCHAR(64)   NOT NULL,
    user_id     BIGINT        NOT NULL,
    amount      DECIMAL(10,2) DEFAULT 0.00,
    pay_method  INT           DEFAULT 1,
    pay_status  INT           DEFAULT 1,
    third_pay_no VARCHAR(128),
    paid_at     TIMESTAMP,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_refunds (
    id              BIGINT PRIMARY KEY,
    refund_no       VARCHAR(64)   NOT NULL UNIQUE,
    pay_no          VARCHAR(64)   NOT NULL,
    order_no        VARCHAR(64)   NOT NULL,
    refund_amount   DECIMAL(10,2) DEFAULT 0.00,
    refund_reason   VARCHAR(500),
    refund_status   INT           DEFAULT 1,
    third_refund_no VARCHAR(128),
    refunded_at     TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_idempotent (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id    VARCHAR(128) NOT NULL,
    business_type VARCHAR(64),
    business_no   VARCHAR(64),
    response_data VARCHAR(1000),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
