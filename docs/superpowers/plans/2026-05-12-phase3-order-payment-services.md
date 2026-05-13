# Phase 3 — 交易域 + 支付域 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现购物车服务（Redis Hash）、订单服务（Seata AT 分布式事务 + 状态流转）、支付服务（幂等 + 回调 + 退款），集成 RocketMQ 订单事件消息 + 延迟消息（30min 超时取消），以及 Seata AT 模式基础设施。

**Architecture:** 购物车基于 Redis Hash 存储。订单服务作为 Seata TM 发起全局事务，协调库存服务（RM）扣减库存。支付服务模拟第三方支付回调，实现幂等性保证。RocketMQ 用于订单事件异步通知和延迟消息取消超时订单。

**Tech Stack:** Java 21, Spring Boot 3.2, Apache Dubbo 3.2, MyBatis-Plus 3.5, Redis 7.2 + Redisson, RocketMQ 5.1, Seata 1.7 (AT mode), MySQL 8.0, JUnit 5 + Mockito

**简化策略：** 本阶段 Seata AT 事务仅覆盖 order-service + inventory-service 两个参与者。优惠券/积分核销留 Phase 4 扩展。支付采用 mock 模式，不对接真实支付宝/微信。

---

## Phase 3 验收标准

- [ ] 所有 3 个服务 `mvn clean install -DskipTests` BUILD SUCCESS
- [ ] 所有单元测试 `mvn test` 通过，覆盖率 > 80%
- [ ] 购物车添加/修改/选中 API 正常，数据存储在 Redis
- [ ] 下单 API 创建订单 + 扣减库存（Seata AT 事务），库存不足时回滚
- [ ] 订单状态流转：待付款→(支付)→待发货→(发货)→待收货→(确认)→已完成
- [ ] 支付回调幂等：重复回调不会重复处理
- [ ] RocketMQ 延迟消息：下单 30min 后未支付自动取消
- [ ] Seata Server 启动正常，undo_log 表正常工作

---

## Task Group A: 数据库 DDL

### Task A1: 创建 db_order + db_payment 表 + Seata undo_log

**Files:**
- Create: `docker-compose/mysql/init/05-order-tables.sql`
- Create: `docker-compose/mysql/init/06-payment-tables.sql`

- [ ] **Step 1: 写入订单域 DDL**

写入 `docker-compose/mysql/init/05-order-tables.sql`:

```sql
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

-- Seata AT 模式 undo_log 表（每个业务库都需要）
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
```

- [ ] **Step 2: 写入支付域 DDL**

写入 `docker-compose/mysql/init/06-payment-tables.sql`:

```sql
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
```

- [ ] **Step 3: 执行 SQL 验证**

```bash
docker exec smt-mysql mysql -u root -proot123 -e "USE db_order; SHOW TABLES;" && docker exec smt-mysql mysql -u root -proot123 -e "USE db_payment; SHOW TABLES;"
```
Expected: db_order 输出 `orders`, `order_items`, `undo_log`；db_payment 输出 `payments`, `payment_refunds`, `payment_idempotent`, `undo_log`

- [ ] **Step 4: 为 db_product 也添加 undo_log（库存服务参与 Seata）**

```bash
docker exec smt-mysql mysql -u root -proot123 db_product -e "
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
"
```

- [ ] **Step 5: Commit**

```bash
git add docker-compose/mysql/init/05-order-tables.sql docker-compose/mysql/init/06-payment-tables.sql
git commit -m "feat: add order + payment domain DDL (orders, payments, refunds, undo_log)"
```

---

## Task Group B: Seata 基础设施

### Task B1: Docker Compose 添加 Seata Server + 配置

**Files:**
- Modify: `docker-compose/docker-compose.yml` — 添加 seata-server 服务
- Create: `docker-compose/seata/application.yml` — Seata Server 配置

- [ ] **Step 1: 在 docker-compose.yml 添加 Seata Server**

在 `docker-compose/docker-compose.yml` 的 `services:` 下添加:

```yaml
  seata-server:
    image: seataio/seata-server:1.7.1
    container_name: smt-seata
    restart: unless-stopped
    environment:
      SEATA_PORT: 8091
      STORE_MODE: db
    ports:
      - "${SEATA_PORT:-19091}:8091"
      - "${SEATA_CONSOLE_PORT:-18092}:7091"
    volumes:
      - ./seata/application.yml:/seata-server/resources/application.yml
    networks:
      - smt-net
    depends_on:
      mysql:
        condition: service_healthy
```

- [ ] **Step 2: 创建 Seata Server 配置**

写入 `docker-compose/seata/application.yml`:

```yaml
server:
  port: 7091

spring:
  application:
    name: seata-server

logging:
  level:
    io.seata: INFO

seata:
  config:
    type: file
  registry:
    type: file
  store:
    mode: db
    db:
      datasource: druid
      db-type: mysql
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://mysql:3306/seata?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
      user: root
      password: root123
      min-conn: 5
      max-conn: 100
      global-table: global_table
      branch-table: branch_table
      lock-table: lock_table
      distributed-lock-table: distributed_lock
      query-limit: 100
      max-wait: 5000
  server:
    service-port: 8091
```

- [ ] **Step 3: 添加 seata 库初始化 SQL 到 01-init-databases.sql**

在 `docker-compose/mysql/init/01-init-databases.sql` 末尾添加:

```sql
CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

- [ ] **Step 4: Commit**

```bash
git add docker-compose/docker-compose.yml docker-compose/seata/ docker-compose/mysql/init/01-init-databases.sql
git commit -m "feat: add Seata Server 1.7 to docker-compose + seata database"
```

---

### Task B2: 服务端添加 Seata 依赖与配置

- [ ] **Step 1: 父 POM 添加 Seata 版本管理**

在根 `pom.xml` 的 `<properties>` 中添加:

```xml
<seata.version>1.7.1</seata.version>
```

在 `<dependencyManagement>` 中添加:

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
    <version>${seata.version}</version>
</dependency>
```

- [ ] **Step 2: order-service pom.xml 添加 Seata 依赖**

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
</dependency>
```

- [ ] **Step 3: order-service application-dev.yml 添加 Seata 配置**

```yaml
seata:
  enabled: true
  application-id: order-service
  tx-service-group: super-market-tx-group
  service:
    vgroup-mapping:
      super-market-tx-group: default
    grouplist:
      default: localhost:19091
  data-source-proxy-mode: AT
