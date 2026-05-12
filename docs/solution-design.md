# 京东式商城系统 — 需求分析与方案设计

> 基于 PRD v1.0 | 2026-05-12

---

## 第一部分：需求分析

### 1. 业务目标与范围

| 维度 | 指标 |
|------|------|
| 注册用户 | 100万 ~ 1000万 |
| 日活用户 | 10万 ~ 100万 |
| 峰值 QPS | 5,000 ~ 20,000 |
| 订单峰值 | 1,000 ~ 5,000 单/秒 |
| 终端 | PC Web（用户前台 + 商家后台 + 运营后台） |

### 2. 用户角色分析

```
                    ┌──────────────────┐
                    │   系统用户体系     │
                    └────────┬─────────┘
           ┌─────────────────┼─────────────────┐
           ▼                 ▼                  ▼
   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
   │   C端用户     │  │   B端商家     │  │  平台运营     │
   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
   │ · 注册/登录   │  │ · 店铺管理   │  │ · 商家审核   │
   │ · 浏览商品   │  │ · 商品上架   │  │ · 类目管理   │
   │ · 下单支付   │  │ · 订单处理   │  │ · 活动配置   │
   │ · 评价商品   │  │ · 营销活动   │  │ · 风控审核   │
   │ · 售后申请   │  │ · 财务结算   │  │ · 数据分析   │
   └──────────────┘  └──────────────┘  └──────────────┘
```

### 3. 功能性需求（按业务域拆分）

#### 3.1 用户域
| 编号 | 功能 | 优先级 | 描述 |
|------|------|--------|------|
| U-01 | 用户注册 | P0 | 手机号/邮箱注册，验证码校验 |
| U-02 | 用户登录 | P0 | 账号密码登录 + JWT Token 签发 |
| U-03 | 第三方登录 | P2 | 微信/QQ OAuth 2.0 登录 |
| U-04 | 会员等级 | P1 | 青铜/白银/黄金/钻石，按消费累计升级 |
| U-05 | 会员权益 | P1 | 折扣、包邮、专属客服、积分倍率 |
| U-06 | 收货地址 CRUD | P0 | 最多 20 个地址，支持默认地址 |
| U-07 | 实名认证 | P1 | 身份证 + 人脸识别 |

#### 3.2 商品域
| 编号 | 功能 | 优先级 | 描述 |
|------|------|--------|------|
| P-01 | SPU/SKU 管理 | P0 | 商品基础信息 CRUD，支持多规格 |
| P-02 | 类目管理 | P0 | 三级类目树，属性模板绑定 |
| P-03 | 商品审核 | P0 | 商家提交 → 平台审核 → 上架/驳回 |
| P-04 | 库存管理 | P0 | 库存扣减/回补/预警，支持安全库存 |
| P-05 | 商品评价 | P1 | 星级评分 + 图文评价 + 追评 |
| P-06 | 商品搜索 | P1 | 全文检索、多条件筛选、排序 |
| P-07 | 商品推荐 | P2 | 基于用户行为的个性化推荐 |

#### 3.3 交易域
| 编号 | 功能 | 优先级 | 描述 |
|------|------|--------|------|
| T-01 | 购物车 | P0 | 添加/删除/修改数量，支持选中结算 |
| T-02 | 下单 | P0 | 购物车 → 确认订单 → 提交订单 |
| T-03 | 订单状态流转 | P0 | 待付款→待发货→待收货→已完成→已取消 |
| T-04 | 订单查询 | P0 | 按状态/时间筛选，分页查询 |
| T-05 | 取消订单 | P1 | 未付款自动取消(30min)，已付款需审核 |
| T-06 | 售后退款 | P1 | 申请→审核→退货→退款 完整流程 |
| T-07 | 结算计算 | P0 | 商品金额+运费+优惠-折扣=实付金额 |

#### 3.4 支付域
| 编号 | 功能 | 优先级 | 描述 |
|------|------|--------|------|
| PAY-01 | 支付发起 | P0 | 对接支付宝/微信支付，生成支付单 |
| PAY-02 | 支付回调 | P0 | 异步回调处理，幂等校验，状态同步 |
| PAY-03 | 账户管理 | P1 | 钱包余额、充值、提现 |
| PAY-04 | 对账 | P1 | 日终对账，差异处理，对账报表 |
| PAY-05 | 退款 | P1 | 原路退回/余额退回 |

