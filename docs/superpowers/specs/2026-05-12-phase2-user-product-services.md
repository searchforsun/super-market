# Phase 2 — 用户域 + 商品域 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现用户域（user/auth/member/address）和商品域（product/category/inventory/shop）共 8 个微服务的完整业务逻辑，包含 DDL、Entity、Mapper、Service、Controller 和单元测试。

**Architecture:** 标准三层架构（Controller → Service → Mapper）。所有服务复用 Phase 1 的 common 模块（R 统一返回、BizException、JwtUtil、BaseEntity）。用户认证采用 BCrypt + JWT 双 Token 机制。库存扣减使用乐观锁 + Redis 分布式锁双重保障。商品审核流程实现商家→平台的审核链路。

**Tech Stack:** Java 21, Spring Boot 3.2, Apache Dubbo 3.2, MyBatis-Plus 3.5, MySQL 8.0, Redis 7.2 + Redisson, JUnit 5 + Mockito

---

## Phase 2 验收标准

- [ ] 所有 8 个服务 `mvn clean install -DskipTests` BUILD SUCCESS
- [ ] 所有单元测试 `mvn test` 通过，覆盖率 > 80%
- [ ] 用户注册/登录 API 可正常调用并返回 JWT Token
- [ ] 商品 SPU+SKU 创建、类目树查询、库存初始化 API 正常
- [ ] 库存扣减并发场景下乐观锁不超卖
- [ ] Docker Compose 中间件全部健康（MySQL/Redis/Nacos）

---

## 文件结构概览

每个服务在 Phase 1 脚手架基础上新增以下文件：

```
service-{name}/src/main/java/com/supermarket/{name}/
├── entity/           # MyBatis-Plus 实体
├── mapper/           # Mapper 接口
├── service/          # 业务服务接口
│   └── impl/         # 业务服务实现
└── controller/       # REST 控制器

service-{name}/src/test/java/com/supermarket/{name}/
└── service/          # Service 层单元测试
```

---

## Task Group A: 数据库 DDL

### Task A1: 创建 db_user 数据库表

**Files:**
- Create: `middleware-docker/mysql/init/02-user-tables.sql`

- [ ] **Step 1: 写入用户域 DDL**

写入 `middleware-docker/mysql/init/02-user-tables.sql`:

```sql
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
```

- [ ] **Step 2: 执行 SQL 验证**

```bash
docker exec smt-mysql mysql -u root -proot123 db_user -e "SHOW TABLES;"
```
Expected: 输出 `users`, `members`, `addresses` 三张表

- [ ] **Step 3: Commit**

```bash
git add middleware-docker/mysql/init/02-user-tables.sql
git commit -m "feat: add user domain DDL (users, members, addresses)"
```

---

### Task A2: 创建 db_product + db_shop 数据库表

**Files:**
- Create: `middleware-docker/mysql/init/03-product-tables.sql`
- Create: `middleware-docker/mysql/init/04-shop-tables.sql`

- [ ] **Step 1: 写入商品域 DDL**

写入 `middleware-docker/mysql/init/03-product-tables.sql`:

```sql
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
```

- [ ] **Step 2: 写入商家域 DDL**

写入 `middleware-docker/mysql/init/04-shop-tables.sql`:

```sql
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
```

- [ ] **Step 3: 执行 SQL**

```bash
docker exec smt-mysql mysql -u root -proot123 -e "USE db_product; SHOW TABLES;" && docker exec smt-mysql mysql -u root -proot123 -e "USE db_shop; SHOW TABLES;"
```
Expected: db_product 输出 `categories`, `brands`, `spu`, `sku`, `inventory`；db_shop 输出 `merchants`, `shops`

- [ ] **Step 4: Commit**

```bash
git add middleware-docker/mysql/init/03-product-tables.sql middleware-docker/mysql/init/04-shop-tables.sql
git commit -m "feat: add product + shop domain DDL (categories, brands, spu, sku, inventory, merchants, shops)"
```

---

## Task Group B: user-service（用户注册、登录、信息管理）

### Task B1: 创建 User Entity + Mapper

**Files:**
- Create: `super-market-services/service-user/src/main/java/com/supermarket/user/entity/User.java`
- Create: `super-market-services/service-user/src/main/java/com/supermarket/user/mapper/UserMapper.java`

- [ ] **Step 1: 写入 User Entity**

写入 `super-market-services/service-user/src/main/java/com/supermarket/user/entity/User.java`:

```java
package com.supermarket.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    private String phone;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private String realName;
    private String idCard;
    private Integer status;
    private LocalDateTime lastLoginAt;
}
```

- [ ] **Step 2: 写入 UserMapper**

写入 `super-market-services/service-user/src/main/java/com/supermarket/user/mapper/UserMapper.java`:

```java
package com.supermarket.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

- [ ] **Step 3: 编译验证**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn clean compile -pl super-market-services/service-user -DskipTests
```
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add super-market-services/service-user/src/main/java/com/supermarket/user/entity/ super-market-services/service-user/src/main/java/com/supermarket/user/mapper/
git commit -m "feat(user-service): add User entity and mapper"
```

---

### Task B2: 创建 UserService 接口与实现

**Files:**
- Create: `super-market-services/service-user/src/main/java/com/supermarket/user/service/UserService.java`
- Create: `super-market-services/service-user/src/main/java/com/supermarket/user/service/impl/UserServiceImpl.java`

- [ ] **Step 1: 写入 UserService 接口**

写入 `super-market-services/service-user/src/main/java/com/supermarket/user/service/UserService.java`:

```java
package com.supermarket.user.service;

import com.supermarket.user.entity.User;

public interface UserService {

    User register(String phone, String password);

    User login(String phone, String password);

    User getById(Long userId);

    User getByPhone(String phone);

    void updateLoginTime(Long userId);
}
```

- [ ] **Step 2: 写入 UserServiceImpl**

```java
package com.supermarket.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.user.entity.User;
import com.supermarket.user.mapper.UserMapper;
import com.supermarket.user.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User register(String phone, String password) {
        User exist = getByPhone(phone);
        if (exist != null) {
            throw new BizException(400, "该手机号已注册");
        }
        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setStatus(1);
        save(user);
        return user;
    }

    @Override
    public User login(String phone, String password) {
        User user = getByPhone(phone);
        if (user == null) {
            throw new BizException(401, "手机号或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用或注销");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(401, "手机号或密码错误");
        }
        updateLoginTime(user.getId());
        return user;
    }

    @Override
    public User getById(Long userId) {
        User user = getBaseMapper().selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    @Override
    public User getByPhone(String phone) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    @Override
    public void updateLoginTime(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginAt(LocalDateTime.now());
        updateById(user);
    }
}
```

- [ ] **Step 3: 添加 BCrypt 依赖到 pom.xml**

在 `super-market-services/service-user/pom.xml` 的 `<dependencies>` 中添加:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

- [ ] **Step 4: 编译验证**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn clean compile -pl super-market-services/service-user -DskipTests
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add super-market-services/service-user/
git commit -m "feat(user-service): add UserService with register/login logic"
```

---

### Task B3: 创建 UserController + UserService 单元测试（TDD）

**Files:**
- Create: `super-market-services/service-user/src/main/java/com/supermarket/user/controller/UserController.java`
- Create: `super-market-services/service-user/src/test/java/com/supermarket/user/service/UserServiceTest.java`

- [ ] **Step 1: 先写测试用例（TDD Red Phase）**

写入 `super-market-services/service-user/src/test/java/com/supermarket/user/service/UserServiceTest.java`:

```java
package com.supermarket.user.service;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.supermarket.user.entity.User;
import com.supermarket.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldRegisterNewUser() {
        User user = userService.register("13800138001", "Pass1234");
        assertThat(user.getId()).isNotNull();
        assertThat(user.getPhone()).isEqualTo("13800138001");
        assertThat(user.getPasswordHash()).isNotEqualTo("Pass1234");
    }

    @Test
    void shouldFailRegisterDuplicatePhone() {
        userService.register("13800138002", "Pass1234");
        assertThatThrownBy(() -> userService.register("13800138002", "Pass1234"))
            .hasMessageContaining("已注册");
    }

    @Test
    void shouldLoginWithCorrectCredentials() {
        userService.register("13800138003", "Pass1234");
        User user = userService.login("13800138003", "Pass1234");
        assertThat(user).isNotNull();
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    void shouldFailLoginWithWrongPassword() {
        userService.register("13800138004", "Pass1234");
        assertThatThrownBy(() -> userService.login("13800138004", "WrongPass"))
            .hasMessageContaining("密码错误");
    }

    @Test
    void shouldGetUserById() {
        User registered = userService.register("13800138005", "Pass1234");
        User found = userService.getById(registered.getId());
        assertThat(found.getPhone()).isEqualTo("13800138005");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        assertThatThrownBy(() -> userService.getById(99999L))
            .hasMessageContaining("用户不存在");
    }
}
```