```

- [ ] **Step 4: inventory-service 同样添加 Seata 依赖和配置**

在 `service-inventory/pom.xml` 添加 seata 依赖，`application-dev.yml` 添加:
```yaml
seata:
  enabled: true
  application-id: inventory-service
  tx-service-group: super-market-tx-group
  service:
    vgroup-mapping:
      super-market-tx-group: default
    grouplist:
      default: localhost:19091
  data-source-proxy-mode: AT
```

- [ ] **Step 5: Commit**

```bash
git add pom.xml super-market-services/service-order/pom.xml super-market-services/service-inventory/pom.xml super-market-services/service-order/src/main/resources/ super-market-services/service-inventory/src/main/resources/
git commit -m "feat: add Seata AT mode config to order-service + inventory-service"
```

---

## Task Group C: Common Dubbo API 扩展

### Task C1: 创建订单域的 Dubbo 接口

**Files:**
- Create: `super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/order/OrderDubboService.java`
- Create: `super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/order/dto/CreateOrderRequest.java`
- Create: `super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/order/dto/OrderDTO.java`
- Create: `super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/payment/PaymentDubboService.java`
- Create: `super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/payment/dto/PaymentDTO.java`

- [ ] **Step 1: 写入 CreateOrderRequest DTO**

```java
package com.supermarket.common.dubbo.api.order.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest implements Serializable {
    private Long userId;
    private Long addressId;
    private String addressSnapshot;
    private List<OrderItemRequest> items;
    private String remark;

    @Data
    public static class OrderItemRequest implements Serializable {
        private Long skuId;
        private String skuName;
        private String skuImage;
        private BigDecimal skuPrice;
        private Integer quantity;
    }
}
```

- [ ] **Step 2: 写入 OrderDTO**

```java
package com.supermarket.common.dubbo.api.order.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDTO implements Serializable {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long shopId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private BigDecimal actualAmount;
    private Integer orderStatus;
    private String addressSnapshot;
    private LocalDateTime expireTime;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 写入 OrderDubboService 接口**

```java
package com.supermarket.common.dubbo.api.order;

import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.common.dubbo.api.order.dto.OrderDTO;

public interface OrderDubboService {
    OrderDTO createOrder(CreateOrderRequest request);
    OrderDTO getByOrderNo(String orderNo);
    void updateStatus(String orderNo, Integer toStatus);
}
```

- [ ] **Step 4: 写入 PaymentDTO 和 PaymentDubboService**

`PaymentDTO.java`:
```java
package com.supermarket.common.dubbo.api.payment.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDTO implements Serializable {
    private String payNo;
    private String orderNo;
    private BigDecimal amount;
    private Integer payMethod;
    private Integer payStatus;
    private String thirdPayNo;
    private LocalDateTime paidAt;
}
```

`PaymentDubboService.java`:
```java
package com.supermarket.common.dubbo.api.payment;

import com.supermarket.common.dubbo.api.payment.dto.PaymentDTO;

public interface PaymentDubboService {
    PaymentDTO createPayment(String orderNo, Long userId, BigDecimal amount, Integer payMethod);
    PaymentDTO getByPayNo(String payNo);
    void handleCallback(String payNo, String thirdPayNo);
}
```

- [ ] **Step 5: Commit**

```bash
git add super-market-common/common-dubbo-api/
git commit -m "feat: add OrderDubboService + PaymentDubboService API definitions"
```

---

## Task Group D: cart-service（购物车服务 — Redis Hash）

### Task D1: CartService + CartController + Tests

**Files:**
- Create: `super-market-services/service-cart/src/main/java/com/supermarket/cart/service/CartService.java`
- Create: `super-market-services/service-cart/src/main/java/com/supermarket/cart/service/impl/CartServiceImpl.java`
- Create: `super-market-services/service-cart/src/main/java/com/supermarket/cart/dto/CartItemDTO.java`
- Create: `super-market-services/service-cart/src/main/java/com/supermarket/cart/controller/CartController.java`
- Create: `super-market-services/service-cart/src/test/java/com/supermarket/cart/service/CartServiceTest.java`
- Create: `super-market-services/service-cart/src/test/resources/application-test.yml`

- [ ] **Step 1: 写入 CartItemDTO**

```java
package com.supermarket.cart.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CartItemDTO implements Serializable {
    private Long skuId;
    private Long spuId;
    private String spuName;
    private String skuSpec;
    private String skuImage;
    private BigDecimal price;
    private Integer quantity;
    private Boolean selected;
}
```

- [ ] **Step 2: 写入 CartService 接口**

```java
package com.supermarket.cart.service;

import com.supermarket.cart.dto.CartItemDTO;
import java.util.List;

public interface CartService {
    void addItem(Long userId, CartItemDTO item);
    void updateQuantity(Long userId, Long skuId, Integer quantity);
    void removeItem(Long userId, Long skuId);
    void selectItem(Long userId, Long skuId, Boolean selected);
    void selectAll(Long userId, Boolean selected);
    List<CartItemDTO> getCartList(Long userId);
    List<CartItemDTO> getSelectedItems(Long userId);
    void clearSelected(Long userId);
    Integer getCartCount(Long userId);
}
```

- [ ] **Step 3: 写入 CartServiceImpl（Redis Hash 实现）**

Redis Key 设计:
- `smt:cart:{userId}` → Hash, field=skuId, value=CartItemDTO JSON
- `smt:cart:version:{userId}` → String, 乐观锁版本号

```java
package com.supermarket.cart.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.supermarket.cart.dto.CartItemDTO;
import com.supermarket.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final RedissonClient redissonClient;

    private static final String CART_PREFIX = "smt:cart:";

    private RMap<String, String> getCartMap(Long userId) {
        return redissonClient.getMap(CART_PREFIX + userId);
    }

    @Override
    public void addItem(Long userId, CartItemDTO item) {
        RMap<String, String> cart = getCartMap(userId);
        String key = String.valueOf(item.getSkuId());
        String existing = cart.get(key);
        if (existing != null) {
            CartItemDTO existItem = JSONUtil.toBean(existing, CartItemDTO.class);
            existItem.setQuantity(existItem.getQuantity() + item.getQuantity());
            cart.put(key, JSONUtil.toJsonStr(existItem));
        } else {
            item.setSelected(true);
            cart.put(key, JSONUtil.toJsonStr(item));
        }
    }

    @Override
    public void updateQuantity(Long userId, Long skuId, Integer quantity) {
        RMap<String, String> cart = getCartMap(userId);
        String key = String.valueOf(skuId);
        String existing = cart.get(key);
        if (existing != null) {
            CartItemDTO item = JSONUtil.toBean(existing, CartItemDTO.class);
            item.setQuantity(quantity);
            cart.put(key, JSONUtil.toJsonStr(item));
        }
    }

    @Override
    public void removeItem(Long userId, Long skuId) {
        cart.remove(String.valueOf(skuId));
    }

    @Override
    public void selectItem(Long userId, Long skuId, Boolean selected) {
        String key = String.valueOf(skuId);
        String existing = cart.get(key);
        if (existing != null) {
            CartItemDTO item = JSONUtil.toBean(existing, CartItemDTO.class);
            item.setSelected(selected);
            cart.put(key, JSONUtil.toJsonStr(item));
        }
    }

    @Override
    public void selectAll(Long userId, Boolean selected) {
        for (var entry : cart.entrySet()) {
            CartItemDTO item = JSONUtil.toBean(entry.getValue(), CartItemDTO.class);
            item.setSelected(selected);
            cart.put(entry.getKey(), JSONUtil.toJsonStr(item));
        }
    }

    @Override
    public List<CartItemDTO> getCartList(Long userId) {
        return cart.values().stream()
            .map(v -> JSONUtil.toBean(v, CartItemDTO.class))
            .collect(Collectors.toList());
    }

    @Override
    public List<CartItemDTO> getSelectedItems(Long userId) {
        return getCartList(userId).stream()
            .filter(CartItemDTO::getSelected)
            .collect(Collectors.toList());
    }

    @Override
    public void clearSelected(Long userId) {
        List<String> toRemove = cart.entrySet().stream()
            .filter(e -> {
                CartItemDTO item = JSONUtil.toBean(e.getValue(), CartItemDTO.class);
                return item.getSelected();
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        cart.removeAll(toRemove);
    }

    @Override
    public Integer getCartCount(Long userId) {
        return cart.size();
    }
}
```

- [ ] **Step 4: 写入 CartController**

```java
package com.supermarket.cart.controller;

import com.supermarket.cart.dto.CartItemDTO;
import com.supermarket.cart.service.CartService;
import com.supermarket.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public R<Void> add(@RequestParam Long userId, @RequestBody CartItemDTO item) {
        cartService.addItem(userId, item);
        return R.ok();
    }

    @PutMapping("/item/{skuId}")
    public R<Void> updateQuantity(@RequestParam Long userId,
                                   @PathVariable Long skuId,
                                   @RequestParam Integer quantity) {
        cartService.updateQuantity(userId, skuId, quantity);
        return R.ok();
    }

    @DeleteMapping("/item/{skuId}")
    public R<Void> remove(@RequestParam Long userId, @PathVariable Long skuId) {
        cartService.removeItem(userId, skuId);
        return R.ok();
    }

    @PutMapping("/item/{skuId}/select")
    public R<Void> select(@RequestParam Long userId,
                          @PathVariable Long skuId,
                          @RequestParam Boolean selected) {
        cartService.selectItem(userId, skuId, selected);
        return R.ok();
    }

    @PutMapping("/select-all")
    public R<Void> selectAll(@RequestParam Long userId, @RequestParam Boolean selected) {
        cartService.selectAll(userId, selected);
        return R.ok();
    }

    @GetMapping("/list")
    public R<List<CartItemDTO>> list(@RequestParam Long userId) {
        return R.ok(cartService.getCartList(userId));
    }

    @GetMapping("/count")
    public R<Integer> count(@RequestParam Long userId) {
        return R.ok(cartService.getCartCount(userId));
    }
}
```

- [ ] **Step 5: 写入单元测试**

`CartServiceTest.java`:
```java
package com.supermarket.cart.service;

import com.supermarket.cart.dto.CartItemDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Test
    void shouldAddItem() {
        cartService.addItem(1L, newItem(1001L, 2));
        List<CartItemDTO> list = cartService.getCartList(1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void shouldUpdateQuantity() {
        cartService.addItem(2L, newItem(1001L, 1));
        cartService.updateQuantity(2L, 1001L, 5);
        List<CartItemDTO> list = cartService.getCartList(2L);
        assertThat(list.get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRemoveItem() {
        cartService.addItem(3L, newItem(1001L, 1));
        cartService.removeItem(3L, 1001L);
        assertThat(cartService.getCartList(3L)).isEmpty();
    }

    @Test
    void shouldGetSelectedItems() {
        cartService.addItem(4L, newItem(1001L, 1));
        cartService.addItem(4L, newItem(1002L, 1));
        cartService.selectItem(4L, 1001L, false);
        List<CartItemDTO> selected = cartService.getSelectedItems(4L);
        assertThat(selected).hasSize(1);
    }

    @Test
    void shouldClearSelected() {
        cartService.addItem(5L, newItem(1001L, 1));
        cartService.addItem(5L, newItem(1002L, 1));
        cartService.selectItem(5L, 1002L, false);
        cartService.clearSelected(5L);
        assertThat(cartService.getCartList(5L)).hasSize(1);
    }

    private CartItemDTO newItem(Long skuId, int qty) {
        CartItemDTO item = new CartItemDTO();
        item.setSkuId(skuId);
        item.setSpuId(skuId + 10000);
        item.setSpuName("测试商品");
        item.setSkuSpec("默认规格");
        item.setPrice(new BigDecimal("99.00"));
        item.setQuantity(qty);
        return item;
    }
}
```

- [ ] **Step 6: 创建测试 profile**

`application-test.yml`:
```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false
  autoconfigure:
    exclude:
      - org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration
      - com.alibaba.cloud.nacos.NacosDiscoveryAutoConfiguration
      - com.alibaba.cloud.nacos.NacosConfigAutoConfiguration
  data:
    redis:
      host: localhost
      port: 16379
      password: redis123

dubbo:
  registry:
    address: N/A
  config-center:
    address: N/A
```

- [ ] **Step 7: 编译 + 测试验证**

```bash
mvn clean test -pl super-market-services/service-cart
```
Expected: `Tests run: 5, Failures: 0`

- [ ] **Step 8: Commit**

```bash
git add super-market-services/service-cart/
git commit -m "feat(cart-service): Redis Hash shopping cart with 5 tests"
```

---

## Task Group E: order-service（订单服务 + Seata AT）

### Task E1: Order Entity + Mapper

**Files:**
- Create: `super-market-services/service-order/src/main/java/com/supermarket/order/entity/Order.java`
- Create: `super-market-services/service-order/src/main/java/com/supermarket/order/entity/OrderItem.java`
- Create: `super-market-services/service-order/src/main/java/com/supermarket/order/mapper/OrderMapper.java`
- Create: `super-market-services/service-order/src/main/java/com/supermarket/order/mapper/OrderItemMapper.java`

- [ ] **Step 1: 写入 Order Entity**

`Order.java`:
```java
package com.supermarket.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Order extends BaseEntity {
    private String orderNo;
    private Long userId;
    private Long shopId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private BigDecimal actualAmount;
    private Integer payMethod;
    private String payNo;
    private Integer orderStatus;
    private String addressSnapshot;
    private LocalDateTime expireTime;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
}
```

- [ ] **Step 2: 写入 OrderItem Entity + Mappers**

`OrderItem.java`:
```java
package com.supermarket.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("order_items")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long skuId;
    private String skuName;
    private String skuImage;
    private BigDecimal skuPrice;
    private Integer quantity;
    private BigDecimal totalPrice;
}
```

`OrderMapper.java`:
```java
package com.supermarket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
```

`OrderItemMapper.java`:
```java
package com.supermarket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT * FROM order_items WHERE order_no = #{orderNo}")
    List<OrderItem> selectByOrderNo(@Param("orderNo") String orderNo);
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn clean compile -pl super-market-services/service-order -DskipTests
```

- [ ] **Step 4: Commit**

```bash
git add super-market-services/service-order/
git commit -m "feat(order-service): add Order/OrderItem entities and mappers"
```

---

### Task E2: OrderService + Seata 分布式事务

**Files:**
- Create: `super-market-services/service-order/src/main/java/com/supermarket/order/service/OrderService.java`
- Create: `super-market-services/service-order/src/main/java/com/supermarket/order/service/impl/OrderServiceImpl.java`
- Create: `super-market-services/service-order/src/main/java/com/supermarket/order/controller/OrderController.java`
- Create: `super-market-services/service-order/src/test/java/com/supermarket/order/service/OrderServiceTest.java`
- Create: `super-market-services/service-order/src/test/resources/application-test.yml`

- [ ] **Step 1: 写入 OrderService 接口**

```java
package com.supermarket.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;

import java.util.List;

public interface OrderService {
    Order createOrder(CreateOrderRequest request);
    Order getByOrderNo(String orderNo);
    Order getById(Long orderId);
    Page<Order> listByUser(Long userId, Integer status, int page, int size);
    Page<Order> listByShop(Long shopId, Integer status, int page, int size);
    void cancelOrder(String orderNo, String reason);
    void paySuccess(String orderNo, String payNo);
    void ship(String orderNo);
    void confirmReceive(String orderNo);
    List<OrderItem> getOrderItems(String orderNo);
}
```

- [ ] **Step 2: 写入 OrderServiceImpl（核心 — Seata AT 事务）**

```java
package com.supermarket.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.dubbo.api.inventory.InventoryDubboService;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;
import com.supermarket.order.mapper.OrderItemMapper;
import com.supermarket.order.mapper.OrderMapper;
import com.supermarket.order.service.OrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderItemMapper orderItemMapper;

    @DubboReference(check = false)
    private InventoryDubboService inventoryDubboService;

    @Override
    @GlobalTransactional(timeoutMills = 30000)
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 1. 生成订单号
        String orderNo = "ORD" + IdUtil.getSnowflakeNextId();

        // 2. 计算金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var item : request.getItems()) {
            BigDecimal itemTotal = item.getSkuPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        // 3. 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(request.getUserId());
        order.setShopId(1L); // 简化：单店铺，后续从SKU反查
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setActualAmount(totalAmount);
        order.setOrderStatus(1); // 待付款
        order.setAddressSnapshot(request.getAddressSnapshot());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        save(order);

        // 4. 创建订单明细
        for (var item : request.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrderNo(orderNo);
            oi.setSkuId(item.getSkuId());
            oi.setSkuName(item.getSkuName());
            oi.setSkuImage(item.getSkuImage());
            oi.setSkuPrice(item.getSkuPrice());
            oi.setQuantity(item.getQuantity());
            oi.setTotalPrice(item.getSkuPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(oi);
        }

        // 5. Seata AT: 调用库存服务扣减（RM参与者）
        for (var item : request.getItems()) {
            boolean deducted = inventoryDubboService.deduct(item.getSkuId(), item.getQuantity());
            if (!deducted) {
                throw new BizException(400, "库存不足: skuId=" + item.getSkuId());
            }
        }

        log.info("订单创建成功: orderNo={}, amount={}", orderNo, totalAmount);
        return order;
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        Order order = getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) throw new BizException(404, "订单不存在");
        return order;
    }

    @Override
    public Order getById(Long orderId) {
        Order order = super.getById(orderId);
        if (order == null) throw new BizException(404, "订单不存在");
        return order;
    }

    @Override
    public Page<Order> listByUser(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
            .eq(Order::getUserId, userId)
            .orderByDesc(Order::getCreatedAt);
        if (status != null) wrapper.eq(Order::getOrderStatus, status);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Order> listByShop(Long shopId, Integer status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
            .eq(Order::getShopId, shopId)
            .orderByDesc(Order::getCreatedAt);
        if (status != null) wrapper.eq(Order::getOrderStatus, status);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo, String reason) {
        Order order = getByOrderNo(orderNo);
        if (order.getOrderStatus() != 1) {
            throw new BizException(400, "仅待付款订单可取消");
        }
        order.setOrderStatus(5); // 已取消
        updateById(order);
        // 回补库存（非Seata，独立事务）
        rollbackInventory(orderNo);
    }

    @Override
    @Transactional
    public void paySuccess(String orderNo, String payNo) {
        Order order = getByOrderNo(orderNo);
        if (order.getOrderStatus() != 1) {
            throw new BizException(400, "订单状态不正确");
        }
        order.setOrderStatus(2); // 待发货
        order.setPayNo(payNo);
        order.setPaidAt(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void ship(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order.getOrderStatus() != 2) throw new BizException(400, "仅待发货订单可发货");
        order.setOrderStatus(3); // 待收货
        order.setShippedAt(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void confirmReceive(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order.getOrderStatus() != 3) throw new BizException(400, "仅待收货订单可确认");
        order.setOrderStatus(4); // 已完成
        order.setReceivedAt(LocalDateTime.now());
        updateById(order);
    }

    @Override
    public List<OrderItem> getOrderItems(String orderNo) {
        return orderItemMapper.selectByOrderNo(orderNo);
    }

    private void rollbackInventory(String orderNo) {
        List<OrderItem> items = getOrderItems(orderNo);
        for (OrderItem item : items) {
            try {
                inventoryDubboService.restore(item.getSkuId(), item.getQuantity());
            } catch (Exception e) {
                log.error("回补库存失败: skuId={}", item.getSkuId(), e);
            }
        }
    }
}
```

- [ ] **Step 3: 写入 OrderController**

```java
package com.supermarket.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supermarket.common.core.result.R;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.order.entity.Order;
import com.supermarket.order.entity.OrderItem;
import com.supermarket.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public R<Order> create(@RequestBody CreateOrderRequest request) {
        return R.ok(orderService.createOrder(request));
    }

    @GetMapping("/{orderNo}")
    public R<Order> detail(@PathVariable String orderNo) {
        Order order = orderService.getByOrderNo(orderNo);
        List<OrderItem> items = orderService.getOrderItems(orderNo);
        // 简单返回，生产环境用 VO 包装
        return R.ok(order);
    }

    @GetMapping("/list/user/{userId}")
    public R<Page<Order>> listByUser(@PathVariable Long userId,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return R.ok(orderService.listByUser(userId, status, page, size));
    }

    @PutMapping("/{orderNo}/cancel")
    public R<Void> cancel(@PathVariable String orderNo, @RequestParam String reason) {
        orderService.cancelOrder(orderNo, reason);
        return R.ok();
    }

    @PutMapping("/{orderNo}/ship")
    public R<Void> ship(@PathVariable String orderNo) {
        orderService.ship(orderNo);
        return R.ok();
    }

    @PutMapping("/{orderNo}/receive")
    public R<Void> receive(@PathVariable String orderNo) {
        orderService.confirmReceive(orderNo);
        return R.ok();
    }
}
```

- [ ] **Step 4: 写入单元测试**

`OrderServiceTest.java`:
```java
package com.supermarket.order.service;

import com.supermarket.common.dubbo.api.inventory.InventoryDubboService;
import com.supermarket.common.dubbo.api.order.dto.CreateOrderRequest;
import com.supermarket.order.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private InventoryDubboService inventoryDubboService;

    @Test
    void shouldCreateOrder() {
        when(inventoryDubboService.deduct(anyLong(), anyInt())).thenReturn(true);
        CreateOrderRequest req = buildRequest();
        Order order = orderService.createOrder(req);
        assertThat(order.getOrderNo()).startsWith("ORD");
        assertThat(order.getOrderStatus()).isEqualTo(1);
        assertThat(order.getExpireTime()).isNotNull();
    }

    @Test
    void shouldCancelOrder() {
        when(inventoryDubboService.deduct(anyLong(), anyInt())).thenReturn(true);
        Order order = orderService.createOrder(buildRequest());
        orderService.cancelOrder(order.getOrderNo(), "不想要了");
        Order cancelled = orderService.getByOrderNo(order.getOrderNo());
        assertThat(cancelled.getOrderStatus()).isEqualTo(5);
    }

    @Test
    void shouldPaySuccess() {
        when(inventoryDubboService.deduct(anyLong(), anyInt())).thenReturn(true);
        Order order = orderService.createOrder(buildRequest());
        orderService.paySuccess(order.getOrderNo(), "PAY2024001");
        Order paid = orderService.getByOrderNo(order.getOrderNo());
        assertThat(paid.getOrderStatus()).isEqualTo(2);
    }

    @Test
    void shouldFullStatusFlow() {
        when(inventoryDubboService.deduct(anyLong(), anyInt())).thenReturn(true);
        Order order = orderService.createOrder(buildRequest());
        String orderNo = order.getOrderNo();

        orderService.paySuccess(orderNo, "PAY001");
        assertThat(orderService.getByOrderNo(orderNo).getOrderStatus()).isEqualTo(2);

        orderService.ship(orderNo);
        assertThat(orderService.getByOrderNo(orderNo).getOrderStatus()).isEqualTo(3);

        orderService.confirmReceive(orderNo);
        assertThat(orderService.getByOrderNo(orderNo).getOrderStatus()).isEqualTo(4);
    }

    private CreateOrderRequest buildRequest() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setUserId(1L);
        req.setAddressSnapshot("{\"name\":\"张三\",\"phone\":\"13800000000\"}");
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(1001L);
        item.setSkuName("测试商品-红色-XL");
        item.setSkuPrice(new BigDecimal("99.00"));
        item.setQuantity(2);
        req.setItems(List.of(item));
        return req;
    }
}
```

- [ ] **Step 5: 创建测试 profile**

`application-test.yml`:
```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false
  autoconfigure:
    exclude:
      - org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration
      - com.alibaba.cloud.nacos.NacosDiscoveryAutoConfiguration
      - com.alibaba.cloud.nacos.NacosConfigAutoConfiguration
      - io.seata.spring.boot.autoconfigure.SeataAutoConfiguration
  datasource:
    url: jdbc:mysql://localhost:13306/db_order?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  global-config:
    db-config:
      id-type: assign_id

dubbo:
  registry:
    address: N/A
  config-center:
    address: N/A

seata:
  enabled: false
```

- [ ] **Step 6: 编译 + 测试验证**

```bash
mvn clean test -pl super-market-services/service-order
```
Expected: `Tests run: 4, Failures: 0`

- [ ] **Step 7: Commit**

```bash
git add super-market-services/service-order/
git commit -m "feat(order-service): order create/cancel/status-flow with Seata AT + 4 tests"
```

---

## Task Group F: payment-service（支付服务）

### Task F1: Payment Entity + Mapper + Service + Controller + Tests

**Files:**
- Create: `super-market-services/service-payment/src/main/java/com/supermarket/payment/entity/Payment.java`
- Create: `super-market-services/service-payment/src/main/java/com/supermarket/payment/entity/PaymentRefund.java`
- Create: `super-market-services/service-payment/src/main/java/com/supermarket/payment/entity/PaymentIdempotent.java`
- Create: `super-market-services/service-payment/src/main/java/com/supermarket/payment/mapper/PaymentMapper.java`
- Create: `super-market-services/service-payment/src/main/java/com/supermarket/payment/mapper/PaymentRefundMapper.java`
- Create: `super-market-services/service-payment/src/main/java/com/supermarket/payment/mapper/PaymentIdempotentMapper.java`
- Create: `super-market-services/service-payment/src/main/java/com/supermarket/payment/service/PaymentService.java`
- Create: `super-market-services/service-payment/src/main/java/com/supermarket/payment/service/impl/PaymentServiceImpl.java`
- Create: `super-market-services/service-payment/src/main/java/com/supermarket/payment/controller/PaymentController.java`
- Create: `super-market-services/service-payment/src/test/java/com/supermarket/payment/service/PaymentServiceTest.java`
- Create: `super-market-services/service-payment/src/test/resources/application-test.yml`

- [ ] **Step 1: 写入 Entities**

`Payment.java`:
```java
package com.supermarket.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payments")
public class Payment extends BaseEntity {
    private String payNo;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private Integer payMethod;
    private Integer payStatus;
    private String thirdPayNo;
    private LocalDateTime paidAt;
}
```

`PaymentRefund.java`:
```java
package com.supermarket.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supermarket.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_refunds")
public class PaymentRefund extends BaseEntity {
    private String refundNo;
    private String payNo;
    private String orderNo;
    private BigDecimal refundAmount;
    private String refundReason;
    private Integer refundStatus;
    private String thirdRefundNo;
    private LocalDateTime refundedAt;
}
```

`PaymentIdempotent.java`:
```java
package com.supermarket.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_idempotent")
public class PaymentIdempotent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private String businessType;
    private String businessNo;
    private String responseData;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 写入 Mappers**

```java
// PaymentMapper.java
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {}

// PaymentRefundMapper.java
@Mapper
public interface PaymentRefundMapper extends BaseMapper<PaymentRefund> {}

// PaymentIdempotentMapper.java
@Mapper
public interface PaymentIdempotentMapper extends BaseMapper<PaymentIdempotent> {
    @Select("SELECT response_data FROM payment_idempotent WHERE request_id = #{requestId}")
    String getResponseByRequestId(@Param("requestId") String requestId);
}
```

- [ ] **Step 3: 写入 PaymentService + Impl**

`PaymentService.java`:
```java
package com.supermarket.payment.service;

import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.entity.PaymentRefund;

public interface PaymentService {
    Payment createPayment(String orderNo, Long userId, BigDecimal amount, Integer payMethod);
    Payment getByPayNo(String payNo);
    Payment getByOrderNo(String orderNo);
    String handleCallback(String requestId, String payNo, String thirdPayNo);
    PaymentRefund refund(String orderNo, BigDecimal refundAmount, String reason);
}
```

`PaymentServiceImpl.java`:
```java
package com.supermarket.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.dubbo.api.order.OrderDubboService;
import com.supermarket.payment.entity.Payment;
import com.supermarket.payment.entity.PaymentIdempotent;
import com.supermarket.payment.entity.PaymentRefund;
import com.supermarket.payment.mapper.PaymentIdempotentMapper;
import com.supermarket.payment.mapper.PaymentMapper;
import com.supermarket.payment.mapper.PaymentRefundMapper;
import com.supermarket.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    private final PaymentRefundMapper refundMapper;
    private final PaymentIdempotentMapper idempotentMapper;

    @DubboReference(check = false)
    private OrderDubboService orderDubboService;

    @Override
    @Transactional
    public Payment createPayment(String orderNo, Long userId, BigDecimal amount, Integer payMethod) {
        Payment exist = getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, orderNo));
        if (exist != null) throw new BizException(400, "该订单已创建支付单");

        String payNo = "PAY" + IdUtil.getSnowflakeNextId();
        Payment payment = new Payment();
        payment.setPayNo(payNo);
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setPayMethod(payMethod);
        payment.setPayStatus(1); // 待支付
        save(payment);
        return payment;
    }

    @Override
    public Payment getByPayNo(String payNo) {
        Payment p = getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getPayNo, payNo));
        if (p == null) throw new BizException(404, "支付单不存在");
        return p;
    }

    @Override
    public Payment getByOrderNo(String orderNo) {
        return getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, orderNo));
    }

    @Override
    @Transactional
    public String handleCallback(String requestId, String payNo, String thirdPayNo) {
        // 幂等检查
        String cached = idempotentMapper.getResponseByRequestId(requestId);
        if (cached != null) {
            log.info("重复回调: requestId={}", requestId);
            return cached;
        }

        Payment payment = getByPayNo(payNo);
        if (payment.getPayStatus() != 1) {
            throw new BizException(400, "支付单状态不正确");
        }

        // 更新支付单
        payment.setPayStatus(2); // 支付成功
        payment.setThirdPayNo(thirdPayNo);
        payment.setPaidAt(LocalDateTime.now());
        updateById(payment);

        // 通知订单服务
        orderDubboService.updateStatus(payment.getOrderNo(), 2);

        // 记录幂等
        PaymentIdempotent idem = new PaymentIdempotent();
        idem.setRequestId(requestId);
        idem.setBusinessType("PAYMENT");
        idem.setBusinessNo(payNo);
        idem.setResponseData("{\"status\":\"success\"}");
        idempotentMapper.insert(idem);

        return "success";
    }

    @Override
    @Transactional
    public PaymentRefund refund(String orderNo, BigDecimal refundAmount, String reason) {
        Payment payment = getByOrderNo(orderNo);
        if (payment == null || payment.getPayStatus() != 2) {
            throw new BizException(400, "仅已支付订单可退款");
        }

        String refundNo = "RFD" + IdUtil.getSnowflakeNextId();
        PaymentRefund refund = new PaymentRefund();
        refund.setRefundNo(refundNo);
        refund.setPayNo(payment.getPayNo());
        refund.setOrderNo(orderNo);
        refund.setRefundAmount(refundAmount);
        refund.setRefundReason(reason);
        refund.setRefundStatus(2); // 模拟退款成功
        refund.setRefundedAt(LocalDateTime.now());
        refundMapper.insert(refund);

        // 更新支付单状态
        payment.setPayStatus(4); // 已退款
        updateById(payment);

        // 通知订单服务
        orderDubboService.updateStatus(orderNo, 6);

        return refund;
    }
}
```

- [ ] **Step 4: 写入 PaymentController**

```java
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public R<Payment> pay(@RequestParam String orderNo,
                          @RequestParam Long userId,
                          @RequestParam BigDecimal amount,
                          @RequestParam(defaultValue = "1") Integer payMethod) {
        return R.ok(paymentService.createPayment(orderNo, userId, amount, payMethod));
    }

    @PostMapping("/callback/mock")
    public R<String> mockCallback(@RequestParam String requestId,
                                  @RequestParam String payNo) {
        String thirdPayNo = "TPN" + IdUtil.getSnowflakeNextId();
        return R.ok(paymentService.handleCallback(requestId, payNo, thirdPayNo));
    }

    @GetMapping("/{payNo}")
    public R<Payment> query(@PathVariable String payNo) {
        return R.ok(paymentService.getByPayNo(payNo));
    }

    @PostMapping("/refund")
    public R<PaymentRefund> refund(@RequestParam String orderNo,
                                   @RequestParam BigDecimal refundAmount,
                                   @RequestParam String reason) {
        return R.ok(paymentService.refund(orderNo, refundAmount, reason));
    }
}
```

- [ ] **Step 5: 写入单元测试**

`PaymentServiceTest.java` (7 tests):
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private OrderDubboService orderDubboService;

    @Test
    void shouldCreatePayment() {
        Payment payment = paymentService.createPayment("ORD001", 1L, new BigDecimal("99.00"), 1);
        assertThat(payment.getPayNo()).startsWith("PAY");
        assertThat(payment.getPayStatus()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicatePayment() {
        paymentService.createPayment("ORD002", 1L, new BigDecimal("99.00"), 1);
        assertThatThrownBy(() -> paymentService.createPayment("ORD002", 1L, new BigDecimal("99.00"), 1))
            .hasMessageContaining("已创建支付单");
    }

    @Test
    void shouldHandleCallback() {
        Payment payment = paymentService.createPayment("ORD003", 1L, new BigDecimal("99.00"), 1);
        paymentService.handleCallback("req-001", payment.getPayNo(), "TPN001");
        Payment updated = paymentService.getByPayNo(payment.getPayNo());
        assertThat(updated.getPayStatus()).isEqualTo(2);
    }

    @Test
    void shouldBeIdempotentOnRepeatedCallback() {
        Payment payment = paymentService.createPayment("ORD004", 1L, new BigDecimal("99.00"), 1);
        paymentService.handleCallback("req-002", payment.getPayNo(), "TPN002");
        String result = paymentService.handleCallback("req-002", payment.getPayNo(), "TPN003");
        assertThat(result).isEqualTo("success"); // 幂等返回首次结果
    }

    @Test
    void shouldRefund() {
        Payment payment = paymentService.createPayment("ORD005", 1L, new BigDecimal("99.00"), 1);
        paymentService.handleCallback("req-003", payment.getPayNo(), "TPN004");
        PaymentRefund refund = paymentService.refund("ORD005", new BigDecimal("99.00"), "不满意");
        assertThat(refund.getRefundNo()).startsWith("RFD");
        assertThat(refund.getRefundStatus()).isEqualTo(2);
    }
}
```

