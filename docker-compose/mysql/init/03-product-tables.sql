USE db_product;

-- 类目表 (三级类目树)
CREATE TABLE IF NOT EXISTS categories (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id       BIGINT DEFAULT 0 COMMENT '父类目ID 0=顶级',
    name            VARCHAR(50) NOT NULL COMMENT '类目名称',
    level           TINYINT NOT NULL COMMENT '层级 1/2/3',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    icon_url        VARCHAR(500) COMMENT '图标',
    status          TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品类目表';

-- 品牌表
CREATE TABLE IF NOT EXISTS brands (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL COMMENT '品牌名称',
    logo_url        VARCHAR(500) COMMENT '品牌LOGO',
    description     VARCHAR(500) COMMENT '品牌描述',
    status          TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌表';

-- SPU表
CREATE TABLE IF NOT EXISTS spu (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    spu_no          VARCHAR(32) NOT NULL UNIQUE COMMENT 'SPU编号',
    shop_id         BIGINT NOT NULL COMMENT '店铺ID',
    category_id     BIGINT NOT NULL COMMENT '三级类目ID',
    brand_id        BIGINT COMMENT '品牌ID',
    name            VARCHAR(200) NOT NULL COMMENT '商品名称',
    subtitle        VARCHAR(200) COMMENT '副标题',
    main_image      VARCHAR(500) COMMENT '主图',
    images          JSON COMMENT '图片列表',
    description     TEXT COMMENT '商品描述(富文本)',
    audit_status    TINYINT DEFAULT 0 COMMENT '0待审 1通过 2驳回',
    shelf_status    TINYINT DEFAULT 0 COMMENT '0下架 1上架',
    is_deleted      TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_shop (shop_id),
    INDEX idx_category (category_id),
    INDEX idx_audit (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SPU商品表';

-- SKU表
CREATE TABLE IF NOT EXISTS sku (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku_no          VARCHAR(32) NOT NULL UNIQUE COMMENT 'SKU编号',
    spu_id          BIGINT NOT NULL COMMENT 'SPU ID',
    spec_name       VARCHAR(100) COMMENT '规格名称(颜色:红色;尺寸:XL)',
    spec_code       VARCHAR(100) COMMENT '规格编码',
    price           DECIMAL(12,2) NOT NULL COMMENT '售价',
    market_price    DECIMAL(12,2) COMMENT '市场价',
    cost_price      DECIMAL(12,2) COMMENT '成本价',
    image           VARCHAR(500) COMMENT 'SKU图片',
    weight          INT DEFAULT 0 COMMENT '重量(g)',
    status          TINYINT DEFAULT 1 COMMENT '1正常 0停用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_spu (spu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU商品表';

-- 库存表
CREATE TABLE IF NOT EXISTS inventory (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku_id          BIGINT NOT NULL UNIQUE COMMENT 'SKU ID',
    total_stock     INT NOT NULL DEFAULT 0 COMMENT '总库存',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    locked_stock    INT NOT NULL DEFAULT 0 COMMENT '锁定库存(下单未支付)',
    safety_stock    INT NOT NULL DEFAULT 0 COMMENT '安全库存预警阈值',
    version         INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';