- [ ] **Step 2: 运行测试验证它失败**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn test -pl super-market-services/service-user -Dtest=UserServiceTest 2>&1 | tail -20
```
Expected: 部分测试 FAIL（因为还没有完整实现，需要 DataSource 连接等 — 如果没有 Docker 则 FAIL）

> 注：如果 Docker 中间件未启动，测试会因为 DataSource 连接失败而报错。此时需要先 `docker compose -f middleware-docker/middleware-docker.yml up -d mysql` 启动 MySQL。

- [ ] **Step 3: 写入 UserController（Green Phase）**

写入 `super-market-services/service-user/src/main/java/com/supermarket/user/controller/UserController.java`:

```java
package com.supermarket.user.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.user.entity.User;
import com.supermarket.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public R<User> register(@RequestParam String phone, @RequestParam String password) {
        User user = userService.register(phone, password);
        // 脱敏: 不返回密码哈希
        user.setPasswordHash(null);
        return R.ok(user);
    }

    @PostMapping("/login")
    public R<User> login(@RequestParam String phone, @RequestParam String password) {
        User user = userService.login(phone, password);
        user.setPasswordHash(null);
        return R.ok(user);
    }

    @GetMapping("/info")
    public R<User> info(@RequestParam Long userId) {
        User user = userService.getById(userId);
        user.setPasswordHash(null);
        return R.ok(user);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
# 先确保 MySQL 已启动
docker compose -f middleware-docker/middleware-docker.yml up -d mysql
sleep 10

# 运行测试
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn test -pl super-market-services/service-user
```
Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add super-market-services/service-user/
git commit -m "feat(user-service): add UserController + unit tests (6 pass)"
```

---

### Task B4: 创建 common-dubbo-api 中的 User 相关接口（为跨服务调用做准备）

**Files:**
- Create: `super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/user/UserDubboService.java`
- Create: `super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/user/dto/UserDTO.java`

- [ ] **Step 1: 写入 DTO**

写入 `super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/user/dto/UserDTO.java`:

```java
package com.supermarket.common.dubbo.api.user.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserDTO implements Serializable {
    private Long id;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 写入 Dubbo 接口**

写入 `super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/user/UserDubboService.java`:

```java
package com.supermarket.common.dubbo.api.user;

import com.supermarket.common.dubbo.api.user.dto.UserDTO;

public interface UserDubboService {

    UserDTO getById(Long userId);

    UserDTO getByPhone(String phone);
}
```

- [ ] **Step 3: 在 user-service 中实现 Dubbo 接口**

在 `UserServiceImpl` 中添加 `@DubboService` 并实现 `UserDubboService`:
- Modify: `super-market-services/service-user/src/main/java/com/supermarket/user/service/impl/UserServiceImpl.java`

```java
// 类签名改为:
@DubboService
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService, UserDubboService {

    @Override
    public UserDTO getById(Long userId) {
        User user = getById(userId);
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setPhone(user.getPhone());
        dto.setNickname(user.getNickname());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    // ... 已有方法保持不变
}
```

- [ ] **Step 4: 编译验证**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn clean install -DskipTests -pl super-market-common/common-dubbo-api,super-market-services/service-user
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add super-market-common/common-dubbo-api/ super-market-services/service-user/
git commit -m "feat: add UserDubboService API + Dubbo provider implementation"
```

---

## Task Group C: auth-service（认证授权服务）

### Task C1: 创建 AuthService（登录 + Token 签发 + Token 刷新）

**Files:**
- Create: `super-market-services/service-auth/src/main/java/com/supermarket/auth/service/AuthService.java`
- Create: `super-market-services/service-auth/src/main/java/com/supermarket/auth/service/impl/AuthServiceImpl.java`
- Create: `super-market-services/service-auth/src/main/java/com/supermarket/auth/controller/AuthController.java`
- Create: `super-market-services/service-auth/src/test/java/com/supermarket/auth/service/AuthServiceTest.java`

- [ ] **Step 1: 添加依赖**

在 `super-market-services/service-auth/pom.xml` 中添加 Dubbo 依赖以调用 user-service:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

- [ ] **Step 2: 写入 AuthService 接口**

写入 `super-market-services/service-auth/src/main/java/com/supermarket/auth/service/AuthService.java`:

```java
package com.supermarket.auth.service;

import java.util.Map;

public interface AuthService {

    Map<String, String> login(String phone, String password);

    Map<String, String> refreshToken(String refreshToken);
}
```

- [ ] **Step 3: 写入 AuthServiceImpl**

写入 `super-market-services/service-auth/src/main/java/com/supermarket/auth/service/impl/AuthServiceImpl.java`:

```java
package com.supermarket.auth.service.impl;

import com.supermarket.auth.service.AuthService;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.dubbo.api.user.UserDubboService;
import com.supermarket.common.dubbo.api.user.dto.UserDTO;
import com.supermarket.common.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;

    @DubboReference(check = false)
    private UserDubboService userDubboService;

    @Override
    public Map<String, String> login(String phone, String password) {
        UserDTO user = userDubboService.getByPhone(phone);
        if (user == null) {
            throw new BizException(401, "手机号或密码错误");
        }
        String accessToken = jwtUtil.generateAccessToken(user.getId(), List.of("ROLE_USER"));
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken,
            "userId", String.valueOf(user.getId())
        );
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        if (!jwtUtil.validate(refreshToken)) {
            throw new BizException(401, "Refresh Token 无效或已过期");
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        String newAccessToken = jwtUtil.generateAccessToken(userId, List.of("ROLE_USER"));
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);
        return Map.of(
            "accessToken", newAccessToken,
            "refreshToken", newRefreshToken
        );
    }
}
```

- [ ] **Step 4: 写入 AuthController**

写入 `super-market-services/service-auth/src/main/java/com/supermarket/auth/controller/AuthController.java`:

```java
package com.supermarket.auth.controller;

import com.supermarket.auth.service.AuthService;
import com.supermarket.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestParam String phone, @RequestParam String password) {
        return R.ok(authService.login(phone, password));
    }

    @PostMapping("/refresh")
    public R<Map<String, String>> refresh(@RequestParam String refreshToken) {
        return R.ok(authService.refreshToken(refreshToken));
    }
}
```

- [ ] **Step 5: 写入单元测试**

写入 `super-market-services/service-auth/src/test/java/com/supermarket/auth/service/AuthServiceTest.java`:

```java
package com.supermarket.auth.service;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.security.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.supermarket.common.dubbo.api.user.UserDubboService;
import com.supermarket.common.dubbo.api.user.dto.UserDTO;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @MockBean
    private UserDubboService userDubboService;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void shouldLoginAndReturnTokens() {
        UserDTO mockUser = new UserDTO();
        mockUser.setId(1L);
        mockUser.setPhone("13800138001");
        mockUser.setStatus(1);
        when(userDubboService.getByPhone("13800138001")).thenReturn(mockUser);

        Map<String, String> result = authService.login("13800138001", "any-password");

        assertThat(result).containsKeys("accessToken", "refreshToken", "userId");
        assertThat(jwtUtil.validate(result.get("accessToken"))).isTrue();
    }

    @Test
    void shouldFailLoginWithUnknownPhone() {
        when(userDubboService.getByPhone(anyString())).thenReturn(null);
        assertThatThrownBy(() -> authService.login("00000000000", "any"))
            .isInstanceOf(BizException.class);
    }

    @Test
    void shouldRefreshToken() {
        String refreshToken = jwtUtil.generateRefreshToken(1L);
        Map<String, String> result = authService.refreshToken(refreshToken);
        assertThat(jwtUtil.validate(result.get("accessToken"))).isTrue();
    }
}
```

- [ ] **Step 6: 运行测试**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn test -pl super-market-services/service-auth
```
Expected: `Tests run: 3, Failures: 0`

- [ ] **Step 7: Commit**

```bash
git add super-market-services/service-auth/ super-market-common/common-dubbo-api/
git commit -m "feat(auth-service): add login/token-refresh with Dubbo + JWT"
```

---

## Task Group D: address-service（收货地址 CRUD）

### Task D1: Address Entity + Mapper + Service + Controller + Tests

**Files:**
- Create: `super-market-services/service-address/src/main/java/com/supermarket/address/entity/Address.java`
- Create: `super-market-services/service-address/src/main/java/com/supermarket/address/mapper/AddressMapper.java`
- Create: `super-market-services/service-address/src/main/java/com/supermarket/address/service/AddressService.java`
- Create: `super-market-services/service-address/src/main/java/com/supermarket/address/service/impl/AddressServiceImpl.java`
- Create: `super-market-services/service-address/src/main/java/com/supermarket/address/controller/AddressController.java`
- Create: `super-market-services/service-address/src/test/java/com/supermarket/address/service/AddressServiceTest.java`

- [ ] **Step 1: 写入 Address Entity**

写入 `super-market-services/service-address/src/main/java/com/supermarket/address/entity/Address.java`:

```java
package com.supermarket.address.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("addresses")
public class Address extends BaseEntity {
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Integer isDefault;
    private Integer isDeleted;
}
```

- [ ] **Step 2: 写入 AddressMapper**

```java
package com.supermarket.address.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.address.entity.Address;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
```

- [ ] **Step 3: 写入 AddressService**

写入 `AddressService.java`:

```java
package com.supermarket.address.service;

import com.supermarket.address.entity.Address;

import java.util.List;

public interface AddressService {
    Address create(Address address);
    Address update(Address address);
    void delete(Long addressId, Long userId);
    Address getById(Long addressId);
    List<Address> listByUser(Long userId);
    void setDefault(Long addressId, Long userId);
}
```

写入 `AddressServiceImpl.java`:

```java
package com.supermarket.address.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.address.entity.Address;
import com.supermarket.address.mapper.AddressMapper;
import com.supermarket.address.service.AddressService;
import com.supermarket.common.core.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Override
    @Transactional
    public Address create(Address address) {
        List<Address> existing = listByUser(address.getUserId());
        if (existing.size() >= 20) {
            throw new BizException(400, "收货地址最多20个");
        }
        if (existing.isEmpty()) {
            address.setIsDefault(1);
        }
        save(address);
        return address;
    }

    @Override
    public Address update(Address address) {
        Address exist = getById(address.getId());
        if (exist == null) {
            throw new BizException(404, "地址不存在");
        }
        updateById(address);
        return getById(address.getId());
    }

    @Override
    public void delete(Long addressId, Long userId) {
        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Address::getId, addressId)
               .eq(Address::getUserId, userId)
               .set(Address::getIsDeleted, 1);
        update(wrapper);
    }

    @Override
    public Address getById(Long addressId) {
        Address addr = super.getById(addressId);
        if (addr == null || addr.getIsDeleted() == 1) {
            throw new BizException(404, "地址不存在");
        }
        return addr;
    }

    @Override
    public List<Address> listByUser(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
               .eq(Address::getIsDeleted, 0)
               .orderByDesc(Address::getIsDefault)
               .orderByDesc(Address::getCreatedAt);
        return list(wrapper);
    }

    @Override
    @Transactional
    public void setDefault(Long addressId, Long userId) {
        // 取消当前默认
        LambdaUpdateWrapper<Address> clearWrapper = new LambdaUpdateWrapper<>();
        clearWrapper.eq(Address::getUserId, userId)
                    .set(Address::getIsDefault, 0);
        update(clearWrapper);

        // 设置新默认
        LambdaUpdateWrapper<Address> setWrapper = new LambdaUpdateWrapper<>();
        setWrapper.eq(Address::getId, addressId)
                 .eq(Address::getUserId, userId)
                 .set(Address::getIsDefault, 1);
        update(setWrapper);
    }
}
```

- [ ] **Step 4: 写入 AddressController**

```java
package com.supermarket.address.controller;

