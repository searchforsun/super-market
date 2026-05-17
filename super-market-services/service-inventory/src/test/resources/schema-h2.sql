DROP TABLE IF EXISTS inventory;

CREATE TABLE inventory (
    id              BIGINT PRIMARY KEY,
    sku_id          BIGINT,
    total_stock     INT DEFAULT 0,
    available_stock INT DEFAULT 0,
    locked_stock    INT DEFAULT 0,
    safety_stock    INT DEFAULT 0,
    version         INT DEFAULT 0,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