#### 3.5 营销域
| 编号 | 功能 | 优先级 | 描述 |
|------|------|--------|------|
| M-01 | 优惠券 | P1 | 满减券/折扣券，发放/领取/核销 |
| M-02 | 秒杀 | P1 | 限时限量，独立秒杀库存，预热+削峰 |
| M-03 | 积分体系 | P1 | 签到得积分，消费返积分，积分兑换 |
| M-04 | 满减活动 | P1 | 阶梯满减，多级优惠叠加规则 |

#### 3.6 商家域
| 编号 | 功能 | 优先级 | 描述 |
|------|------|--------|------|
| S-01 | 商家入驻 | P0 | 提交资质 → 平台审核 → 开通店铺 |
| S-02 | 店铺管理 | P0 | 店铺信息、装修、公告管理 |
| S-03 | 订单处理 | P0 | 查看订单、发货、处理退款申请 |
| S-04 | 商家结算 | P1 | 结算单生成、对账、打款 |

#### 3.7 平台域
| 编号 | 功能 | 优先级 | 描述 |
|------|------|--------|------|
| O-01 | 商家审核 | P0 | 资质审核，入驻审批 |
| O-02 | 商品审核 | P0 | 违规商品下架，类目调整 |
| O-03 | 运营配置 | P1 | Banner、广告位、首页推荐配置 |
| O-04 | 风控 | P1 | 刷单检测、恶意退款识别、IP黑名单 |
| O-05 | 数据分析 | P2 | GMV、订单量、用户增长等报表 |

### 4. 非功能性需求

| 类别 | 需求 | 指标 |
|------|------|------|
| **性能** | 接口响应时间 | P99 < 200ms（核心接口）|
| **性能** | 页面加载时间 | 首屏 < 2s |
| **可用性** | 核心服务 SLA | 99.99%（全年宕机 < 53min）|
| **可用性** | 非核心服务 SLA | 99.9% |
| **伸缩性** | 弹性伸缩 | 基于 CPU/Memory/QPS 自动扩缩容 |
| **安全性** | 认证 | JWT + OAuth 2.0 |
| **安全性** | 防护 | XSS/CSRF/SQL注入/WAF |
| **安全性** | 数据加密 | 传输层 TLS 1.3，存储层 AES-256 |
| **可观测性** | 监控 | Prometheus + Grafana |
| **可观测性** | 链路追踪 | SkyWalking 全链路追踪 |
| **可观测性** | 日志 | ELK 集中日志 |

### 5. 核心业务流程分析

#### 5.1 用户下单流程（关键路径）

```
用户浏览商品 ──→ 加入购物车 ──→ 点击结算
                                    │
                                    ▼
                            确认订单页面
                      （选择地址、优惠券、支付方式）
                                    │
                                    ▼
                            提交订单 ──→ 订单服务创建订单(预创建)
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
             库存服务扣减     优惠券服务核销    积分服务计算
           (Seata AT事务)   (Seata AT事务)   (Seata AT事务)
                    │               │               │
                    └───────────────┼───────────────┘
                                    ▼
                            订单状态 → 待付款
                                    │
                                    ▼
                            支付服务发起支付
                                    │
                        ┌───────────┴───────────┐
                        ▼                       ▼
                  支付成功(回调)          支付超时(30min)
                        │                       │
                        ▼                       ▼
                  订单→待发货            订单→已取消
                  通知商家发货            库存回补+优惠券退回
```

#### 5.2 商品上架流程

```
商家创建商品(草稿) ──→ 填写SPU信息 + SKU规格 + 图片上传
                                    │
                                    ▼
                            提交审核 ──→ 平台运营审核
                                    │
                        ┌───────────┴───────────┐
                        ▼                       ▼
                    审核通过                 审核驳回
                        │                       │
                        ▼                       ▼
              商品服务持久化SPU/SKU        通知商家修改
              库存服务初始化库存
              发送RocketMQ事务消息
                        │
                        ▼
              Canal监听binlog变化
                        │
                        ▼
              索引同步服务更新ES
                        │
                        ▼
              商品可搜索/可购买
```

#### 5.3 售后退款流程

```
用户申请退款 ──→ 退款服务创建退款单
                        │
                ┌───────┴───────┐
                ▼               ▼
          仅退款(未发货)    退货退款(已发货)
                │               │
                ▼               ▼
         商家审核通过      用户退货+填物流
                │               │
                ▼               ▼
         退款服务退款      商家确认收货
                │               │
                ▼               ▼
         支付回调处理      退款服务退款
                │               │
                └───────┬───────┘
                        ▼
              订单状态更新 → 已退款
              库存服务回补库存
```