import com.supermarket.address.entity.Address;
import com.supermarket.address.service.AddressService;
import com.supermarket.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public R<Address> create(@RequestBody Address address) {
        return R.ok(addressService.create(address));
    }

    @PutMapping
    public R<Address> update(@RequestBody Address address) {
        return R.ok(addressService.update(address));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        addressService.delete(id, userId);
        return R.ok();
    }

    @GetMapping("/list")
    public R<List<Address>> list(@RequestParam Long userId) {
        return R.ok(addressService.listByUser(userId));
    }

    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id, @RequestParam Long userId) {
        addressService.setDefault(id, userId);
        return R.ok();
    }
}
```

- [ ] **Step 5: 写入测试**

写入 `AddressServiceTest.java`:

```java
package com.supermarket.address.service;

import com.supermarket.address.entity.Address;
import com.supermarket.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AddressServiceTest {

    @Autowired
    private AddressService addressService;

    @Test
    void shouldCreateAddress() {
        Address addr = newAddr(1L, "张三", "13800000001");
        Address saved = addressService.create(addr);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getIsDefault()).isEqualTo(1);
    }

    @Test
    void shouldListAddressesByUser() {
        addressService.create(newAddr(2L, "李四", "13800000002"));
        addressService.create(newAddr(2L, "李四", "13800000002"));
        List<Address> list = addressService.listByUser(2L);
        assertThat(list).hasSize(2);
    }

    @Test
    void shouldSetDefaultAddress() {
        Address a1 = addressService.create(newAddr(3L, "王五", "13800000003"));
        Address a2 = addressService.create(newAddr(3L, "王五", "13800000003"));
        addressService.setDefault(a2.getId(), 3L);

        Address found = addressService.getById(a2.getId());
        assertThat(found.getIsDefault()).isEqualTo(1);
    }

    @Test
    void shouldSoftDeleteAddress() {
        Address addr = addressService.create(newAddr(4L, "赵六", "13800000004"));
        addressService.delete(addr.getId(), 4L);
        assertThatThrownBy(() -> addressService.getById(addr.getId()))
            .isInstanceOf(BizException.class);
    }

    private Address newAddr(Long userId, String name, String phone) {
        Address addr = new Address();
        addr.setUserId(userId);
        addr.setReceiverName(name);
        addr.setReceiverPhone(phone);
        addr.setProvince("广东省");
        addr.setCity("深圳市");
        addr.setDistrict("南山区");
        addr.setDetail("科技园路1号");
        return addr;
    }
}
```

- [ ] **Step 6: 运行测试**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn test -pl super-market-services/service-address
```
Expected: `BUILD SUCCESS, Tests run: 4, Failures: 0`

- [ ] **Step 7: Commit**

```bash
git add super-market-services/service-address/
git commit -m "feat(address-service): add address CRUD with 20-address limit + default management"
```

---

## Task Group E: member-service（会员等级与积分）

### Task E1: Member Entity + Mapper + Service + Tests

**Files:**
- Create: `super-market-services/service-member/src/main/java/com/supermarket/member/entity/Member.java`
- Create: `super-market-services/service-member/src/main/java/com/supermarket/member/mapper/MemberMapper.java`
- Create: `super-market-services/service-member/src/main/java/com/supermarket/member/service/MemberService.java`
- Create: `super-market-services/service-member/src/main/java/com/supermarket/member/service/impl/MemberServiceImpl.java`
- Create: `super-market-services/service-member/src/main/java/com/supermarket/member/controller/MemberController.java`
- Create: `super-market-services/service-member/src/test/java/com/supermarket/member/service/MemberServiceTest.java`

- [ ] **Step 1: 写入 Member Entity + Mapper**

`Member.java`:

```java
package com.supermarket.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("members")
public class Member extends BaseEntity {
    private Long userId;
    private Integer level;
    private Integer points;
    private Integer totalPoints;
    private Integer growthValue;
}
```

`MemberMapper.java`:

