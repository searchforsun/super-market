USE db_payment;

-- 支付单表
CREATE TABLE IF NOT EXISTS payments (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    pay_no          VARCHAR(32) NOT NULL UNIQUE COMMENT '支付单号',
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    amount          DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    pay_method      TINYINT NOT NULL COMMENT '支付方式 1支付宝 2微信',
    pay_status      TINYINT NOT NULL COMMENT '1待支付 2支付成功 3支付失败 4已退款',
    third_pay_no    VARCHAR(64) COMMENT '第三方支付流水号',
    paid_at         DATETIME COMMENT '支付成功时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pay_no (pay_no),
    INDEX idx_order_no (order_no),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单表';

-- 退款单表
CREATE TABLE IF NOT EXISTS payment_refunds (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    refund_no       VARCHAR(32) NOT NULL UNIQUE COMMENT '退款单号',
    pay_no          VARCHAR(32) NOT NULL COMMENT '原支付单号',
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
    refund_amount   DECIMAL(12,2) NOT NULL COMMENT '退款金额',
    refund_reason   VARCHAR(500) COMMENT '退款原因',
    refund_status   TINYINT NOT NULL COMMENT '1退款中 2退款成功 3退款失败',
    third_refund_no VARCHAR(64) COMMENT '第三方退款流水号',
    refunded_at     DATETIME COMMENT '退款成功时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refund_no (refund_no),
    INDEX idx_pay_no (pay_no),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款单表';

-- 幂等表（支付回调幂等）
CREATE TABLE IF NOT EXISTS payment_idempotent (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id      VARCHAR(64) NOT NULL UNIQUE COMMENT '请求唯一标识',
    business_type   VARCHAR(32) NOT NULL COMMENT '业务类型 PAYMENT/REFUND',
    business_no     VARCHAR(32) NOT NULL COMMENT '业务单号',
    response_data   JSON COMMENT '首次处理响应',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付幂等表';

-- Seata undo_log
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id       BIGINT NOT NULL,
    xid             VARCHAR(128) NOT NULL,
    context         VARCHAR(128) NOT NULL,
    rollback_info   LONGBLOB NOT NULL,
    log_status      INT NOT NULL,
    log_created     DATETIME NOT NULL,
    log_modified    DATETIME NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo_log';
