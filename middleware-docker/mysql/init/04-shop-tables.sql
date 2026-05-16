USE db_shop;

-- 商家表
CREATE TABLE IF NOT EXISTS merchants (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL UNIQUE COMMENT '关联用户ID',
    company_name    VARCHAR(200) COMMENT '公司名称',
    business_license VARCHAR(200) COMMENT '营业执照URL',
    legal_person    VARCHAR(50) COMMENT '法人姓名',
    id_card         VARCHAR(18) COMMENT '法人身份证',
    contact_phone   VARCHAR(20) COMMENT '联系电话',
    audit_status    TINYINT DEFAULT 0 COMMENT '0待审 1通过 2驳回',
    audit_reason    VARCHAR(500) COMMENT '审核原因',
    status          TINYINT DEFAULT 1 COMMENT '1正常 0禁用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_audit (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家表';

-- 店铺表
CREATE TABLE IF NOT EXISTS shops (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id     BIGINT NOT NULL UNIQUE COMMENT '商家ID',
    shop_name       VARCHAR(100) NOT NULL COMMENT '店铺名称',
    shop_logo       VARCHAR(500) COMMENT '店铺LOGO',
    shop_desc       VARCHAR(500) COMMENT '店铺简介',
    status          TINYINT DEFAULT 1 COMMENT '1营业 0停业',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';
