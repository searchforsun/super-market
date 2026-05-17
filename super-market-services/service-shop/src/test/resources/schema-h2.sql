DROP TABLE IF EXISTS shops;
DROP TABLE IF EXISTS merchants;

CREATE TABLE IF NOT EXISTS merchants (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL UNIQUE,
    company_name    VARCHAR(200),
    business_license VARCHAR(200),
    legal_person    VARCHAR(50),
    id_card         VARCHAR(18),
    contact_phone   VARCHAR(20),
    audit_status    TINYINT DEFAULT 0,
    audit_reason    VARCHAR(500),
    status          TINYINT DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shops (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id     BIGINT NOT NULL UNIQUE,
    shop_name       VARCHAR(100) NOT NULL,
    shop_logo       VARCHAR(500),
    shop_desc       VARCHAR(500),
    status          TINYINT DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