---

## 第二部分：方案设计

### 1. 系统架构总览

```
┌──────────────────────────────────────────────────────────────────┐
│                        客户端层 (Client)                          │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐    │
│  │  用户前台 (B2C)  │ │  商家后台 (B2B)  │ │  运营后台 (Admin) │    │
│  │  Vue3 + Vite    │ │  Vue3 + Vite    │ │  Vue3 + Vite    │    │
│  │  Element Plus   │ │  Element Plus   │ │  Element Plus   │    │
│  └────────┬────────┘ └────────┬────────┘ └────────┬────────┘    │
└───────────┼──────────────────┼──────────────────┼───────────────┘
            │                  │                  │
            └──────────────────┼──────────────────┘
                               │ HTTPS
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│                        接入层 (Access)                            │
│  Nginx (反向代理 + 静态资源 + gzip)                                │
│  生产环境: MetalLB + Nginx Ingress + ModSecurity WAF              │
└─────────────────────────────┬────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                        网关层 (Gateway)                           │
│  Spring Cloud Gateway 集群                                        │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐        │
│  │ 鉴权认证  │ 限流熔断  │ 请求路由  │ 协议转换  │ 链路追踪  │        │
│  │ JWT验签  │ Sentinel │ 路径匹配  │HTTP→Dubbo│SkyWalking│        │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘        │
└─────────────────────────────┬────────────────────────────────────┘
                              │ Dubbo 3.x (Triple Protocol / TCP)
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────────────────────────────────────────────────────────┐
│                      业务服务层 (Business Services)               │
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │ 用户服务  │ │ 商品服务  │ │ 订单服务  │ │ 支付服务  │            │
│  │ user-svc │ │ prod-svc │ │order-svc │ │ pay-svc  │            │
│  └─────┬────┘ └─────┬────┘ └─────┬────┘ └─────┬────┘            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │ 营销服务  │ │ 搜索服务  │ │ 商家服务  │ │ 平台服务  │            │
│  │  mkt-svc │ │search-svc│ │ shop-svc │ │ plat-svc │            │
│  └─────┬────┘ └─────┬────┘ └─────┬────┘ └─────┬────┘            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                         │
│  │ 消息服务  │ │ 文件服务  │ │ 通知服务  │                         │
│  │ msg-svc  │ │ file-svc │ │notify-svc│                         │
│  └──────────┘ └──────────┘ └──────────┘                         │
│                                                                  │
│  注册中心/配置中心: Nacos 3节点集群                                │
│  服务调用: Dubbo 3.2.x Triple Protocol                            │
└─────────────────────────────┬────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────────────────────────────────────────────────────────┐
│                      中间件层 (Middleware)                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │ RocketMQ │ │  Redis   │ │    ES    │ │  Seata   │            │
│  │ 消息队列  │ │ 分布式缓存 │ │ 搜索引擎  │ │ 分布式事务 │            │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │ Sentinel │ │ XXL-Job  │ │SkyWalking│ │ Redisson │            │
│  │ 熔断降级  │ │ 任务调度  │ │ 链路追踪  │ │ 分布式锁  │            │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘            │
└─────────────────────────────┬────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────────────────────────────────────────────────────────┐
│                     数据存储层 (Data Storage)                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │  MySQL   │ │ MongoDB  │ │  MinIO   │ │ClickHouse│            │
│  │ 关系数据库│ │ NoSQL    │ │ 对象存储  │ │ 数据仓库  │            │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘            │
│  分库分表: ShardingSphere-JDBC 5.4.x                             │
│  数据同步: Canal 1.1.x → ES / ClickHouse                         │
└──────────────────────────────────────────────────────────────────┘
```

### 2. 微服务详细设计

#### 2.1 服务清单与接口契约

```
服务总数: 16 个核心微服务

基础服务 (P0):
  ├── user-service       用户服务 (8001)
  ├── auth-service       认证授权服务 (8002)
  ├── member-service     会员服务 (8003)
  ├── address-service    地址服务 (8004)
  ├── product-service    商品基础服务 (8101)
  ├── category-service   类目服务 (8102)
  ├── inventory-service  库存服务 (8103)
  ├── review-service     评价服务 (8104)
  ├── cart-service       购物车服务 (8201)
  ├── order-service      订单服务 (8202)
  ├── payment-service    支付服务 (8301)
  ├── shop-service       商家/店铺服务 (8401)

扩展服务 (P1):
  ├── coupon-service     优惠券服务 (8501)
  ├── seckill-service    秒杀服务 (8502)
  ├── search-service     搜索服务 (8601)

支撑服务 (P1):
  ├── file-service       文件服务 (8701)
  ├── notify-service     通知服务 (8702)
  └── platform-service   运营/风控服务 (8801)
```

