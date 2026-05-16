# Phase 5 — 支撑服务 + 可观测性 方案设计

> 2026-05-13 | 基于 solution-design.md + PRD v1.0

## 一、总览

### 1.1 服务清单

| 服务 | 中文名称 | 端口 | 数据库 | 核心职责 |
|------|----------|------|--------|----------|
| file-service | 文件服务 | 9371 | db_product | MinIO 上传/下载、元数据管理、缩略图生成 |
| notify-service | 通知服务 | 9372 | db_product | 站内信 + 邮件 + 短信多通道通知、RocketMQ 事件驱动 |
| platform-service | 运营平台服务 | 9381 | db_platform | Banner/广告位管理、首页推荐、刷单风控、数据报表 |

### 1.2 基础设施新增

| 组件 | 新增内容 |
|------|----------|
| **MySQL** | 7 张新表（file_records, notifications, notify_templates, banners, ad_positions, risk_rules, risk_logs） |
| **Docker** | Prometheus :19090 + Grafana :13000 |
| **RocketMQ** | `notify-event-topic` — 通知事件（订单状态变更触发） |
| **MinIO** | 3 个 Bucket: `smt-product`, `smt-avatar`, `smt-document` |
| **Spring Boot** | file-service 添加 MinIO SDK，notify-service 添加 Mail Starter |

---

## 二、Docker Compose — 监控基础设施

```yaml
prometheus:
  image: prom/prometheus:v2.51.2
  container_name: smt-prometheus
  ports: - "19090:9090"
  volumes:
    - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    - prometheus-data:/prometheus

grafana:
  image: grafana/grafana:10.4.2
  container_name: smt-grafana
  environment:
    GF_SECURITY_ADMIN_USER: admin
    GF_SECURITY_ADMIN_PASSWORD: admin123
  ports: - "13000:3000"
  volumes:
    - grafana-data:/var/lib/grafana
    - ./grafana/provisioning:/etc/grafana/provisioning
```

Prometheus 默认配置了 `spring-boot-apps` job（通过 `/actuator/prometheus` 采集指标），Grafana 默认数据源指向 Prometheus。

---

## 三、数据库 DDL

### 3.1 file_records (db_product)

```sql
CREATE TABLE file_records (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name   VARCHAR(255) NOT NULL,
    file_size   BIGINT NOT NULL,
    md5         VARCHAR(32) NOT NULL,
    bucket      VARCHAR(50) NOT NULL,
    object_key  VARCHAR(500) NOT NULL,
    url         VARCHAR(500),
    thumb_url   VARCHAR(500),
    uploader_id BIGINT COMMENT '上传者ID',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uploader (uploader_id),
    INDEX idx_md5 (md5)
);
```

### 3.2 notifications + notify_templates (db_product)

```sql
CREATE TABLE notify_templates (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码(ORDER_CREATED等)',
    name        VARCHAR(100) NOT NULL COMMENT '模板名称',
    channel     TINYINT NOT NULL COMMENT '1站内信 2邮件 3短信',
    title       VARCHAR(200) COMMENT '标题模板',
    content     TEXT NOT NULL COMMENT '内容模板(支持${变量})',
    status      TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL COMMENT '接收用户ID',
    template_id BIGINT COMMENT '模板ID',
    channel     TINYINT NOT NULL COMMENT '1站内信 2邮件 3短信',
    title       VARCHAR(200),
    content     TEXT NOT NULL,
    target      VARCHAR(200) COMMENT '邮件地址/手机号',
    status      TINYINT DEFAULT 0 COMMENT '0未读 1已读',
    send_status TINYINT DEFAULT 0 COMMENT '0待发送 1已发送 2发送失败',
    send_at     DATETIME,
    read_at     DATETIME,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id, status),
    INDEX idx_send (send_status)
);
```

### 3.3 banners + ad_positions (db_platform)