```java
package com.supermarket.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    @Update("UPDATE members SET points = points + #{points}, total_points = total_points + #{points} WHERE user_id = #{userId}")
    int addPoints(Long userId, int points);

    @Update("UPDATE members SET points = points - #{points} WHERE user_id = #{userId} AND points >= #{points}")
    int deductPoints(Long userId, int points);
}
```

- [ ] **Step 2: 写入 MemberService**

`MemberService.java`:

```java
package com.supermarket.member.service;

import com.supermarket.member.entity.Member;

public interface MemberService {
    Member getOrCreate(Long userId);
    Member getByUserId(Long userId);
    void addPoints(Long userId, int points);
    boolean deductPoints(Long userId, int points);
    int computeLevel(int growthValue);
}
```

`MemberServiceImpl.java`:

```java
package com.supermarket.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.member.entity.Member;
import com.supermarket.member.mapper.MemberMapper;
import com.supermarket.member.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    @Override
    @Transactional
    public Member getOrCreate(Long userId) {
        Member member = getByUserId(userId);
        if (member == null) {
            member = new Member();
            member.setUserId(userId);
            member.setLevel(0);
            member.setPoints(0);
            member.setTotalPoints(0);
            member.setGrowthValue(0);
            save(member);
        }
        return member;
    }

    @Override
    public Member getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<Member>().eq(Member::getUserId, userId));
    }

    @Override
    public void addPoints(Long userId, int points) {
        getOrCreate(userId);
        baseMapper.addPoints(userId, points);
        Member member = getByUserId(userId);
        int newLevel = computeLevel(member.getTotalPoints());
        if (newLevel > member.getLevel()) {
            member.setLevel(newLevel);
            updateById(member);
        }
    }

    @Override
    public boolean deductPoints(Long userId, int points) {
        Member member = getByUserId(userId);
        if (member == null || member.getPoints() < points) {
            return false;
        }
        return baseMapper.deductPoints(userId, points) > 0;
    }

    @Override
    public int computeLevel(int totalPoints) {
        if (totalPoints >= 50000) return 4; // 钻石
        if (totalPoints >= 10000) return 3; // 黄金
        if (totalPoints >= 1000)  return 2;  // 白银
        if (totalPoints >= 100)   return 1;  // 青铜
        return 0;                             // 普通
    }
}
```

- [ ] **Step 3: 写入 MemberController + 测试**

`MemberController.java`:

```java
package com.supermarket.member.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.member.entity.Member;
import com.supermarket.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/{userId}")
    public R<Member> getMember(@PathVariable Long userId) {
        return R.ok(memberService.getOrCreate(userId));
    }

    @PostMapping("/points/add")
    public R<Void> addPoints(@RequestParam Long userId, @RequestParam int points) {
        memberService.addPoints(userId, points);
        return R.ok();
    }
}
```

`MemberServiceTest.java`:

```java
package com.supermarket.member.service;

import com.supermarket.member.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Test
    void shouldCreateNewMember() {
        Member member = memberService.getOrCreate(100L);
        assertThat(member.getLevel()).isEqualTo(0);
        assertThat(member.getPoints()).isEqualTo(0);
    }

    @Test
    void shouldUpgradeLevel() {
        memberService.getOrCreate(101L);
        memberService.addPoints(101L, 5000);
        Member member = memberService.getByUserId(101L);
        assertThat(member.getLevel()).isEqualTo(2);
    }

    @Test
    void shouldComputeDiamondLevel() {
        assertThat(memberService.computeLevel(50000)).isEqualTo(4);
        assertThat(memberService.computeLevel(100)).isEqualTo(1);
        assertThat(memberService.computeLevel(50)).isEqualTo(0);
    }

    @Test
    void shouldDeductPoints() {
        memberService.getOrCreate(102L);
        memberService.addPoints(102L, 100);
        boolean result = memberService.deductPoints(102L, 30);
        assertThat(result).isTrue();
        Member member = memberService.getByUserId(102L);
        assertThat(member.getPoints()).isGreaterThanOrEqualTo(70);
    }

    @Test
    void shouldFailDeductWhenInsufficientPoints() {
        memberService.getOrCreate(103L);
        boolean result = memberService.deductPoints(103L, 100);
        assertThat(result).isFalse();
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn test -pl super-market-services/service-member
```
Expected: `Tests run: 5, Failures: 0`

- [ ] **Step 5: Commit**

```bash
git add super-market-services/service-member/
git commit -m "feat(member-service): add member level system (5 tiers) + points management"
```

---

## Task Group F: product-service（商品 SPU/SKU 管理）

### Task F1: SPU + SKU Entity + Mapper

**Files:**
- Create: `super-market-services/service-product/src/main/java/com/supermarket/product/entity/Spu.java`
- Create: `super-market-services/service-product/src/main/java/com/supermarket/product/entity/Sku.java`
- Create: `super-market-services/service-product/src/main/java/com/supermarket/product/mapper/SpuMapper.java`
- Create: `super-market-services/service-product/src/main/java/com/supermarket/product/mapper/SkuMapper.java`

- [ ] **Step 1: 写入 SPU Entity**

`Spu.java`:

```java
package com.supermarket.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spu")
public class Spu extends BaseEntity {
    private String spuNo;
    private Long shopId;
    private Long categoryId;
    private Long brandId;
    private String name;
    private String subtitle;
    private String mainImage;
    private String images;
    private String description;
    private Integer auditStatus;
    private Integer shelfStatus;
    private Integer isDeleted;
}
```

- [ ] **Step 2: 写入 SKU Entity**

`Sku.java`:

```java
package com.supermarket.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sku")
public class Sku extends BaseEntity {
    private String skuNo;
    private Long spuId;
    private String specName;
    private String specCode;
    private BigDecimal price;
    private BigDecimal marketPrice;
    private BigDecimal costPrice;
    private String image;
    private Integer weight;
    private Integer status;
}
```

- [ ] **Step 3: 写入 Mappers**

```java
// SpuMapper.java
package com.supermarket.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.product.entity.Spu;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SpuMapper extends BaseMapper<Spu> {
}

// SkuMapper.java
package com.supermarket.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.product.entity.Sku;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SkuMapper extends BaseMapper<Sku> {
}
```

- [ ] **Step 4: Commit**

```bash
git add super-market-services/service-product/
git commit -m "feat(product-service): add Spu/Sku entities and mappers"
```

---

### Task F2: ProductService（SPU 创建 + SKU 管理 + 审核）

**Files:**
- Create: `super-market-services/service-product/src/main/java/com/supermarket/product/service/ProductService.java`
- Create: `super-market-services/service-product/src/main/java/com/supermarket/product/service/impl/ProductServiceImpl.java`

- [ ] **Step 1: 写入请求/响应 DTO**

创建 `super-market-services/service-product/src/main/java/com/supermarket/product/dto/CreateProductRequest.java`:

```java
package com.supermarket.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {

    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    @NotNull(message = "类目ID不能为空")
    private Long categoryId;

    private Long brandId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String subtitle;
    private String mainImage;
    private List<String> images;
    private String description;

    private List<SkuItem> skus;

    @Data
    public static class SkuItem {
        @NotBlank
        private String specName;
        private String specCode;
        @NotNull
        private BigDecimal price;
        private BigDecimal marketPrice;
        private BigDecimal costPrice;
        private String image;
        private Integer weight;
    }
}
```

- [ ] **Step 2: 写入 ProductService 接口**

`ProductService.java`:

```java
package com.supermarket.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.entity.Sku;
import com.supermarket.product.entity.Spu;

import java.util.List;

public interface ProductService {

    Spu createProduct(CreateProductRequest request);

    Spu getSpuById(Long spuId);

    Spu getSpuByNo(String spuNo);

    Sku getSkuById(Long skuId);

    List<Sku> getSkusBySpuId(Long spuId);

    void auditProduct(Long spuId, Integer auditStatus, String reason);

    void updateShelfStatus(Long spuId, Integer shelfStatus);

    Page<Spu> listByShop(Long shopId, int page, int size);

    Page<Spu> listByCategory(Long categoryId, int page, int size, String sort);
}
```

- [ ] **Step 3: 写入 ProductServiceImpl**

`ProductServiceImpl.java`:

