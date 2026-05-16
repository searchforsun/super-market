# 用户 RBAC 数据模型改进设计

> 2026-05-16 | 方案 B：标准 RBAC 模型

## 一、新增表结构

```sql
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
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user (user_id),
    INDEX idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 权限定义表（本期建表空置，留扩展点）
CREATE TABLE IF NOT EXISTS permissions (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(50) NOT NULL UNIQUE COMMENT 'product:audit / order:view',
    label       VARCHAR(50) COMMENT '显示名称',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';
```

## 二、种子数据

```sql
INSERT INTO roles (name, label) VALUES
('ROLE_USER', '普通用户'),
('ROLE_ADMIN', '管理员'),
('ROLE_MERCHANT', '商家');
```

## 三、改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `middleware-docker/mysql/init/02-user-tables.sql` | 修改 | 末尾追加 3 张建表语句 |
| `middleware-docker/mysql/init/20-seed-data.sql` | 修改 | 追加角色种子数据 |
| `super-market-services/service-auth/pom.xml` | 修改 | 已依赖 common-mybatis，无需改动 |
| `super-market-services/service-auth/.../entity/Role.java` | 新增 | 角色实体 |
| `super-market-services/service-auth/.../entity/UserRole.java` | 新增 | 用户角色关联实体 |
| `super-market-services/service-auth/.../mapper/RoleMapper.java` | 新增 | MyBatis-Plus Mapper |
| `super-market-services/service-auth/.../mapper/UserRoleMapper.java` | 新增 | MyBatis-Plus Mapper |
| `super-market-services/service-auth/.../AuthServiceImpl.java` | 修改 | 角色查询改 DB + 修复 refresh |
| `super-market-services/service-user/.../UserServiceImpl.java` | 修改 | register 时自动赋予 ROLE_USER |

## 四、核心逻辑变更

### 4.1 注册时自动赋予 ROLE_USER

```java
// UserServiceImpl.register() 末尾追加
UserRole userRole = new UserRole();
userRole.setUserId(user.getId());
userRole.setRoleId(1L); // ROLE_USER
userRoleMapper.insert(userRole);
```

### 4.2 登录时从 DB 查角色

```java
// AuthServiceImpl.login() — 改为 DB 查询
List<String> roles = userRoleMapper.selectList(
    new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId())
).stream()
.map(ur -> roleMapper.selectById(ur.getRoleId()).getName())
.collect(Collectors.toList());

if (roles.isEmpty()) {
    roles.add("ROLE_USER"); // 兜底：老用户没关联记录
}
// 管理员判断：改为查 DB 而非硬编码手机号
// 商家判断：保留 ShopDubboService + 自动写入 user_roles
```

### 4.3 refreshToken 修复

```java
// AuthServiceImpl.refreshToken() — 查 DB 恢复角色
List<String> roles = getRolesFromDb(userId);
String newAccessToken = jwtUtil.generateAccessToken(userId, roles);
```

## 五、兼容性

- 老用户（`user_roles` 表无记录）：兜底赋予 `ROLE_USER`
- 现有管理员手机号（13800000000, 13900000000）：通过种子数据在 `user_roles` 中关联 `ROLE_ADMIN`
- 商家角色：`ShopDubboService.hasMerchant()` 仍然调用，但结果写入 `user_roles`（首次登录自动同步）
- Gateway `RoleBasedFilter`：不感知此变更，继续读 `X-User-Roles` 头
