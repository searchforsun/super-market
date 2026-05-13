USE db_product;

-- 商品评价表
CREATE TABLE IF NOT EXISTS reviews (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    spu_id          BIGINT NOT NULL COMMENT 'SPU ID',
    sku_id          BIGINT COMMENT 'SKU ID',
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
    rating          TINYINT NOT NULL COMMENT '星级 1-5',
    content         TEXT COMMENT '评价内容',
    images          JSON COMMENT '评价图片列表',
    is_anonymous    TINYINT DEFAULT 0 COMMENT '是否匿名',
    reply_content   TEXT COMMENT '商家回复',
    reply_at        DATETIME COMMENT '回复时间',
    status          TINYINT DEFAULT 1 COMMENT '1正常 0隐藏',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_spu (spu_id),
    INDEX idx_user (user_id),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';