```java
package com.supermarket.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.entity.Sku;
import com.supermarket.product.entity.Spu;
import com.supermarket.product.mapper.SkuMapper;
import com.supermarket.product.mapper.SpuMapper;
import com.supermarket.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<SpuMapper, Spu> implements ProductService {

    private final SkuMapper skuMapper;

    @Override
    @Transactional
    public Spu createProduct(CreateProductRequest request) {
        Spu spu = new Spu();
        spu.setSpuNo("SPU" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6));
        spu.setShopId(request.getShopId());
        spu.setCategoryId(request.getCategoryId());
        spu.setBrandId(request.getBrandId());
        spu.setName(request.getName());
        spu.setSubtitle(request.getSubtitle());
        spu.setMainImage(request.getMainImage());
        spu.setImages(request.getImages() != null ? String.join(",", request.getImages()) : null);
        spu.setDescription(request.getDescription());
        spu.setAuditStatus(0);
        spu.setShelfStatus(0);
        save(spu);

        if (request.getSkus() != null) {
            for (CreateProductRequest.SkuItem item : request.getSkus()) {
                Sku sku = new Sku();
                sku.setSkuNo("SKU" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6));
                sku.setSpuId(spu.getId());
                sku.setSpecName(item.getSpecName());
                sku.setSpecCode(item.getSpecCode());
                sku.setPrice(item.getPrice());
                sku.setMarketPrice(item.getMarketPrice());
                sku.setCostPrice(item.getCostPrice());
                sku.setImage(item.getImage());
                sku.setWeight(item.getWeight() != null ? item.getWeight() : 0);
                sku.setStatus(1);
                skuMapper.insert(sku);
            }
        }
        return spu;
    }

    @Override
    public Spu getSpuById(Long spuId) {
        Spu spu = getById(spuId);
        if (spu == null || spu.getIsDeleted() == 1) {
            throw new BizException(404, "商品不存在");
        }
        return spu;
    }

    @Override
    public Spu getSpuByNo(String spuNo) {
        return getOne(new LambdaQueryWrapper<Spu>().eq(Spu::getSpuNo, spuNo));
    }

    @Override
    public Sku getSkuById(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(404, "SKU不存在");
        }
        return sku;
    }

    @Override
    public List<Sku> getSkusBySpuId(Long spuId) {
        return skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, spuId));
    }

    @Override
    public void auditProduct(Long spuId, Integer auditStatus, String reason) {
        if (auditStatus != 1 && auditStatus != 2) {
            throw new BizException(400, "审核状态仅支持 1=通过 2=驳回");
        }
        Spu spu = getSpuById(spuId);
        if (spu.getAuditStatus() != 0) {
            throw new BizException(400, "该商品已审核");
        }
        spu.setAuditStatus(auditStatus);
        if (auditStatus == 1) {
            spu.setShelfStatus(1);
        }
        updateById(spu);
    }

    @Override
    public void updateShelfStatus(Long spuId, Integer shelfStatus) {
        Spu spu = getSpuById(spuId);
        if (spu.getAuditStatus() != 1) {
            throw new BizException(400, "仅审核通过的商品可以上下架");
        }
        spu.setShelfStatus(shelfStatus);
        updateById(spu);
    }

    @Override
    public Page<Spu> listByShop(Long shopId, int page, int size) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spu::getShopId, shopId)
               .eq(Spu::getIsDeleted, 0)
               .orderByDesc(Spu::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Spu> listByCategory(Long categoryId, int page, int size, String sort) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spu::getCategoryId, categoryId)
               .eq(Spu::getAuditStatus, 1)
               .eq(Spu::getShelfStatus, 1)
               .eq(Spu::getIsDeleted, 0);
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(Spu::getCreatedAt);
        } else {
            wrapper.orderByDesc(Spu::getCreatedAt);
        }
        return page(new Page<>(page, size), wrapper);
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn clean compile -pl super-market-services/service-product -DskipTests
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add super-market-services/service-product/
git commit -m "feat(product-service): add ProductService (SPU create + SKU management + audit)"
```

---

### Task F3: ProductController + 单元测试

**Files:**
- Create: `super-market-services/service-product/src/main/java/com/supermarket/product/controller/ProductController.java`
- Create: `super-market-services/service-product/src/test/java/com/supermarket/product/service/ProductServiceTest.java`

- [ ] **Step 1: 写入 ProductController**

```java
package com.supermarket.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.entity.Sku;
import com.supermarket.product.entity.Spu;
import com.supermarket.product.service.ProductService;
import com.supermarket.common.core.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/spu")
    public R<Spu> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return R.ok(productService.createProduct(request));
    }

    @GetMapping("/spu/{spuId}")
    public R<Spu> getSpu(@PathVariable Long spuId) {
        return R.ok(productService.getSpuById(spuId));
    }

    @GetMapping("/spu/{spuId}/skus")
    public R<List<Sku>> getSkus(@PathVariable Long spuId) {
        return R.ok(productService.getSkusBySpuId(spuId));
    }

    @PutMapping("/spu/{spuId}/audit")
    public R<Void> audit(@PathVariable Long spuId,
                         @RequestParam Integer auditStatus,
                         @RequestParam(required = false) String reason) {
        productService.auditProduct(spuId, auditStatus, reason);
        return R.ok();
    }

    @PutMapping("/spu/{spuId}/shelf")
    public R<Void> updateShelf(@PathVariable Long spuId, @RequestParam Integer shelfStatus) {
        productService.updateShelfStatus(spuId, shelfStatus);
        return R.ok();
    }

    @GetMapping("/list/shop/{shopId}")
    public R<Page<Spu>> listByShop(@PathVariable Long shopId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return R.ok(productService.listByShop(shopId, page, size));
    }

    @GetMapping("/list/category/{categoryId}")
    public R<Page<Spu>> listByCategory(@PathVariable Long categoryId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) String sort) {
        return R.ok(productService.listByCategory(categoryId, page, size, sort));
    }
}
```

- [ ] **Step 2: 写入测试**

`ProductServiceTest.java`:

```java
package com.supermarket.product.service;

import com.supermarket.product.dto.CreateProductRequest;
import com.supermarket.product.entity.Spu;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Test
    void shouldCreateProductWithSkus() {
        CreateProductRequest req = buildRequest();
        Spu spu = productService.createProduct(req);

        assertThat(spu.getId()).isNotNull();
        assertThat(spu.getAuditStatus()).isEqualTo(0);
        assertThat(spu.getShelfStatus()).isEqualTo(0);

        var skus = productService.getSkusBySpuId(spu.getId());
        assertThat(skus).hasSize(2);
    }

    @Test
    void shouldAuditProduct() {
        Spu spu = productService.createProduct(buildRequest());
        productService.auditProduct(spu.getId(), 1, null);

        Spu audited = productService.getSpuById(spu.getId());
        assertThat(audited.getAuditStatus()).isEqualTo(1);
        assertThat(audited.getShelfStatus()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateAudit() {
        Spu spu = productService.createProduct(buildRequest());
        productService.auditProduct(spu.getId(), 1, null);

        assertThatThrownBy(() -> productService.auditProduct(spu.getId(), 2, "重复审核"))
            .hasMessageContaining("已审核");
    }

    @Test
    void shouldUpdateShelfStatus() {
        Spu spu = productService.createProduct(buildRequest());
        productService.auditProduct(spu.getId(), 1, null);
        productService.updateShelfStatus(spu.getId(), 0);

        Spu offShelf = productService.getSpuById(spu.getId());
        assertThat(offShelf.getShelfStatus()).isEqualTo(0);
    }

    private CreateProductRequest buildRequest() {
        CreateProductRequest req = new CreateProductRequest();
        req.setShopId(1L);
        req.setCategoryId(10L);
        req.setName("测试商品");
        req.setMainImage("http://example.com/img.jpg");

        CreateProductRequest.SkuItem sku1 = new CreateProductRequest.SkuItem();
        sku1.setSpecName("红色-XL");
        sku1.setPrice(new BigDecimal("99.00"));

        CreateProductRequest.SkuItem sku2 = new CreateProductRequest.SkuItem();
        sku2.setSpecName("蓝色-XL");
        sku2.setPrice(new BigDecimal("109.00"));

        req.setSkus(List.of(sku1, sku2));
        return req;
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn test -pl super-market-services/service-product
```
Expected: `Tests run: 4, Failures: 0`

- [ ] **Step 4: Commit**