#### 2.2 核心服务接口定义

**用户服务 (user-service)**

```java
// 用户注册
POST /api/user/register
Request:  { phone, password, verifyCode }
Response: { userId, token }

// 用户登录
POST /api/user/login
Request:  { phone, password }
Response: { userId, token, refreshToken }

// 获取用户信息
GET /api/user/info?userId={id}
Response: { userId, nickname, avatar, phone, memberLevel }
```

**商品服务 (product-service)**

```java
// 创建商品(商家)
POST /api/product/spu
Request:  { name, categoryId, brandId, images, skus: [...], description }
Response: { spuId }

// 商品详情(SPU + SKU列表)
GET /api/product/spu/{spuId}
Response: { spuId, name, category, brand, skus: [...], images, status }

// 商品列表(分页+筛选)
GET /api/product/list?page=1&size=20&categoryId=xxx&keyword=xxx&sort=price_asc
Response: { total, records: [...], pages }

// 商品审核(运营)
PUT /api/product/spu/{spuId}/audit
Request:  { status: APPROVED|REJECTED, reason }
```

**库存服务 (inventory-service)**

```java
// 扣减库存(订单创建时)
Dubbo: InventoryService.deduct(List<DeductItem> items)
  DeductItem: { skuId, quantity, orderNo }
  Response: { success, failedItems: [...] }

// 回补库存(取消订单/退款)
Dubbo: InventoryService.restore(List<RestoreItem> items)

// 查询库存
GET /api/inventory/sku/{skuId}
Response: { skuId, totalStock, availableStock, lockedStock }
```

**订单服务 (order-service)**

```java
// 创建订单
POST /api/order/create
Request:  { addressId, cartItemIds: [...], couponId, payMethod, remark }
Response: { orderNo, totalAmount, actualAmount, expireTime }

// 订单列表
GET /api/order/list?status=xxx&page=1&size=10
Response: { total, records: [OrderVO...] }

// 订单详情
GET /api/order/{orderNo}
Response: { orderNo, status, items, address, payment, timeline }

// 订单状态变更(内部)
Dubbo: OrderService.updateStatus(orderNo, fromStatus, toStatus)

// 取消订单
PUT /api/order/{orderNo}/cancel
Request:  { reason }
```

**支付服务 (payment-service)**

```java
// 发起支付
POST /api/payment/pay
Request:  { orderNo, payMethod: ALIPAY|WECHAT }
Response: { payNo, payUrl, qrCode }

// 支付回调(第三方)
POST /api/payment/callback/alipay
Request:  { ...支付宝回调参数 }
(验签 + 幂等 + 更新状态 + 通知订单服务)

// 退款
POST /api/payment/refund
Request:  { orderNo, refundAmount, reason }
Response: { refundNo, status }
```

**购物车服务 (cart-service)**

```java
// 加入购物车
POST /api/cart/add
Request:  { skuId, quantity }

// 购物车列表
GET /api/cart/list
Response: [ { cartItemId, skuId, spuName, skuInfo, price, quantity, selected } ]

// 修改数量
PUT /api/cart/item/{cartItemId}
Request:  { quantity }

// 选中/取消选中
PUT /api/cart/item/{cartItemId}/select
Request:  { selected: true|false }
```

### 3. 数据库设计

#### 3.1 分库策略

```
                            ┌──────────────────┐
                            │   Nacos 配置中心   │
                            │  数据源 & 分片规则  │
                            └────────┬─────────┘
                                     │
           ┌─────────────────────────┼─────────────────────────┐
           │                         │                         │
           ▼                         ▼                         ▼
   ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
   │ ShardingSphere│         │  读写分离     │         │  分表策略     │
   │  垂直分库     │         │  一主多从     │         │  哈希取模     │
   └──────────────┘         └──────────────┘         └──────────────┘
```

| 数据库 | 所属服务 | 核心表 | 分表策略 |
|--------|----------|--------|----------|
| db_user | user/auth/member/address | users, members, addresses | 无(用户量级可控) |
| db_product | product/category/inventory/review | spu, sku, categories, reviews | sku按商品ID哈希 16表 |
| db_order | order/cart | orders, order_items, carts | orders按用户ID哈希 16表 |
| db_payment | payment | payments, refunds, accounts | payments按支付单号哈希 8表 |
| db_marketing | coupon/seckill/points | coupons, seckill_products, points | 无 |
| db_shop | shop | shops, merchants, settlements | 无 |
| db_platform | platform | audits, configs, risks | 无 |