- [ ] **Step 6: 创建测试 profile**

同 order-service 的 test profile，数据源改为 `db_payment`。

- [ ] **Step 7: 编译 + 测试验证**

```bash
mvn clean test -pl super-market-services/service-payment
```
Expected: `Tests run: 5, Failures: 0`

- [ ] **Step 8: Commit**

```bash
git add super-market-services/service-payment/
git commit -m "feat(payment-service): payment create/callback/refund with idempotency + 5 tests"
```

---

## Task Group G: InventoryDubboService 扩展

### Task G1: 添加 Dubbo 扣减/回补接口

**Files:**
- Create: `super-market-common/common-dubbo-api/src/main/java/com/supermarket/common/dubbo/api/inventory/InventoryDubboService.java`

- [ ] **Step 1: 写入 InventoryDubboService**

```java
package com.supermarket.common.dubbo.api.inventory;

public interface InventoryDubboService {
    boolean deduct(Long skuId, int quantity);
    boolean restore(Long skuId, int quantity);
}
```

- [ ] **Step 2: 在 inventory-service 的 InventoryServiceImpl 上实现**

添加 `@DubboService` 和实现 `InventoryDubboService`:

```java
@DubboService
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory>
        implements InventoryService, InventoryDubboService {

    @Override
    public boolean deduct(Long skuId, int quantity) {
        return deductStock(skuId, quantity);
    }

    @Override
    public boolean restore(Long skuId, int quantity) {
        return restoreStock(skuId, quantity);
    }
}
```

