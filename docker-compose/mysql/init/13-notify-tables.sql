USE db_product;

CREATE TABLE IF NOT EXISTS notify_templates (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码',
    name        VARCHAR(100) NOT NULL COMMENT '模板名称',
    channel     TINYINT NOT NULL COMMENT '1站内信 2邮件 3短信',
    title       VARCHAR(200) COMMENT '标题模板',
    content     TEXT NOT NULL COMMENT '内容模板(支持${变量})',
    status      TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL COMMENT '接收用户ID',
    template_id BIGINT COMMENT '模板ID',
    channel     TINYINT NOT NULL COMMENT '1站内信 2邮件 3短信',
    title       VARCHAR(200),
    content     TEXT NOT NULL,
    target      VARCHAR(200) COMMENT '邮件地址/手机号',
    status      TINYINT DEFAULT 0 COMMENT '0未读 1已读',
    send_status TINYINT DEFAULT 1 COMMENT '0待发送 1已发送 2发送失败',
    send_at     DATETIME,
    read_at     DATETIME,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id, status),
    INDEX idx_send (send_status)
);
