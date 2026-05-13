USE db_order;

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    shop_id         BIGINT NOT NULL COMMENT '店铺ID',
    total_amount    DECIMAL(12,2) NOT NULL COMMENT '订单总金额',
    discount_amount DECIMAL(12,2) DEFAULT 0 COMMENT '优惠金额',
    freight_amount  DECIMAL(12,2) DEFAULT 0 COMMENT '运费',
    actual_amount   DECIMAL(12,2) NOT NULL COMMENT '实付金额',
    pay_method      TINYINT COMMENT '支付方式 1支付宝 2微信',
    pay_no          VARCHAR(32) COMMENT '支付单号',
    order_status    TINYINT NOT NULL COMMENT '1待付款 2待发货 3待收货 4已完成 5已取消 6已退款',
    address_snapshot JSON NOT NULL COMMENT '收货地址快照',
    expire_time     DATETIME COMMENT '支付过期时间',
    paid_at         DATETIME COMMENT '支付时间',
    shipped_at      DATETIME COMMENT '发货时间',
    received_at     DATETIME COMMENT '收货时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_user_status (user_id, order_status),
    INDEX idx_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS order_items (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
    sku_id          BIGINT NOT NULL COMMENT 'SKU ID',
    sku_name        VARCHAR(200) NOT NULL COMMENT '商品名称(快照)',
    sku_image       VARCHAR(500) COMMENT '商品图片(快照)',
    sku_price       DECIMAL(12,2) NOT NULL COMMENT '购买时单价',
    quantity        INT NOT NULL COMMENT '购买数量',
    total_price     DECIMAL(12,2) NOT NULL COMMENT '小计',
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- Seata AT undo_log
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id       BIGINT NOT NULL COMMENT '分支事务ID',
    xid             VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    context         VARCHAR(128) NOT NULL COMMENT '上下文',
    rollback_info   LONGBLOB NOT NULL COMMENT '回滚信息',
    log_status      INT NOT NULL COMMENT '日志状态',
    log_created     DATETIME NOT NULL COMMENT '创建时间',
    log_modified    DATETIME NOT NULL COMMENT '修改时间',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo_log';
