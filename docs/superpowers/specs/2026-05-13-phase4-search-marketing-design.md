# Phase 4 — 搜索域 + 营销域 方案设计

> 2026-05-13 | 基于 solution-design.md + PRD v1.0

## 一、总览

### 1.1 服务清单

| 服务 | 中文名称 | 端口 | 数据库 | 核心职责 |
|------|----------|------|--------|----------|
| search-service | 搜索服务 | 9361 | ES + db_product | 商品全文检索、多条件筛选排序、Canal 索引同步 |
| coupon-service | 优惠券服务 | 9351 | db_marketing | 优惠券模板管理、批次发放、用户领取、下单核销 |
| seckill-service | 秒杀服务 | 9352 | db_marketing | 秒杀场次管理、Redis 预减 + Lua 原子扣减、MQ 异步下单 |
| review-service | 评价服务 | 9314 | db_product | 商品星级评分、图文评价、追评 |

### 1.2 集成架构

```
商品服务 (spu/sku 变更)
     │
     ▼ (MySQL binlog)
  Canal Server ──→ search-service ──→ Elasticsearch (smt_product 索引)
                     │
                     ▼ (用户搜索)
                Gateway /api/search/**

秒杀服务 ──→ Redis Lua 原子扣减 ──→ RocketMQ (seckill-order-topic)
     │                                      │
     │                                      ▼
     └── WebSocket 推送结果 ──────── order-service 异步创建订单

优惠券服务 ──→ Redis 库存预热 ──→ Seata AT 下单核销
```

### 1.3 基础设施新增

| 组件 | 新增内容 |
|------|----------|
| **MySQL** | 6 张新表（coupon_templates, coupon_batches, user_coupons, seckill_sessions, seckill_products, reviews） |
| **ES 索引** | `smt_product` — 商品搜索索引（spu + sku + 类目 + 库存 + 评分聚合） |
| **RocketMQ** | `seckill-order-topic` — 秒杀异步下单 |
| **Redis Key** | `smt:seckill:stock:{id}`, `smt:seckill:product:{id}`, `smt:coupon:stock:{id}` |

---

## 二、数据库 DDL

### 2.1 优惠券相关表 (db_marketing)

```sql
-- 优惠券模板表
CREATE TABLE coupon_templates (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type            TINYINT NOT NULL COMMENT '1满减券 2折扣券 3直减券',
    discount_value  DECIMAL(12,2) NOT NULL COMMENT '优惠值(满减金额/折扣率/直减金额)',
    min_amount      DECIMAL(12,2) DEFAULT 0 COMMENT '最低消费金额门槛',
    total_stock     INT NOT NULL DEFAULT 0 COMMENT '总发放量',
    remaining_stock INT NOT NULL DEFAULT 0 COMMENT '剩余数量',
    per_user_limit  INT DEFAULT 1 COMMENT '每人限领数量',
    valid_days      INT NOT NULL COMMENT '有效天数(自领取起)',
    status          TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

-- 优惠券批次表（平台发放记录）
CREATE TABLE coupon_batches (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id     BIGINT NOT NULL COMMENT '优惠券模板ID',
    batch_name      VARCHAR(100) COMMENT '批次名称',
    quantity        INT NOT NULL COMMENT '本批次发放数量',
    distribute_type TINYINT NOT NULL COMMENT '1平台发放 2用户领取 3活动赠送',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券批次表';

-- 用户优惠券表
CREATE TABLE user_coupons (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    template_id     BIGINT NOT NULL COMMENT '优惠券模板ID',
    batch_id        BIGINT COMMENT '发放批次ID',
    coupon_code     VARCHAR(32) NOT NULL UNIQUE COMMENT '券码',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '1未使用 2已使用 3已过期',
    order_no        VARCHAR(32) COMMENT '使用的订单号',
    used_at         DATETIME COMMENT '使用时间',
    expire_time     DATETIME NOT NULL COMMENT '过期时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status),
    INDEX idx_code (coupon_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';
```

### 2.2 秒杀相关表 (db_marketing)

```sql
-- 秒杀场次表
CREATE TABLE seckill_sessions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL COMMENT '秒杀场次名称',
    start_time      DATETIME NOT NULL COMMENT '开始时间',
    end_time        DATETIME NOT NULL COMMENT '结束时间',
    status          TINYINT DEFAULT 0 COMMENT '0未开始 1进行中 2已结束',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀场次表';

-- 秒杀商品表
CREATE TABLE seckill_products (
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
```

### 2.3 评价表 (db_product)

```sql
-- 商品评价表
CREATE TABLE reviews (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    spu_id          BIGINT NOT NULL COMMENT 'SPU ID',
    sku_id          BIGINT COMMENT 'SKU ID',
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
    rating          TINYINT NOT NULL COMMENT '星级 1-5',
    content         TEXT COMMENT '评价内容',
    images          JSON COMMENT '评价图片列表',
    is_anonymous    TINYINT DEFAULT 0 COMMENT '是否匿名 0否 1是',
    reply_content   TEXT COMMENT '商家回复',
    reply_at        DATETIME COMMENT '回复时间',
    status          TINYINT DEFAULT 1 COMMENT '1正常 0隐藏',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_spu (spu_id),
    INDEX idx_user (user_id),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';
```

