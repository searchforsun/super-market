DROP TABLE IF EXISTS addresses;

CREATE TABLE addresses (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT,
    receiver_name   VARCHAR(50),
    receiver_phone  VARCHAR(20),
    province        VARCHAR(50),
    city            VARCHAR(50),
    district        VARCHAR(50),
    detail          VARCHAR(255),
    is_default      INT DEFAULT 0,
    is_deleted      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