- [ ] **Step 3: 更新测试 profile — inventory-service 添加 Seata 排除**

在 `service-inventory/src/test/resources/application-test.yml` 的 `spring.autoconfigure.exclude` 中添加:
```yaml
- io.seata.spring.boot.autoconfigure.SeataAutoConfiguration
```

- [ ] **Step 4: Commit**

```bash
git add super-market-common/common-dubbo-api/ super-market-services/service-inventory/
git commit -m "feat: add InventoryDubboService for Seata AT inter-service inventory ops"
```

---

## Task Group H: RocketMQ 集成

### Task H1: order-service 添加 RocketMQ 生产者 + 消费者

**Files:**
- Modify: `super-market-services/service-order/pom.xml` — 添加 RocketMQ 依赖
- Create: `super-market-services/service-order/src/main/java/com/supermarket/order/mq/OrderEventProducer.java`
- Create: `super-market-services/service-order/src/main/java/com/supermarket/order/mq/OrderTimeoutConsumer.java`
- Modify: `application-dev.yml` — 添加 RocketMQ 配置

- [ ] **Step 1: pom.xml 添加 RocketMQ 依赖**

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3</version>
</dependency>
```

- [ ] **Step 2: application-dev.yml 添加 RocketMQ 配置**

```yaml
rocketmq:
  name-server: localhost:19876
  producer:
    group: order-producer-group
  consumer:
    group: order-consumer-group
