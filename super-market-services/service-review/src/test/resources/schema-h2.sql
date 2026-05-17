DROP TABLE IF EXISTS reviews;

CREATE TABLE reviews (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT,
    spu_id          BIGINT,
    sku_id          BIGINT,
    order_no        VARCHAR(64),
    rating          INT DEFAULT 5,
    content         VARCHAR(1000),
    images          VARCHAR(2000),
    is_anonymous    INT DEFAULT 0,
    reply_content   VARCHAR(1000),
    reply_at        TIMESTAMP,
    status          INT DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