#### 3.2 核心表结构

**users 表**

```sql
CREATE TABLE users (
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
```

**spu / sku 表**

```sql
CREATE TABLE spu (
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

CREATE TABLE sku (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku_no          VARCHAR(32) NOT NULL UNIQUE COMMENT 'SKU编号',
    spu_id          BIGINT NOT NULL COMMENT 'SPU ID',
    spec_name       VARCHAR(100) COMMENT '规格名称(颜色:红色;尺寸:XL)',
    spec_code       VARCHAR(100) COMMENT '规格编码',
    price           DECIMAL(12,2) NOT NULL COMMENT '售价(分)',
    market_price    DECIMAL(12,2) COMMENT '市场价',
    cost_price      DECIMAL(12,2) COMMENT '成本价',
    image           VARCHAR(500) COMMENT 'SKU图片',
    weight          INT DEFAULT 0 COMMENT '重量(g)',
    status          TINYINT DEFAULT 1 COMMENT '1正常 0停用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_spu (spu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU商品表';
```

**orders 表（分表）**

```sql
CREATE TABLE orders (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号(雪花ID)',
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

CREATE TABLE order_items (
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
```

**库存表**

```sql
CREATE TABLE inventory (
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

### 4. 缓存设计

#### 4.1 多级缓存架构

```
┌─────────────────────────────────────────────────────┐
│                   请求进入                            │
└─────────────────────┬───────────────────────────────┘
                      ▼
┌─────────────────────────────────────────────────────┐
│  L1: 本地缓存 (Caffeine)                             │
│  容量: 1000条 | TTL: 60s | 命中率目标: 80%+          │
│  适用: 类目树、字典配置、热点商品基础信息              │
└─────────────────────┬───────────────────────────────┘
                      │ MISS
                      ▼
┌─────────────────────────────────────────────────────┐
│  L2: 分布式缓存 (Redis 7.2)                          │
│  容量: 32GB | TTL: 按业务场景 | 命中率目标: 95%+     │
│  适用: 用户Session、商品详情、库存、购物车、热点数据   │
└─────────────────────┬───────────────────────────────┘
                      │ MISS
                      ▼
┌─────────────────────────────────────────────────────┐
│  L3: 数据库 (MySQL)                                  │
│  最后兜底，查询后回写 L1 + L2                        │
└─────────────────────────────────────────────────────┘
```

#### 4.2 Redis Key 设计规范

```
Key 命名规范: {项目前缀}:{业务域}:{业务含义}:{标识}

# 用户相关
smt:user:token:{userId}              → JWT Token (TTL: 7d)
smt:user:info:{userId}               → 用户信息 Hash (TTL: 1h)

# 商品相关
smt:product:spu:{spuId}              → SPU详情 Hash (TTL: 30min)
smt:product:sku:{skuId}              → SKU信息 Hash (TTL: 30min)
smt:product:category:tree            → 类目树 JSON (TTL: 1h)

# 库存相关
smt:inventory:stock:{skuId}          → 库存数量 (TTL: 永久)
smt:inventory:stock:lock:{skuId}     → 分布式锁 Key (TTL: 30s)

# 购物车
smt:cart:{userId}                    → Hash {skuId: quantity} (TTL: 30d)

# 秒杀
smt:seckill:product:{spuId}          → 秒杀商品信息 (预热加载)
smt:seckill:stock:{skuId}            → 秒杀库存 (预扣减, TTL: 活动结束)

# 订单
smt:order:info:{orderNo}             → 订单详情 Hash (TTL: 1h)
smt:order:repeat:{requestId}         → 幂等Key (TTL: 1h)

# 通用
smt:lock:{resource}:{id}             → 分布式锁 (TTL: 30s)
smt:rate:{api}:{ip}                  → 限流计数 (TTL: 1s)
```

### 5. 消息队列设计

#### 5.1 Topic 规划

```
RocketMQ Topic 设计:

Topic: order-event-topic         订单事件
  Tag: ORDER_CREATED             订单创建 → 通知服务发消息, 营销服务发积分
  Tag: ORDER_PAID                支付成功 → 通知商家发货, 数据服务统计
  Tag: ORDER_SHIPPED             已发货 → 通知用户
  Tag: ORDER_CANCELLED           已取消 → 库存回补, 优惠券退回
  Tag: ORDER_REFUNDED            已退款 → 库存回补, 数据服务