```sql
CREATE TABLE banners (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    link_url    VARCHAR(500),
    sort_order  INT DEFAULT 0,
    position    VARCHAR(50) DEFAULT 'HOME_TOP' COMMENT '展示位置',
    status      TINYINT DEFAULT 1,
    start_time  DATETIME,
    end_time    DATETIME,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pos_status (position, status)
);

CREATE TABLE ad_positions (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    status      TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 3.4 risk_rules + risk_logs (db_platform)

```sql
CREATE TABLE risk_rules (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    type        TINYINT NOT NULL COMMENT '1刷单检测 2恶意退款 3IP黑名单 4频率限制',
    config      JSON NOT NULL COMMENT '规则配置(阈值/条件)',
    status      TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE risk_logs (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id     BIGINT,
    user_id     BIGINT,
    target_id   VARCHAR(100) COMMENT '目标(订单号/IP等)',
    risk_type   TINYINT NOT NULL,
    risk_score  INT DEFAULT 0,
    detail      JSON COMMENT '检测详情',
    action      TINYINT DEFAULT 0 COMMENT '0记录 1告警 2拦截',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_time (created_at)
);
```

---

## 四、file-service — 文件服务

### 4.1 核心接口

```java
// 上传文件
POST /api/file/upload
Request: multipart/form-data { file, bucket? }
Response: { fileId, url, fileName, fileSize, md5 }

// 文件列表
GET /api/file/list?uploaderId=xxx&page=1&size=20

// 删除
DELETE /api/file/{fileId}

// 详情
GET /api/file/{fileId}
```

### 4.2 安全策略

- 类型白名单: jpg/jpeg/png/gif/webp/pdf/doc/docx/xlsx
- 最大 10MB
- 图片类自动生成 200x200 缩略图
- MD5 去重: 相同文件不重复上传

### 4.3 Bucket 规划

| Bucket | 用途 | 公开访问 |
|--------|------|----------|
| `smt-product` | 商品图片 | 是 |
| `smt-avatar` | 用户头像 | 是 |
| `smt-document` | 文档/附件 | 否（通过接口鉴权访问） |

### 4.4 MinIO 依赖

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

### 4.5 关键实现

```java
// MinioConfig — MinioClient Bean
@Bean
public MinioClient minioClient() {
    return MinioClient.builder()
        .endpoint("http://localhost:19000")
        .credentials("minioadmin", "minioadmin123")
        .build();
}

// FileServiceImpl.upload()
String objectKey = bucket + "/" + UUID.randomUUID() + "_" + originalName;
minioClient.putObject(PutObjectArgs.builder()
    .bucket(bucket).object(objectKey)
    .stream(inputStream, fileSize, -1)
    .contentType(contentType).build());
String url = minioEndpoint + "/" + bucket + "/" + objectKey;
```

---

## 五、notify-service — 通知服务

### 5.1 多通道架构

```
事件源（订单/商品/系统）→ RocketMQ notify-event-topic
  → notify-service 消费
    → 查询 notify_templates 匹配模板
    → 渲染内容（变量替换）
    → 多通道分发:
        ├── 站内信: INSERT notifications (user_id, channel=1)
        ├── 邮件: JavaMailSender.send() (channel=2)
        └── 短信: SmsProvider.send() 预留接口 (channel=3)
```

### 5.2 通知模板

| 编码 | 通道 | 触发事件 | 模板内容示例 |
|------|------|----------|-------------|
| ORDER_CREATED | 站内信 | 订单创建 | "您的订单 ${orderNo} 已创建，金额 ${amount} 元" |
| ORDER_PAID | 站内信 | 支付成功 | "订单 ${orderNo} 已支付成功" |
| ORDER_SHIPPED | 站内信+邮件 | 已发货 | "订单 ${orderNo} 已发货，物流单号 ${trackingNo}" |
| ORDER_REFUNDED | 站内信+邮件 | 已退款 | "订单 ${orderNo} 已退款 ${amount} 元" |
| COUPON_EXPIRING | 站内信 | 优惠券即将过期 | "您的优惠券将于 ${expireDate} 过期" |

### 5.3 核心接口

```java
// ===== 用户端 =====
// 我的通知列表
GET /api/notify/list?userId=xxx&page=1&size=20

// 未读数量
GET /api/notify/unread-count?userId=xxx

// 标记已读
PUT /api/notify/{id}/read

// 全部已读
PUT /api/notify/read-all?userId=xxx

// ===== 管理端 =====
// 创建通知模板
POST /api/notify/admin/template
Request: { code, name, channel, title, content }

// ===== 内部 API =====
// 发送通知（事件消费端调用）
POST /api/notify/internal/send
Request: { userId, templateCode, params: {orderNo:..., amount:...}, channel? }

// Dubbo 接口
NotifyDubboService.send(Long userId, String templateCode, Map<String, String> params);
```

### 5.4 RocketMQ 消费者

```java
@RocketMQMessageListener(topic = "order-event-topic", consumerGroup = "notify-consumer-group")
public class OrderEventConsumer implements RocketMQListener<String> {
    // 根据 Tag(ORDER_CREATED/ORDER_PAID/...) 匹配模板并发送通知
}
```

---

## 六、platform-service — 运营平台服务

### 6.1 Banner 管理

```java
// Banner CRUD
POST   /api/platform/admin/banner          // 创建 Banner
PUT    /api/platform/admin/banner/{id}     // 更新
DELETE /api/platform/admin/banner/{id}     // 删除
GET    /api/platform/admin/banner/list     // 管理端列表

// 前台
GET    /api/platform/banners?position=HOME_TOP   // 按位置获取有效 Banner
```

### 6.2 广告位管理

```java
GET    /api/platform/admin/positions       // 广告位列表
POST   /api/platform/admin/positions       // 创建广告位
PUT    /api/platform/admin/positions/{id}  // 更新广告位
```

### 6.3 风控系统

```java
// 风控规则 CRUD
POST   /api/platform/admin/risk/rule
GET    /api/platform/admin/risk/rules
PUT    /api/platform/admin/risk/rule/{id}

// 风控检测（内部 Dubbo 调用）
RiskDubboService.evaluate(RiskRequest) → RiskResult { score, action }

// 风控日志
GET    /api/platform/admin/risk/logs?page=1&size=20&riskType=1
```

### 6.4 风控检测逻辑

```java
// RiskServiceImpl.evaluate()
// 刷单检测: 同一用户 1 小时内下单 > 10 次 → risk_score += 50
// 恶意退款: 同一用户 7 天内退款率 > 50% → risk_score += 70
// IP 黑名单: IP 在黑名单中 → action = BLOCK
// 评分 >= 80: action = ALERT, >= 120: action = BLOCK
```

### 6.5 数据报表

```java
// GMV 统计
GET /api/platform/admin/report/gmv?startDate=xxx&endDate=xxx
Response: { totalGmv, dailyGmv: [{date, amount}...], avgOrderAmount }

// 订单统计
GET /api/platform/admin/report/orders?startDate=xxx&endDate=xxx
Response: { totalOrders, statusBreakdown: {created,paid,completed,cancelled}, dailyOrders: [...] }

// 用户增长
GET /api/platform/admin/report/users?startDate=xxx&endDate=xxx
Response: { totalUsers, newUsers, activeUsers, dailyNewUsers: [...] }

// 热销商品 Top 10
GET /api/platform/admin/report/top-products?limit=10
Response: [ { spuId, spuName, salesCount, totalAmount }... ]
```

报表通过聚合查询业务库（db_order/db_user/db_product）实时计算，简单 COUNT/SUM 直接查 DB，复杂聚合预计算到 Redis 缓存。

---

## 七、Gateway 路由更新

```yaml
- id: file-service
  uri: lb://file-service
  predicates: - Path=/api/file/**
- id: notify-service
  uri: lb://notify-service
  predicates: - Path=/api/notify/**
- id: platform-service
  uri: lb://platform-service
  predicates: - Path=/api/platform/**
```

### 鉴权白名单更新

`/api/platform/banners` 加入白名单（首页 Banner 无需登录）

---

## 八、验收标准

- [ ] `mvn clean install -DskipTests` BUILD SUCCESS（新增 3 个服务）
- [ ] 所有单元测试通过（目标 18+ 测试用例）
- [ ] file-service: 文件上传 MinIO + 元数据入库 + URL 返回可访问
- [ ] file-service: 类型白名单拦截 + 大小限制 + 缩略图生成
- [ ] notify-service: 站内信列表/未读/已读 + RocketMQ 消费订单事件自动通知
- [ ] notify-service: 邮件发送 + 短信预留接口
- [ ] platform-service: Banner/广告位 CRUD + 前台接口
- [ ] platform-service: 风控规则评估 + 日志记录
- [ ] platform-service: GMV/订单/用户报表查询
- [ ] Prometheus :19090 + Grafana :13000 可访问
- [ ] 全量 `mvn test` 回归通过