---

## 三、search-service — 搜索引擎服务

### 3.1 ES 索引设计

```json
// smt_product 索引 Mapping
{
  "properties": {
    "spuId":    { "type": "long" },
    "spuNo":    { "type": "keyword" },
    "shopId":   { "type": "long" },
    "shopName": { "type": "keyword" },
    "categoryId": { "type": "long" },
    "categoryName": { "type": "keyword" },
    "brandName":   { "type": "keyword" },
    "name":     { "type": "text", "analyzer": "ik_max_word", "fields": {"raw": {"type": "keyword"}} },
    "subtitle": { "type": "text", "analyzer": "ik_max_word" },
    "mainImage": { "type": "keyword", "index": false },
    "minPrice": { "type": "double" },
    "maxPrice": { "type": "double" },
    "totalStock": { "type": "integer" },
    "salesCount": { "type": "integer" },
    "avgRating":  { "type": "double" },
    "reviewCount": { "type": "integer" },
    "skuList": {
      "type": "nested",
      "properties": {
        "skuId":    { "type": "long" },
        "specName": { "type": "keyword" },
        "price":    { "type": "double" },
        "stock":    { "type": "integer" },
        "image":    { "type": "keyword", "index": false }
      }
    },
    "shelfStatus": { "type": "integer" },
    "createdAt":   { "type": "date" }
  }
}
```

### 3.2 核心接口

```java
// 商品搜索
GET /api/search/product?keyword=xxx&categoryId=xxx&brand=xxx
                        &minPrice=xxx&maxPrice=xxx
                        &sort=price_asc|price_desc|sales_desc|rating_desc|newest
                        &page=1&size=20

// 响应
{ "total": 1000, "records": [ProductSearchVO...], "aggregations": {
    "categories": [{id, name, count}...],
    "brands":     [{name, count}...],
    "priceRange": {min, max}
}}

// Canal 索引同步 (内部)
POST /api/search/internal/sync
Request: { type: PRODUCT_UPDATED|PRODUCT_DELETED|STOCK_CHANGED|REVIEW_UPDATED, spuId, data }
```

### 3.3 Canal 同步链路

```
MySQL db_product.spu/sku/inventory 变更
  → Canal Server 监听 binlog
  → Canal Client (search-service 内) 消费事件
  → 构建 ES 文档 (聚合 spu + min(sku.price) + sum(inventory.total_stock) + avg(reviews.rating))
  → ES bulk index
```

### 3.4 搜索查询策略

- **分词**: IK Analyzer (ik_max_word 索引, ik_smart 搜索)
- **排序**: 综合评分 `_score` + 销量权重 + 价格因子
- **高亮**: 商品名称 `name` 字段高亮
- **分页**: `from + size`（深度分页 < 1000 条）
- **聚合**: 类目聚合 + 品牌聚合 + 价格区间聚合

---

## 四、coupon-service — 优惠券服务

### 4.1 核心业务流程

```
平台/运营:
  创建优惠券模板 → 生成批次 → 用户可见

用户:
  浏览可用优惠券列表 → 领取(幂等, 库存扣减) → 下单页选择优惠券
  → 订单创建时 Seata AT 核销(user_coupon status=USED)
```

### 4.2 核心接口

```java
// ===== 管理端 =====
// 创建优惠券模板
POST /api/coupon/admin/template
Request: { name, type, discountValue, minAmount, totalStock, perUserLimit, validDays }
Response: { templateId }

// 发放优惠券（批量派发）
POST /api/coupon/admin/distribute
Request: { templateId, userIds:[], quantity }
Response: { batchId, successCount }

// ===== 用户端 =====
// 可领取优惠券列表
GET /api/coupon/available?userId=xxx
Response: [ { templateId, name, type, discountValue, minAmount, remainingStock, validDays } ]

// 用户领取优惠券
POST /api/coupon/claim
Request: { userId, templateId }
Response: { couponId, couponCode, expireTime }
(Redisson分布式锁防超发 + 幂等: userId+templateId唯一约束)

// 我的优惠券
GET /api/coupon/my?userId=xxx&status=1
Response: [ { couponId, couponCode, name, discountValue, minAmount, expireTime, status } ]

// ===== 内部 Dubbo 接口 =====
// 核销优惠券（Seata AT RM 参与者）
Dubbo: CouponDubboService.useCoupon(userId, couponId, orderNo) → boolean
// 退回优惠券（订单取消）
Dubbo: CouponDubboService.returnCoupon(couponId) → boolean
```

### 4.3 防超发设计

- **领取**: Redisson 分布式锁 `smt:lock:coupon:claim:{userId}:{templateId}` + Redis 库存预减 `smt:coupon:stock:{templateId}`
- **核销**: Seata AT undo_log 保证全局事务一致性
- **幂等**: `user_coupons` 表 `UNIQUE(user_id, template_id)` 防止重复领取

