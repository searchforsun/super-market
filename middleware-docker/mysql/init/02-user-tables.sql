-- ============================================
-- 用户域数据库表 (db_user)
-- ============================================

USE db_user;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone           VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号',
    email           VARCHAR(100) COMMENT '邮箱',
    password_hash   VARCHAR(255) NOT NULL COMMENT 'BCrypt密码',
    nickname        VARCHAR(50) COMMENT '昵称',
    avatar_url      VARCHAR(500) COMMENT '头像URL',
    real_name       VARCHAR(50) COMMENT '实名',
    id_card         VARCHAR(18) COMMENT '身份证号',
    status          TINYINT DEFAULT 1 COMMENT '1正常 0禁用 -1注销',
    last_login_at   DATETIME COMMENT '最后登录时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 会员等级表
CREATE TABLE IF NOT EXISTS members (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    level           TINYINT DEFAULT 0 COMMENT '0普通 1青铜 2白银 3黄金 4钻石',
    points          INT DEFAULT 0 COMMENT '当前积分',
    total_points    INT DEFAULT 0 COMMENT '累计积分',
    growth_value    INT DEFAULT 0 COMMENT '成长值',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

-- 收货地址表
CREATE TABLE IF NOT EXISTS addresses (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    receiver_name   VARCHAR(50) NOT NULL COMMENT '收货人',
    receiver_phone  VARCHAR(20) NOT NULL COMMENT '收货人电话',
    province        VARCHAR(50) NOT NULL COMMENT '省',
    city            VARCHAR(50) NOT NULL COMMENT '市',
    district        VARCHAR(50) NOT NULL COMMENT '区',
    detail          VARCHAR(200) NOT NULL COMMENT '详细地址',
    is_default      TINYINT DEFAULT 0 COMMENT '是否默认 0否 1是',
    is_deleted      TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 角色定义表
CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(30) NOT NULL UNIQUE COMMENT '角色编码 ROLE_ADMIN/ROLE_MERCHANT/ROLE_USER',
    label       VARCHAR(50) COMMENT '显示名称',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user (user_id),
    INDEX idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 权限定义表（留扩展点）
CREATE TABLE IF NOT EXISTS permissions (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(50) NOT NULL UNIQUE COMMENT '权限编码',
    label       VARCHAR(50) COMMENT '显示名称',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';