```

- [ ] **Step 3: 写入 OrderEventProducer**

```java
package com.supermarket.order.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    private static final String ORDER_EVENT_TOPIC = "order-event-topic";
    private static final String DELAY_ORDER_TOPIC = "delay-order-topic";

    public void sendOrderCreated(String orderNo) {
        send(ORDER_EVENT_TOPIC, "ORDER_CREATED", orderNo);
    }

    public void sendOrderTimeoutCheck(String orderNo) {
        Message<String> msg = MessageBuilder.withPayload(orderNo).build();
        // Level 16 = 30min (1s/5s/10s/30s/1m/2m/3m/4m/5m/6m/7m/8m/9m/10m/20m/30m/1h/2h)
        rocketMQTemplate.syncSend(DELAY_ORDER_TOPIC, msg, 3000, 16);
        log.info("发送延迟消息: orderNo={}, delayLevel=16(30min)", orderNo);
    }

    public void sendOrderPaid(String orderNo) {
        send(ORDER_EVENT_TOPIC, "ORDER_PAID", orderNo);
    }

    public void sendOrderCancelled(String orderNo) {
        send(ORDER_EVENT_TOPIC, "ORDER_CANCELLED", orderNo);
    }

    private void send(String topic, String tag, String body) {
        Message<String> msg = MessageBuilder.withPayload(body).build();
        rocketMQTemplate.syncSend(topic + ":" + tag, msg);
        log.info("发送消息: topic={}, tag={}", topic, tag);
    }
}
```

- [ ] **Step 4: 写入 OrderTimeoutConsumer（延迟消息消费者）**

```java
package com.supermarket.order.mq;