Topic: product-event-topic       商品事件
  Tag: PRODUCT_APPROVED          商品审核通过 → 索引同步服务更新ES
  Tag: PRODUCT_UPDATED           商品更新 → 索引同步, 缓存清理
  Tag: PRODUCT_SHELF_CHANGE      上下架变更 → 索引同步, 缓存清理

Topic: inventory-event-topic     库存事件
  Tag: STOCK_CHANGED             库存变更 → 同步ES库存
  Tag: STOCK_ALARM               库存不足预警 → 通知商家

Topic: delay-order-topic         延迟消息(订单超时)
  Tag: ORDER_PAYMENT_TIMEOUT     下单后30min未支付 → RocketMQ延迟消息Level 16

Topic: canal-sync-topic          Canal数据同步
  Tag: BINLOG_CHANGE             DB变更事件 → 更新ES/缓存/ClickHouse
```

#### 5.2 事务消息（商品上架示例）

```
商家提交商品审核通过:
  1. 商品服务发送半消息 (RocketMQ事务消息, Tag: PRODUCT_APPROVED)
  2. 商品服务执行本地事务:
     - 更新 spu.audit_status = 1
     - 写入 spu 和 sku 记录
  3. 本地事务成功 → 提交消息
  4. 索引同步服务消费:
     - 构建ES索引文档
     - 同步到 ES

如果本地事务失败 → 回滚半消息 → 消费者不会收到
```

### 6. 安全设计

#### 6.1 认证鉴权流程

```
用户登录
  │
  ▼
POST /api/auth/login (phone + password)
  │
  ▼
auth-service 校验密码(BCrypt) ──→ 失败: 返回401
  │ 成功
  ▼
生成 Access Token (JWT, TTL=2h, 含 userId+roles)
生成 Refresh Token (TTL=7d)
  │
  ▼
返回 { accessToken, refreshToken }
  │
  ▼
前端存储: accessToken → 内存, refreshToken → httpOnly Cookie
  │
  ▼
后续请求: Authorization: Bearer {accessToken}
  │
  ▼
Spring Cloud Gateway 全局过滤器:
  1. 提取 Token
  2. JWT 验签 (RSA256)
  3. 解析 userId + roles
  4. 透传 Header: X-User-Id, X-User-Roles
  │
  ▼
Access Token 过期 → 自动用 Refresh Token 换取新 Token
```

#### 6.2 权限模型 RBAC

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│   用户    │──N:M│   角色    │──N:M│   权限    │
│  User    │     │  Role    │     │Permission│
└──────────┘     └──────────┘     └──────────┘

角色定义:
  ROLE_USER      普通用户 (浏览、下单、评价)
  ROLE_MERCHANT  商家 (商品管理、订单处理、店铺管理)
  ROLE_ADMIN     运营管理员 (审核、配置、风控)

权限粒度:
  product:read    → 查看商品
  product:write   → 编辑商品(商家)
  product:audit   → 审核商品(运营)
  order:read      → 查看订单
  order:cancel    → 取消订单
  ...
```

#### 6.3 安全防护清单

| 防护项 | 方案 |
|--------|------|
| XSS | Vue3 默认转义 + CSP Header |
| CSRF | SameSite Cookie + CSRF Token |
| SQL注入 | MyBatis-Plus 参数化查询 |
| 接口限流 | Sentinel 网关层 + 服务层双层限流 |
| 敏感数据 | 密码BCrypt, 身份证AES-256加密存储 |
| 传输安全 | Nginx TLS 1.3 终止 |
| 文件上传 | 类型白名单 + 大小限制 + 病毒扫描 |
| 幂等 | 订单创建/支付使用 requestId 做唯一索引 |

### 7. 关键场景技术方案

#### 7.1 秒杀场景

```
时间线:
  T-1h    秒杀商品预热: 秒杀库存加载到Redis, 商品信息加载到本地缓存
  T-5min  用户进入秒杀页面: WebSocket推送倒计时
  T-0     秒杀开始

请求链路:
  用户点击秒杀
    → Gateway限流 (Sentinel: 令牌桶, 10000 QPS)
    → SeckillService.seckill(userId, seckillSkuId)
      → Redisson分布式锁 (key: smt:lock:seckill:{skuId}:{userId})
      → Redis Lua脚本原子扣减秒杀库存
        if redis.call('get', KEYS[1]) > 0 then
          redis.call('decr', KEYS[1])
          return 1  -- 抢到
        else
          return 0  -- 已抢完
        end
      → 抢到: 发送RocketMQ消息异步创建订单
      → 未抢到: 返回"已抢光"

  异步下单流程:
    RocketMQ Consumer → 创建预订单 → 扣减真实库存 → 返回订单号
    WebSocket推送结果给用户
```