---

## 五、seckill-service — 秒杀服务

### 5.1 秒杀时间线

```
T-1h    秒杀商品预热: Redis加载库存、本地Caffeine缓存商品信息
T-5min  用户进入秒杀页: WebSocket推送倒计时
T-0     秒杀开始
  → Gateway Sentinel限流 (令牌桶 10000 QPS)
  → Redisson分布式锁 (用户+商品粒度)
  → Redis Lua脚本原子扣减
  → 抢到: 发送RocketMQ消息 → order-service异步创建订单
  → 未抢到: 返回"已抢光"
  → WebSocket推送抢购结果给用户
```

### 5.2 Lua 脚本

```lua
-- 原子扣减秒杀库存
-- KEYS[1] = smt:seckill:stock:{seckillProductId}
-- ARGV[1] = quantity
local stock = redis.call('get', KEYS[1])
if not stock or tonumber(stock) < tonumber(ARGV[1]) then
    return 0  -- 库存不足
end
redis.call('decrby', KEYS[1], ARGV[1])
return 1  -- 扣减成功
```

### 5.3 核心接口

```java
// ===== 管理端 =====
// 创建秒杀场次
POST /api/seckill/admin/session
Request: { name, startTime, endTime }
Response: { sessionId }

// 配置秒杀商品
POST /api/seckill/admin/product
Request: { sessionId, spuId, skuId, seckillPrice, seckillStock, limitPerUser }
Response: { seckillProductId }

// ===== 用户端 =====
// 秒杀场次列表
GET /api/seckill/sessions
Response: [ { sessionId, name, startTime, endTime, status } ]

// 秒杀商品列表
GET /api/seckill/products?sessionId=xxx
Response: [ { seckillProductId, spuName, skuSpec, seckillPrice, seckillStock, originalPrice } ]

// 执行秒杀
POST /api/seckill/execute
Request: { userId, seckillProductId, quantity }
Response: { success, orderNo?, message }

// ===== 内部 =====
// 秒杀商品预热 (定时任务)
POST /api/seckill/internal/preheat
// 秒杀结束清理 (定时任务)
POST /api/seckill/internal/cleanup
```

### 5.4 异步下单链路

```
Redis Lua扣减成功
  → 发送 RocketMQ seckill-order-topic 消息
  → order-service 消费: 创建预订单 + 扣减真实库存
  → WebSocket推送结果: { orderNo, status: SUCCESS }
  → 失败重试(最多3次) → 最终失败: Redis库存补偿 + 告警
```

---

## 六、review-service — 评价服务

### 6.1 核心接口

```java
// 发表评价
POST /api/review
Request: { userId, spuId, skuId, orderNo, rating, content, images, isAnonymous }
Response: { reviewId }

// 追加评价
POST /api/review/{reviewId}/append
Request: { content, images }

// 商家回复
POST /api/review/{reviewId}/reply
Request: { content }

// SPU评价列表
GET /api/review/list/spu/{spuId}?page=1&size=10&rating=5
Response: { total, avgRating, ratingDistribution: {1:0,2:0,3:5,4:20,5:75}, records: [...] }

// 用户评价列表
GET /api/review/list/user/{userId}?page=1&size=10
```

### 6.2 评分聚合缓存

- Redis Key `smt:product:rating:{spuId}`: Hash `{ avgRating, reviewCount, r1, r2, r3, r4, r5 }`
- 评价发表/修改后异步更新缓存 + 发送 `product-event-topic:REVIEW_UPDATED` 更新 ES

---

## 七、Gateway 路由更新

```yaml
- id: search-service
  uri: lb://search-service
  predicates: - Path=/api/search/**
- id: coupon-service
  uri: lb://coupon-service
  predicates: - Path=/api/coupon/**
- id: seckill-service
  uri: lb://seckill-service
  predicates: - Path=/api/seckill/**
- id: review-service
  uri: lb://review-service
  predicates: - Path=/api/review/**
```

### 鉴权白名单更新

`/api/search/**` 加入白名单（商品搜索无需登录）

---

## 八、验收标准

- [ ] `mvn clean install -DskipTests` BUILD SUCCESS（新增 4 个服务）
- [ ] 所有单元测试通过（目标 20+ 测试用例）
- [ ] ES 商品搜索：关键字检索 + 类目筛选 + 排序，响应时间 P99 < 200ms
- [ ] Canal 数据同步：商品上架 → 1s 内 ES 可搜索
- [ ] 优惠券完整流程：创建模板 → 用户领取 → 下单核销（Seata AT）
- [ ] 优惠券防超发：100 并发领取同一优惠券，实际发放数 ≤ total_stock
- [ ] 秒杀 Redis Lua 原子扣减：1000 并发抢 100 库存，不超卖
- [ ] 秒杀异步下单：MQ → order-service 创建订单 → WebSocket 推送结果
- [ ] 评价发表+追评+商家回复+评分聚合