import com.supermarket.order.entity.Order;
import com.supermarket.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "delay-order-topic",
    consumerGroup = "order-consumer-group"
)
public class OrderTimeoutConsumer implements RocketMQListener<String> {

    private final OrderService orderService;

    @Override
    public void onMessage(String orderNo) {
        log.info("收到延迟消息: orderNo={}", orderNo);
        try {
            Order order = orderService.getByOrderNo(orderNo);
            if (order.getOrderStatus() == 1 && order.getExpireTime().isBefore(LocalDateTime.now())) {
                orderService.cancelOrder(orderNo, "支付超时自动取消");
                log.info("超时订单已取消: orderNo={}", orderNo);
            }
        } catch (Exception e) {
            log.error("处理超时订单失败: orderNo={}", orderNo, e);
        }
    }
}
```

- [ ] **Step 5: 在 OrderServiceImpl.createOrder 中集成消息发送**

在 `createOrder()` 方法末尾添加:
```java
// 发送订单创建事件
orderEventProducer.sendOrderCreated(orderNo);
// 发送延迟消息(30min后检查支付状态)
orderEventProducer.sendOrderTimeoutCheck(orderNo);
```

- [ ] **Step 6: Commit**

```bash
git add super-market-services/service-order/
git commit -m "feat(order-service): add RocketMQ producer + delay message consumer for order events"
```

---

## Task Group I: Gateway 路由更新

### Task I1: 添加 cart/order/payment 路由

- [ ] **Step 1: 在 Gateway application.yml 添加路由**

```yaml
- id: cart-service
  uri: lb://cart-service
  predicates:
    - Path=/api/cart/**
- id: order-service
  uri: lb://order-service
  predicates:
    - Path=/api/order/**
- id: payment-service
  uri: lb://payment-service
  predicates:
    - Path=/api/payment/**
```

- [ ] **Step 2: Commit**

```bash
git add super-market-gateway/
git commit -m "feat(gateway): add cart/order/payment route rules"
```

---

## Task Group J: 全量集成验证

- [ ] **Step 1: 全量编译**

```bash
mvn clean install -DskipTests
```
Expected: `BUILD SUCCESS`

- [ ] **Step 2: 全量测试**

```bash
mvn clean test
```
Expected: 全部测试通过（Phase 2 的 34 个 + Phase 3 的 14 个 = ~48 个测试用例）

- [ ] **Step 3: 验证服务端口**

```bash
echo "=== 新服务 ===" && grep "SERVER_PORT" super-market-services/service-cart/src/main/resources/application.yml && grep "SERVER_PORT" super-market-services/service-order/src/main/resources/application.yml && grep "SERVER_PORT" super-market-services/service-payment/src/main/resources/application.yml
```
Expected: 9321, 9322, 9331

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "chore: Phase 3 complete — order + payment domain (3 services, ~48 tests total)

Implemented:
- cart-service: Redis Hash shopping cart (add/update/remove/select/clear)
- order-service: Seata AT distributed transaction + status flow + RocketMQ events
- payment-service: payment/callback/refund with idempotency
- InventoryDubboService: Dubbo RPC for deduct/restore
- Seata Server: AT mode infrastructure
- RocketMQ: order events + delay message (30min timeout)

DDL: orders, order_items, payments, payment_refunds, payment_idempotent, undo_log (×3)"
```

---

## Phase 3 完成检查清单

### 数据库
- [ ] db_order: orders, order_items, undo_log
- [ ] db_payment: payments, payment_refunds, payment_idempotent, undo_log
- [ ] db_product: undo_log (追加)

### 中间件
- [ ] Seata Server 启动并健康
- [ ] RocketMQ NameServer + Broker 可连接

### 服务
- [ ] cart-service: Redis 购物车 CRUD + 选中管理
- [ ] order-service: 下单(Seata AT) + 状态流转 + 取消 + RocketMQ 消息
- [ ] payment-service: 支付单 + 回调幂等 + 退款
- [ ] InventoryDubboService: 跨服务库存扣减/回补

### 测试
- [ ] cart-service: 5 tests
- [ ] order-service: 4 tests
- [ ] payment-service: 5 tests
- [ ] Phase 2 回归: 34 tests
- [ ] 全量 `mvn clean install -DskipTests` BUILD SUCCESS
- [ ] 全量 `mvn test` 所有测试通过 (~48 tests)

---

> **Plan saved:** `docs/superpowers/plans/2026-05-12-phase3-order-payment-services.md`
>
> **Prerequisites:** JDK 21 + Docker Compose 中间件全部健康 + Phase 2 全部测试通过
>
> **Next:** Execute this plan using `superpowers:subagent-driven-development`.