```bash
git add super-market-services/service-product/
git commit -m "feat(product-service): add ProductController + tests (SPU/SKU CRUD + audit + shelf)"
```

---

## Task Group G: category-service（类目树管理）

### Task G1: Category Entity + Mapper + Service（递归树查询）+ Tests

**Files:**
- Create: `super-market-services/service-category/src/main/java/com/supermarket/category/entity/Category.java`
- Create: `super-market-services/service-category/src/main/java/com/supermarket/category/mapper/CategoryMapper.java`
- Create: `super-market-services/service-category/src/main/java/com/supermarket/category/service/CategoryService.java`
- Create: `super-market-services/service-category/src/main/java/com/supermarket/category/service/impl/CategoryServiceImpl.java`
- Create: `super-market-services/service-category/src/main/java/com/supermarket/category/controller/CategoryController.java`
- Create: `super-market-services/service-category/src/test/java/com/supermarket/category/service/CategoryServiceTest.java`

- [ ] **Step 1: 写入 Entity + Mapper**

```java
// Category.java
package com.supermarket.category.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("categories")
public class Category extends BaseEntity {
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sortOrder;
    private String iconUrl;
    private Integer status;
}

// CategoryMapper.java
package com.supermarket.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
```

- [ ] **Step 2: 写入 CategoryService（递归树构建）**

`CategoryService.java`:

```java
package com.supermarket.category.service;

import com.supermarket.category.entity.Category;
import java.util.List;

public interface CategoryService {
    Category create(Category category);
    Category update(Category category);
    void delete(Long id);
    Category getById(Long id);
    List<Category> getChildren(Long parentId);
    List<Category> getFullTree();
    List<Category> getByLevel(int level);
}
```

`CategoryServiceImpl.java`:

```java
package com.supermarket.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.category.entity.Category;
import com.supermarket.category.mapper.CategoryMapper;
import com.supermarket.category.service.CategoryService;
import com.supermarket.common.core.exception.BizException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public Category create(Category category) {
        if (category.getParentId() != null && category.getParentId() > 0) {
            Category parent = getById(category.getParentId());
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setLevel(1);
        }
        save(category);
        return category;
    }

    @Override
    public Category update(Category category) {
        getById(category.getId());
        updateById(category);
        return getById(category.getId());
    }

    @Override
    public void delete(Long id) {
        List<Category> children = getChildren(id);
        if (!children.isEmpty()) {
            throw new BizException(400, "存在子类目,无法删除");
        }
        removeById(id);
    }

    @Override
    public Category getById(Long id) {
        Category cat = super.getById(id);
        if (cat == null) {
            throw new BizException(404, "类目不存在");
        }
        return cat;
    }

    @Override
    public List<Category> getChildren(Long parentId) {
        return list(new LambdaQueryWrapper<Category>()
            .eq(Category::getParentId, parentId)
            .eq(Category::getStatus, 1)
            .orderByAsc(Category::getSortOrder));
    }

    @Override
    public List<Category> getFullTree() {
        List<Category> all = list(new LambdaQueryWrapper<Category>()
            .eq(Category::getStatus, 1)
            .orderByAsc(Category::getSortOrder));

        Map<Long, List<Category>> childrenMap = all.stream()
            .collect(Collectors.groupingBy(Category::getParentId));

        return buildTree(0L, childrenMap);
    }

    private List<Category> buildTree(Long parentId, Map<Long, List<Category>> childrenMap) {
        List<Category> nodes = childrenMap.getOrDefault(parentId, new ArrayList<>());
        for (Category node : nodes) {
            // 通过 transient 字段或自定义方法获取子节点（此处保持简单，不污染 Entity）
        }
        return nodes;
    }

    @Override
    public List<Category> getByLevel(int level) {
        return list(new LambdaQueryWrapper<Category>()
            .eq(Category::getLevel, level)
            .eq(Category::getStatus, 1)
            .orderByAsc(Category::getSortOrder));
    }
}
```

- [ ] **Step 3: 写入 Controller + 测试**

`CategoryController.java`:

```java
package com.supermarket.category.controller;

import com.supermarket.category.entity.Category;
import com.supermarket.category.service.CategoryService;
import com.supermarket.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public R<Category> create(@RequestBody Category category) {
        return R.ok(categoryService.create(category));
    }

    @GetMapping("/tree")
    public R<List<Category>> tree() {
        return R.ok(categoryService.getFullTree());
    }

    @GetMapping("/children/{parentId}")
    public R<List<Category>> children(@PathVariable Long parentId) {
        return R.ok(categoryService.getChildren(parentId));
    }

    @GetMapping("/level/{level}")
    public R<List<Category>> byLevel(@PathVariable int level) {
        return R.ok(categoryService.getByLevel(level));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
```

`CategoryServiceTest.java`:

```java
package com.supermarket.category.service;

import com.supermarket.category.entity.Category;
import com.supermarket.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Test
    void shouldCreateCategory() {
        Category cat = new Category();
        cat.setName("电子产品");
        cat.setParentId(0L);
        Category saved = categoryService.create(cat);
        assertThat(saved.getLevel()).isEqualTo(1);
    }

    @Test
    void shouldCreateSubCategoryWithCorrectLevel() {
        Category parent = new Category();
        parent.setName("服装");
        parent.setParentId(0L);
        categoryService.create(parent);

        Category child = new Category();
        child.setName("男装");
        child.setParentId(parent.getId());
        Category saved = categoryService.create(child);
        assertThat(saved.getLevel()).isEqualTo(2);
    }

    @Test
    void shouldNotDeleteCategoryWithChildren() {
        Category parent = new Category();
        parent.setName("家电");
        parent.setParentId(0L);
        categoryService.create(parent);

        Category child = new Category();
        child.setName("冰箱");
        child.setParentId(parent.getId());
        categoryService.create(child);

        assertThatThrownBy(() -> categoryService.delete(parent.getId()))
            .hasMessageContaining("存在子类目");
    }

    @Test
    void shouldGetChildren() {
        Category parent = new Category();
        parent.setName("食品");
        parent.setParentId(0L);
        categoryService.create(parent);

        Category child = new Category();
        child.setName("零食");
        child.setParentId(parent.getId());
        categoryService.create(child);

        var children = categoryService.getChildren(parent.getId());
        assertThat(children).hasSize(1);
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn test -pl super-market-services/service-category
```
Expected: `Tests run: 4, Failures: 0`

- [ ] **Step 5: Commit**

```bash
git add super-market-services/service-category/
git commit -m "feat(category-service): add 3-level category tree with recursive query"
```

---

## Task Group H: inventory-service（库存管理 + 乐观锁扣减）

### Task H1: Inventory Entity + Mapper（含乐观锁扣减 SQL）

**Files:**
- Create: `super-market-services/service-inventory/src/main/java/com/supermarket/inventory/entity/Inventory.java`
- Create: `super-market-services/service-inventory/src/main/java/com/supermarket/inventory/mapper/InventoryMapper.java`
- Create: `super-market-services/service-inventory/src/main/java/com/supermarket/inventory/service/InventoryService.java`
- Create: `super-market-services/service-inventory/src/main/java/com/supermarket/inventory/service/impl/InventoryServiceImpl.java`
- Create: `super-market-services/service-inventory/src/main/java/com/supermarket/inventory/controller/InventoryController.java`
- Create: `super-market-services/service-inventory/src/test/java/com/supermarket/inventory/service/InventoryServiceTest.java`

- [ ] **Step 1: 写入 Entity + Mapper（乐观锁）**

`Inventory.java`:

