USE db_platform;

CREATE TABLE IF NOT EXISTS banners (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    link_url    VARCHAR(500),
    sort_order  INT DEFAULT 0,
    position    VARCHAR(50) DEFAULT 'HOME_TOP' COMMENT '展示位置',
    status      TINYINT DEFAULT 1,
    start_time  DATETIME,
    end_time    DATETIME,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pos_status (position, status)
);

CREATE TABLE IF NOT EXISTS ad_positions (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    status      TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS risk_rules (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    type        TINYINT NOT NULL COMMENT '1刷单检测 2恶意退款 3IP黑名单 4频率限制',
    config      JSON NOT NULL COMMENT '规则配置(阈值/条件)',
    status      TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS risk_logs (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id     BIGINT,
    user_id     BIGINT,
    target_id   VARCHAR(100) COMMENT '目标(订单号/IP等)',
    risk_type   TINYINT NOT NULL,
    risk_score  INT DEFAULT 0,
    detail      JSON COMMENT '检测详情',
    action      TINYINT DEFAULT 0 COMMENT '0记录 1告警 2拦截',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_time (created_at)
);
