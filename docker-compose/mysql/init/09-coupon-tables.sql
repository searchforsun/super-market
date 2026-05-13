USE db_marketing;

-- 优惠券模板表
CREATE TABLE IF NOT EXISTS coupon_templates (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type            TINYINT NOT NULL COMMENT '1满减券 2折扣券 3直减券',
    discount_value  DECIMAL(12,2) NOT NULL COMMENT '优惠值',
    min_amount      DECIMAL(12,2) DEFAULT 0 COMMENT '最低消费金额门槛',
    total_stock     INT NOT NULL DEFAULT 0 COMMENT '总发放量',
    remaining_stock INT NOT NULL DEFAULT 0 COMMENT '剩余数量',
    per_user_limit  INT DEFAULT 1 COMMENT '每人限领数量',
    valid_days      INT NOT NULL COMMENT '有效天数',
    status          TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

-- 优惠券批次表
CREATE TABLE IF NOT EXISTS coupon_batches (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id     BIGINT NOT NULL COMMENT '优惠券模板ID',
    batch_name      VARCHAR(100) COMMENT '批次名称',
    quantity        INT NOT NULL COMMENT '本批次发放数量',
    distribute_type TINYINT NOT NULL COMMENT '1平台发放 2用户领取 3活动赠送',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券批次表';

-- 用户优惠券表
CREATE TABLE IF NOT EXISTS user_coupons (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    template_id     BIGINT NOT NULL COMMENT '优惠券模板ID',
    batch_id        BIGINT COMMENT '发放批次ID',
    coupon_code     VARCHAR(32) NOT NULL UNIQUE COMMENT '券码',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '1未使用 2已使用 3已过期',
    order_no        VARCHAR(32) COMMENT '使用的订单号',
    used_at         DATETIME COMMENT '使用时间',
    expire_time     DATETIME NOT NULL COMMENT '过期时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status),
    INDEX idx_code (coupon_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';