#### 7.2 分布式事务（下单核心链路）

```
场景: 用户提交订单

参与服务: 订单服务、库存服务、优惠券服务、积分服务
事务模式: Seata AT 模式

流程:
  订单服务 (TM - 事务发起方)
    │
    ├──→ 库存服务 (RM) - deductStock
    │     Seata代理数据源: UPDATE inventory SET available_stock = ? - ?, locked_stock = ? + ?
    │     同时写入 undo_log 表
    │
    ├──→ 优惠券服务 (RM) - useCoupon
    │     Seata代理数据源: UPDATE user_coupon SET status = 'USED'
    │
    └──→ 订单服务 (RM) - createOrder
          Seata代理数据源: INSERT INTO orders + INSERT INTO order_items

  全部成功 → 提交全局事务 (Seata TC 通知所有 RM commit, 删除 undo_log)
  任一失败 → 回滚全局事务 (Seata TC 通知所有 RM rollback, 根据 undo_log 回滚数据)
```

#### 7.3 库存扣减方案

```
方案: Redis预减 + DB最终一致

下单流程:
  1. 检查 Redis 库存 (> 0)
  2. Redis 预减库存 (DECR)
  3. 发送 RocketMQ 消息
  4. 消费端: DB 真实扣减 (乐观锁)
     UPDATE inventory
     SET available_stock = available_stock - #{quantity},
         locked_stock = locked_stock + #{quantity},
         version = version + 1
     WHERE sku_id = #{skuId} AND available_stock >= #{quantity} AND version = #{version}
  5. DB扣减失败 → 重试 (最多3次) → 最终失败 → Redis库存补偿 + 告警

超时释放:
  - RocketMQ延迟消息 (30min)
  - 消费端检查订单状态 = 待付款 → 取消订单 + 释放库存

防止超卖:
  - DB层面: 乐观锁 version 保证
  - Redis层面: Lua脚本原子操作
  - 对账: 定时任务对比 Redis 和 DB 库存, 差异告警
```

### 8. 部署架构

#### 8.1 Docker Compose 开发环境

```yaml
# 目录结构
docker-compose/
├── docker-compose.yml          # 主编排文件
├── .env                        # 环境变量
├── mysql/
│   └── init/                   # 初始化SQL脚本
├── nacos/
│   └── conf/                   # Nacos配置
├── rocketmq/
│   └── conf/                   # RocketMQ配置
└── nginx/
    └── conf.d/                 # Nginx配置

# 开发环境中间件端口规划
Nacos:            8848 (HTTP), 9848 (gRPC)
MySQL:            3306
Redis:            6379
RocketMQ:         9876 (NameServer), 10911 (Broker)
Elasticsearch:    9200
MinIO:            9000 (API), 9001 (Console)
XXL-Job:          8080
SkyWalking:       11800 (gRPC), 12800 (HTTP)
Sentinel:         8858
Canal:            11111
```

#### 8.2 K8s 生产部署

```
集群规划 (最低配置: 3 Master + 5 Worker):

Master 节点 (3台, 4C8G):
  ├── etcd (3节点, 与Master混部)
  ├── kube-apiserver + kube-scheduler + kube-controller-manager
  └── Nginx Ingress Controller (通过MetalLB暴露)

Worker 节点 (5台, 8C16G):
  ├── 业务服务 (Dubbo微服务集群, Deployment)
  ├── 中间件 (StatefulSet)
  └── 存储 (StatefulSet + PV/PVC)

MetalLB (Layer 2模式):
  分配VIP段: 192.168.1.200-192.168.1.250
  对外暴露: Nginx Ingress (80/443)

存储方案:
  ├── 本地存储: hostPath (开发/测试)
  └── 分布式存储: Longhorn / Rook Ceph (生产)
```

### 9. 监控与可观测性

