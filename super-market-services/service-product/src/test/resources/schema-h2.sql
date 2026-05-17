DROP TABLE IF EXISTS sku;
DROP TABLE IF EXISTS spu;

CREATE TABLE spu (
    id              BIGINT PRIMARY KEY,
    spu_no          VARCHAR(64),
    shop_id         BIGINT,
    category_id     BIGINT,
    brand_id        BIGINT,
    name            VARCHAR(255),
    subtitle        VARCHAR(255),
    main_image      VARCHAR(500),
    images          VARCHAR(2000),
    description     VARCHAR(4000),
    audit_status    INT DEFAULT 0,
    shelf_status    INT DEFAULT 0,
    is_deleted      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sku (
    id              BIGINT PRIMARY KEY,
    sku_no          VARCHAR(64),
    spu_id          BIGINT,
    spec_name       VARCHAR(255),
    spec_code       VARCHAR(255),
    price           DECIMAL(10,2),
    market_price    DECIMAL(10,2),
    cost_price      DECIMAL(10,2),
    image           VARCHAR(500),
    weight          INT DEFAULT 0,
    status          INT DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
