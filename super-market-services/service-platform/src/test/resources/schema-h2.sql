DROP TABLE IF EXISTS risk_logs;
DROP TABLE IF EXISTS risk_rules;
DROP TABLE IF EXISTS ad_positions;
DROP TABLE IF EXISTS banners;

CREATE TABLE IF NOT EXISTS banners (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    link_url    VARCHAR(500),
    sort_order  INT DEFAULT 0,
    position    VARCHAR(50) DEFAULT 'HOME_TOP',
    status      TINYINT DEFAULT 1,
    start_time  TIMESTAMP,
    end_time    TIMESTAMP,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ad_positions (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    status      TINYINT DEFAULT 1,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS risk_rules (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    type        TINYINT NOT NULL,
    config      VARCHAR(500) NOT NULL,
    status      TINYINT DEFAULT 1,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS risk_logs (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id     BIGINT,
    user_id     BIGINT,
    target_id   VARCHAR(100),
    risk_type   TINYINT NOT NULL,
    risk_score  INT DEFAULT 0,
    detail      VARCHAR(500),
    action      TINYINT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