```
┌─────────────────────────────────────────────────────────┐
│                    可观测性三支柱                         │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Metrics    │  │   Tracing    │  │   Logging    │  │
│  │  指标        │  │  链路追踪     │  │  日志        │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                 │          │
│         ▼                 ▼                 ▼          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Prometheus  │  │  SkyWalking  │  │  ELK Stack   │  │
│  │  + Grafana   │  │  9.7.x      │  │  (ES+Kibana) │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                         │
│  关键告警规则:                                          │
│  ├── 服务可用率 < 99.9% → P1 告警                      │
│  ├── 接口P99延迟 > 500ms → P2 告警                     │
│  ├── Redis内存 > 80% → P2 告警                         │
│  ├── 消息积压 > 10000 → P1 告警                        │
│  ├── 订单成功率 < 99% → P0 告警 (立即处理)              │
│  └── DB连接池耗尽 → P1 告警                            │
└─────────────────────────────────────────────────────────┘
```

### 10. 开发路线图

```
Phase 1 (W1-W2): 基础设施搭建
  ├── Docker Compose 中间件编排
  ├── Maven 父工程 + 公共模块 (common, api)
  ├── 项目脚手架生成 (16个微服务模块)
  └── CI/CD 流水线搭建

Phase 2 (W3-W6): 核心业务域 — 用户 + 商品
  ├── user-service + auth-service + address-service
  ├── product-service + category-service + inventory-service
  ├── shop-service
  └── Gateway + Nacos + Sentinel 集成

Phase 3 (W7-W10): 核心业务域 — 交易 + 支付
  ├── cart-service + order-service
  ├── payment-service
  ├── Seata 分布式事务集成
  └── RocketMQ 消息驱动

Phase 4 (W11-W13): 搜索 + 营销
  ├── search-service + Canal 数据同步
  ├── coupon-service + seckill-service
  ├── member-service + 积分体系
  └── ES 搜索引擎集成

Phase 5 (W14-W16): 支撑服务 + 完善
  ├── review-service + notify-service
  ├── file-service + MinIO 集成
  ├── platform-service (运营后台)
  ├── XXL-Job 定时任务
  └── SkyWalking + Prometheus + Grafana

Phase 6 (W17-W18): 压测 + 优化
  ├── JMeter 全链路压测
  ├── 性能调优 (JVM、DB、缓存)
  ├── 高可用演练 (故障注入)
  └── 文档完善
```

### 11. 项目代码结构

```
super-market/
├── docs/                           # 文档
│   ├── prd.md                      # 产品需求文档
│   └── solution-design.md          # 本方案设计文档
├── docker-compose/                 # 本地开发环境
│   ├── docker-compose.yml
│   ├── .env
│   └── conf.d/                     # 各中间件配置
├── frontend/                       # 前端项目 (Monorepo)
│   ├── app-b2c/                    # 用户前台
│   ├── app-b2b/                    # 商家后台
│   ├── app-admin/                  # 运营后台
│   └── packages/                   # 公共组件库
├── super-market-common/            # 公共模块
│   ├── common-core/                # 核心工具/异常/常量
│   ├── common-dubbo-api/           # Dubbo API 接口定义
│   ├── common-security/            # 安全模块 (JWT/RBAC)
│   ├── common-web/                 # Web 通用配置
│   └── common-mybatis/             # MyBatis 通用配置
├── super-market-services/          # 微服务实现
│   ├── service-user/               # 用户服务
│   ├── service-auth/               # 认证授权服务
│   ├── service-member/             # 会员服务
│   ├── service-address/            # 地址服务
│   ├── service-product/            # 商品基础服务
│   ├── service-category/           # 类目服务
│   ├── service-inventory/          # 库存服务
│   ├── service-review/             # 评价服务
│   ├── service-cart/               # 购物车服务
│   ├── service-order/              # 订单服务
│   ├── service-payment/            # 支付服务
│   ├── service-coupon/             # 优惠券服务
│   ├── service-seckill/            # 秒杀服务
│   ├── service-search/             # 搜索服务
│   ├── service-shop/               # 商家/店铺服务
│   ├── service-platform/           # 运营平台服务
│   ├── service-file/               # 文件服务
│   └── service-notify/             # 通知服务
├── super-market-gateway/           # Spring Cloud Gateway
├── super-market-k8s/               # K8s 部署配置
│   ├── base/                       # 基础设施 (中间件 StatefulSet)
│   ├── apps/                       # 业务服务 (Deployment)
│   └── monitoring/                 # 监控组件
├── pom.xml                         # Maven 父 POM
└── README.md
```

---

> **下一步**: 方案评审通过后，进入 Phase 1 — 基础设施搭建与项目脚手架初始化。