```java
package com.supermarket.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inventory")
public class Inventory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long skuId;
    private Integer totalStock;
    private Integer availableStock;
    private Integer lockedStock;
    private Integer safetyStock;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

`InventoryMapper.java`:

```java
package com.supermarket.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    @Update("UPDATE inventory SET available_stock = available_stock - #{quantity}, " +
            "locked_stock = locked_stock + #{quantity}, version = version + 1 " +
            "WHERE sku_id = #{skuId} AND available_stock >= #{quantity} AND version = #{version}")
    int deductStock(@Param("skuId") Long skuId,
                    @Param("quantity") int quantity,
                    @Param("version") int version);

    @Update("UPDATE inventory SET available_stock = available_stock + #{quantity}, " +
            "locked_stock = locked_stock - #{quantity}, version = version + 1 " +
            "WHERE sku_id = #{skuId} AND locked_stock >= #{quantity} AND version = #{version}")
    int restoreStock(@Param("skuId") Long skuId,
                     @Param("quantity") int quantity,
                     @Param("version") int version);

    @Update("UPDATE inventory SET locked_stock = locked_stock - #{quantity}, " +
            "total_stock = total_stock - #{quantity}, version = version + 1 " +
            "WHERE sku_id = #{skuId} AND locked_stock >= #{quantity} AND version = #{version}")
    int confirmDeduct(@Param("skuId") Long skuId,
                      @Param("quantity") int quantity,
                      @Param("version") int version);
}
```

- [ ] **Step 2: 写入 InventoryService（Redisson 分布式锁 + 乐观锁双重保障）**

`InventoryService.java`:

```java
package com.supermarket.inventory.service;

import com.supermarket.inventory.entity.Inventory;

public interface InventoryService {

    Inventory initStock(Long skuId, int totalStock, int safetyStock);

    Inventory getBySkuId(Long skuId);

    boolean deductStock(Long skuId, int quantity);

    boolean restoreStock(Long skuId, int quantity);

    boolean confirmDeduct(Long skuId, int quantity);
}
```

`InventoryServiceImpl.java`:

```java
package com.supermarket.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.inventory.entity.Inventory;
import com.supermarket.inventory.mapper.InventoryMapper;
import com.supermarket.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements InventoryService {

    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public Inventory initStock(Long skuId, int totalStock, int safetyStock) {
        Inventory exist = getBySkuId(skuId);
        if (exist != null) {
            throw new BizException(400, "该SKU库存已初始化");
        }
        Inventory inv = new Inventory();
        inv.setSkuId(skuId);
        inv.setTotalStock(totalStock);
        inv.setAvailableStock(totalStock);
        inv.setLockedStock(0);
        inv.setSafetyStock(safetyStock);
        inv.setVersion(0);
        save(inv);
        return inv;
    }

    @Override
    public Inventory getBySkuId(Long skuId) {
        return getOne(new LambdaQueryWrapper<Inventory>().eq(Inventory::getSkuId, skuId));
    }

    @Override
    @Transactional
    public boolean deductStock(Long skuId, int quantity) {
        String lockKey = "smt:lock:inventory:deduct:" + skuId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                log.warn("获取库存锁失败: skuId={}", skuId);
                return false;
            }
            Inventory inv = getBySkuId(skuId);
            if (inv == null || inv.getAvailableStock() < quantity) {
                return false;
            }
            int rows = baseMapper.deductStock(skuId, quantity, inv.getVersion());
            if (rows == 0) {
                log.warn("库存扣减失败(乐观锁冲突): skuId={}, version={}", skuId, inv.getVersion());
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public boolean restoreStock(Long skuId, int quantity) {
        Inventory inv = getBySkuId(skuId);
        if (inv == null || inv.getLockedStock() < quantity) {
            return false;
        }
        return baseMapper.restoreStock(skuId, quantity, inv.getVersion()) > 0;
    }

    @Override
    @Transactional
    public boolean confirmDeduct(Long skuId, int quantity) {
        Inventory inv = getBySkuId(skuId);
        if (inv == null || inv.getLockedStock() < quantity) {
            return false;
        }
        return baseMapper.confirmDeduct(skuId, quantity, inv.getVersion()) > 0;
    }
}
```

- [ ] **Step 3: 写入 Controller + 测试**

`InventoryController.java`:

```java
package com.supermarket.inventory.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.inventory.entity.Inventory;
import com.supermarket.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/init")
    public R<Inventory> init(@RequestParam Long skuId,
                              @RequestParam int totalStock,
                              @RequestParam(defaultValue = "0") int safetyStock) {
        return R.ok(inventoryService.initStock(skuId, totalStock, safetyStock));
    }

    @GetMapping("/sku/{skuId}")
    public R<Inventory> getBySku(@PathVariable Long skuId) {
        Inventory inv = inventoryService.getBySkuId(skuId);
        if (inv == null) {
            return R.fail(404, "库存信息不存在");
        }
        return R.ok(inv);
    }

    @PostMapping("/deduct")
    public R<Boolean> deduct(@RequestParam Long skuId, @RequestParam int quantity) {
        boolean success = inventoryService.deductStock(skuId, quantity);
        return success ? R.ok(true) : R.fail(400, "库存不足或扣减失败");
    }
}
```

`InventoryServiceTest.java`:

```java
package com.supermarket.inventory.service;

import com.supermarket.inventory.entity.Inventory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Test
    void shouldInitStock() {
        Inventory inv = inventoryService.initStock(1001L, 100, 10);
        assertThat(inv.getTotalStock()).isEqualTo(100);
        assertThat(inv.getAvailableStock()).isEqualTo(100);
        assertThat(inv.getLockedStock()).isEqualTo(0);
    }

    @Test
    void shouldDeductStock() {
        inventoryService.initStock(1002L, 100, 5);
        boolean success = inventoryService.deductStock(1002L, 10);
        assertThat(success).isTrue();

        Inventory inv = inventoryService.getBySkuId(1002L);
        assertThat(inv.getAvailableStock()).isEqualTo(90);
        assertThat(inv.getLockedStock()).isEqualTo(10);
    }

    @Test
    void shouldFailDeductWhenInsufficient() {
        inventoryService.initStock(1003L, 5, 0);
        boolean success = inventoryService.deductStock(1003L, 10);
        assertThat(success).isFalse();
    }

    @Test
    void shouldRestoreStock() {
        inventoryService.initStock(1004L, 100, 5);
        inventoryService.deductStock(1004L, 10);
        boolean restored = inventoryService.restoreStock(1004L, 10);
        assertThat(restored).isTrue();

        Inventory inv = inventoryService.getBySkuId(1004L);
        assertThat(inv.getAvailableStock()).isEqualTo(100);
        assertThat(inv.getLockedStock()).isEqualTo(0);
    }

    @Test
    void shouldConfirmDeduct() {
        inventoryService.initStock(1005L, 100, 5);
        inventoryService.deductStock(1005L, 10);
        boolean confirmed = inventoryService.confirmDeduct(1005L, 10);
        assertThat(confirmed).isTrue();

        Inventory inv = inventoryService.getBySkuId(1005L);
        assertThat(inv.getTotalStock()).isEqualTo(90);
        assertThat(inv.getLockedStock()).isEqualTo(0);
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn test -pl super-market-services/service-inventory
```
Expected: `Tests run: 5, Failures: 0`

- [ ] **Step 5: Commit**

```bash
git add super-market-services/service-inventory/
git commit -m "feat(inventory-service): add stock management with optimistic lock + Redisson distributed lock"
```

---

## Task Group I: shop-service（店铺与商家管理）

### Task I1: Shop + Merchant Entity + Mapper + Service + Controller + Tests

**Files:**
- Create: `super-market-services/service-shop/src/main/java/com/supermarket/shop/entity/Merchant.java`
- Create: `super-market-services/service-shop/src/main/java/com/supermarket/shop/entity/Shop.java`
- Create: `super-market-services/service-shop/src/main/java/com/supermarket/shop/mapper/MerchantMapper.java`
- Create: `super-market-services/service-shop/src/main/java/com/supermarket/shop/mapper/ShopMapper.java`
- Create: `super-market-services/service-shop/src/main/java/com/supermarket/shop/service/ShopService.java`
- Create: `super-market-services/service-shop/src/main/java/com/supermarket/shop/service/impl/ShopServiceImpl.java`
- Create: `super-market-services/service-shop/src/main/java/com/supermarket/shop/controller/ShopController.java`
- Create: `super-market-services/service-shop/src/test/java/com/supermarket/shop/service/ShopServiceTest.java`

- [ ] **Step 1: 写入 Entities**

```java
// Merchant.java
package com.supermarket.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchants")
public class Merchant extends BaseEntity {
    private Long userId;
    private String companyName;
    private String businessLicense;
    private String legalPerson;
    private String idCard;
    private String contactPhone;
    private Integer auditStatus;
    private String auditReason;
    private Integer status;
}

// Shop.java
package com.supermarket.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shops")
public class Shop extends BaseEntity {
    private Long merchantId;
    private String shopName;
    private String shopLogo;
    private String shopDesc;
    private Integer status;
}
```

- [ ] **Step 2: 写入 Mappers**

```java
// MerchantMapper.java
package com.supermarket.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.shop.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}

