USE db_marketing;

-- 秒杀场次表
CREATE TABLE IF NOT EXISTS seckill_sessions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL COMMENT '秒杀场次名称',
    start_time      DATETIME NOT NULL COMMENT '开始时间',
    end_time        DATETIME NOT NULL COMMENT '结束时间',
    status          TINYINT DEFAULT 0 COMMENT '0未开始 1进行中 2已结束',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀场次表';

-- 秒杀商品表
CREATE TABLE IF NOT EXISTS seckill_products (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id      BIGINT NOT NULL COMMENT '秒杀场次ID',
    spu_id          BIGINT NOT NULL COMMENT 'SPU ID',
    sku_id          BIGINT NOT NULL COMMENT 'SKU ID',
    seckill_price   DECIMAL(12,2) NOT NULL COMMENT '秒杀价格',
    seckill_stock   INT NOT NULL COMMENT '秒杀库存',
    limit_per_user  INT DEFAULT 1 COMMENT '每人限购数量',
    status          TINYINT DEFAULT 0 COMMENT '0未开始 1进行中 2已售罄 3已结束',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';
