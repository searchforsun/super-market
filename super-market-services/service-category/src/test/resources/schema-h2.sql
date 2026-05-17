DROP TABLE IF EXISTS categories;

CREATE TABLE categories (
    id              BIGINT PRIMARY KEY,
    parent_id       BIGINT DEFAULT 0,
    name            VARCHAR(100),
    level           INT DEFAULT 1,
    sort_order      INT DEFAULT 0,
    icon_url        VARCHAR(500),
    status          INT DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