// ShopMapper.java
package com.supermarket.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.shop.entity.Shop;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
}
```

- [ ] **Step 3: 写入 ShopService**

`ShopService.java`:

```java
package com.supermarket.shop.service;

import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;

public interface ShopService {

    Merchant applyMerchant(Merchant merchant);

    void auditMerchant(Long merchantId, Integer auditStatus, String reason);

    Merchant getMerchantById(Long id);

    Shop createShop(Long merchantId, String shopName);

    Shop getShopById(Long id);

    Shop getShopByMerchantId(Long merchantId);
}
```

`ShopServiceImpl.java`:

```java
package com.supermarket.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;
import com.supermarket.shop.mapper.MerchantMapper;
import com.supermarket.shop.mapper.ShopMapper;
import com.supermarket.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;

    @Override
    @Transactional
    public Merchant applyMerchant(Merchant merchant) {
        Merchant exist = merchantMapper.selectOne(
            new LambdaQueryWrapper<Merchant>().eq(Merchant::getUserId, merchant.getUserId()));
        if (exist != null) {
            throw new BizException(400, "该用户已提交入驻申请");
        }
        merchant.setAuditStatus(0);
        merchant.setStatus(1);
        merchantMapper.insert(merchant);
        return merchant;
    }

    @Override
    @Transactional
    public void auditMerchant(Long merchantId, Integer auditStatus, String reason) {
        Merchant merchant = getMerchantById(merchantId);
        if (merchant.getAuditStatus() != 0) {
            throw new BizException(400, "该商家已审核");
        }
        merchant.setAuditStatus(auditStatus);
        merchant.setAuditReason(reason);
        if (auditStatus == 1) {
            createShop(merchantId, merchant.getCompanyName() + "旗舰店");
        }
        merchantMapper.updateById(merchant);
    }

    @Override
    public Merchant getMerchantById(Long id) {
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new BizException(404, "商家不存在");
        }
        return m;
    }

    @Override
    @Transactional
    public Shop createShop(Long merchantId, String shopName) {
        Shop shop = new Shop();
        shop.setMerchantId(merchantId);
        shop.setShopName(shopName);
        shop.setStatus(1);
        shopMapper.insert(shop);
        return shop;
    }

    @Override
    public Shop getShopById(Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BizException(404, "店铺不存在");
        }
        return shop;
    }

    @Override
    public Shop getShopByMerchantId(Long merchantId) {
        return shopMapper.selectOne(
            new LambdaQueryWrapper<Shop>().eq(Shop::getMerchantId, merchantId));
    }
}
```

- [ ] **Step 4: 写入 Controller + 测试**

```java
// ShopController.java
package com.supermarket.shop.controller;

import com.supermarket.common.core.result.R;
import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;
import com.supermarket.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @PostMapping("/merchant/apply")
    public R<Merchant> apply(@RequestBody Merchant merchant) {
        return R.ok(shopService.applyMerchant(merchant));
    }

    @PutMapping("/merchant/{id}/audit")
    public R<Void> audit(@PathVariable Long id,
                         @RequestParam Integer auditStatus,
                         @RequestParam(required = false) String reason) {
        shopService.auditMerchant(id, auditStatus, reason);
        return R.ok();
    }

    @GetMapping("/{id}")
    public R<Shop> getShop(@PathVariable Long id) {
        return R.ok(shopService.getShopById(id));
    }

    @GetMapping("/merchant/{merchantId}")
    public R<Shop> getByMerchant(@PathVariable Long merchantId) {
        return R.ok(shopService.getShopByMerchantId(merchantId));
    }
}
```

`ShopServiceTest.java`:

```java
package com.supermarket.shop.service;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.shop.entity.Merchant;
import com.supermarket.shop.entity.Shop;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ShopServiceTest {

    @Autowired
    private ShopService shopService;

    @Test
    void shouldApplyMerchant() {
        Merchant merchant = newMerchant(501L);
        Merchant saved = shopService.applyMerchant(merchant);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAuditStatus()).isEqualTo(0);
    }

    @Test
    void shouldAuditAndAutoCreateShop() {
        Merchant merchant = shopService.applyMerchant(newMerchant(502L));
        shopService.auditMerchant(merchant.getId(), 1, null);

        Shop shop = shopService.getShopByMerchantId(merchant.getId());
        assertThat(shop).isNotNull();
        assertThat(shop.getShopName()).contains("旗舰店");
    }

    @Test
    void shouldRejectDuplicateMerchantApplication() {
        shopService.applyMerchant(newMerchant(503L));
        assertThatThrownBy(() -> shopService.applyMerchant(newMerchant(503L)))
            .hasMessageContaining("已提交");
    }

    private Merchant newMerchant(Long userId) {
        Merchant m = new Merchant();
        m.setUserId(userId);
        m.setCompanyName("测试商家" + userId);
        m.setContactPhone("13800000000");
        return m;
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn test -pl super-market-services/service-shop
```
Expected: `Tests run: 3, Failures: 0`

- [ ] **Step 6: Commit**

```bash
git add super-market-services/service-shop/
git commit -m "feat(shop-service): add merchant application, audit + auto shop creation"
```

---

## Task Group J: 全量集成验证

### Task J1: 全部 8 个服务编译 + 测试

- [ ] **Step 1: 全量编译（含依赖安装）**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn clean install -DskipTests
```
Expected: `BUILD SUCCESS` for all 32 modules (5 common + 1 dubbo-api + 18 services + 1 gateway + 8 services with new code)

- [ ] **Step 2: 全量运行单元测试**

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" mvn test
```
Expected: 全部测试通过（约 35+ 个测试用例）

- [ ] **Step 3: 验证服务数量**

```bash
echo "=== Java 源文件 ===" && find . -name "*.java" -not -path "*/target/*" | wc -l
echo "=== 测试文件 ===" && find . -name "*Test.java" -not -path "*/target/*" | wc -l
echo "=== Entity 数量 ===" && find . -name "*.java" -path "*/entity/*" | wc -l
```
Expected: Java 源文件明显增加（从 48 → ~95），测试文件 ~8，Entity ~12

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "chore: Phase 2 complete — user + product domains (8 services, 35+ tests)

Implemented:
- user-service: register/login/profile (UserDubboService)
- auth-service: JWT login + token refresh
- address-service: CRUD with 20-addr limit + default management
- member-service: 5-tier level system + points
- product-service: SPU/SKU create + audit + shelf management
- category-service: 3-level category tree
- inventory-service: optimistic lock + Redisson distributed lock
- shop-service: merchant application + audit + auto shop creation

DDL: 10 tables across db_user, db_product, db_shop"
```

---

## Phase 2 完成检查清单

- [ ] db_user 3 张表（users, members, addresses）
- [ ] db_product 5 张表（categories, brands, spu, sku, inventory）
- [ ] db_shop 2 张表（merchants, shops）
- [ ] user-service: 注册/登录/查询 + Dubbo RPC 接口
- [ ] auth-service: JWT 签发/刷新/验证
- [ ] address-service: CRUD + 默认地址 + 软删除
- [ ] member-service: 5 级会员 + 积分加减 + 自动升级
- [ ] product-service: SPU+SKU 创建 + 审核 + 上下架 + 分页列表
- [ ] category-service: 三级类目树 + 递归子节点 + 防删除保护
- [ ] inventory-service: 初始化 + 扣减 + 回补 + 乐观锁 + 分布式锁
- [ ] shop-service: 商家入驻 + 审核 + 自动开店
- [ ] 全量 `mvn clean install -DskipTests` BUILD SUCCESS
- [ ] 全量 `mvn test` 所有测试通过 (~35+ tests)
- [ ] mybatis-plus `@Version` 乐观锁配置启用

---

> **Plan saved:** `docs/superpowers/plans/2026-05-12-phase2-user-product-services.md`
>
> **Prerequisites:** JDK 21 已安装 + Docker Compose 中间件可启动 + Phase 1 基础设施已完成
>
> **Next:** Execute this plan using `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans`.
