# 超级市场电商平台 — 功能优化设计文档

> 基于 2026-05-16 全栈评测 | 当前评分 6.5/10 | 目标评分 8.5/10

---

## 一、P0 · 安全底线（阻断生产部署）

### 1.1 RBAC 权限控制

**问题：** JWT 角色已生成并下发至 `X-User-Roles` 请求头，但 Gateway 和服务端均未校验。任意登录用户可调用管理员接口。

**方案：双层防护**

```
请求 → Gateway AuthGlobalFilter(-100) → Gateway RoleBasedFilter(-90) → 后端 RoleInterceptor → Controller
```

**Gateway 层 — RoleBasedFilter（Order = -90）**

- 新建 `super-market-gateway/src/main/java/.../filter/RoleBasedFilter.java`
- 路径 → 角色映射表：
  ```
  /api/platform/admin/**  →  ROLE_ADMIN
  /api/*/admin/**          →  ROLE_ADMIN 或 ROLE_MERCHANT
  /api/shop/merchant/**    →  ROLE_MERCHANT
  其余                      →  ROLE_USER 及以上
  ```
- 从 `X-User-Roles` 读取角色列表，匹配失败返回 403
- 不对 whitelist 路径生效（复用 AuthGlobalFilter 的 whitelist 判断逻辑）

**服务端层 — RoleInterceptor（可选精细控制）**

- 新建 `common-web` 中 `@RequireRole` 注解 + `RoleInterceptor`（`HandlerInterceptor`）
- 敏感接口显式声明：审核商品 `@RequireRole(ROLE_ADMIN)`，发货 `@RequireRole(ROLE_MERCHANT)`
- 普通接口不需要注解，由 Gateway 兜底
- 拦截器读 `X-User-Roles`，不匹配抛 `BizException(403, "权限不足")`

**文件清单：**

| 文件 | 操作 | 位置 |
|------|------|------|
| `RoleBasedFilter.java` | 新增 | `super-market-gateway/src/main/java/.../filter/` |
| `RequireRole.java` | 新增 | `super-market-common/common-web/src/main/java/.../annotation/` |
| `RoleInterceptor.java` | 新增 | `super-market-common/common-web/src/main/java/.../interceptor/` |
| `WebMvcConfig.java` | 修改 | `super-market-common/common-web/`（注册拦截器） |

---

### 1.2 输入验证补齐

**问题：** 18 个 Controller 中仅 1 个使用 `@Valid`/`@NotBlank`。登录、支付等核心接口零校验。

**方案：混合策略**

- 复杂 `@RequestBody` → 建 DTO 类 + `@Valid` + Jakarta 约束注解（`@NotBlank`/`@Pattern`/`@Min`/`@Max`）
- 简单参数（≤2 个 `@RequestParam`）→ 直接在参数上加约束注解
- `GlobalExceptionHandler` 补齐 `ConstraintViolationException` 处理

**实施顺序：**

| 批次 | 接口 | 优先级 |
|------|------|--------|
| 1 | AuthController.login/refresh、UserController.register/login | 高 |
| 2 | PaymentController.pay/refund、OrderController.create/cancel | 高 |
| 3 | ProductController 审核接口、CouponController admin 接口、ShopController audit 接口 | 中 |
| 4 | 其余 Controller | 低 |

**验证注解标准：**

| 字段类型 | 约束 |
|---------|------|
| phone | `@NotBlank` + `@Pattern(regexp = "^1[3-9]\\d{9}$")` |
| password | `@NotBlank` + `@Size(min = 6, max = 32)` |
| orderNo / payNo | `@NotBlank` |
| amount | `@NotNull` + `@DecimalMin("0.01")` |
| reason | `@Size(max = 500)` |

---

### 1.3 密钥脱敏

**问题：** 密码（`root123`/`redis123`）明文硬编码在 18 个 `application-dev.yml` 中并提交仓库。JWT secret 有默认值。K8s `secret.yaml` 含所有密码已提交。

**方案：全环境变量注入，无默认值**

**改动 1：application-dev.yml（18 个文件）**

```yaml
# 改前
datasource:
  password: root123
redis:
  password: redis123

# 改后
datasource:
  password: ${MYSQL_PASSWORD:}
redis:
  password: ${REDIS_PASSWORD:}
```

`${VAR:}` 为空默认值——环境变量未设时启动报错，确保不会意外使用空密码。

**改动 2：JwtProperties.java**

删除 `private String secret = "super-market-default-secret-key-..."` 默认值。启动时若 `jwt.secret` 未配置则抛 `IllegalStateException`。

**改动 3：K8s secret.yaml**

- 从仓库删除 `super-market-k8s/base/secret.yaml`
- 新增 `super-market-k8s/base/secret.yaml.example` 含占位符 `change_me`
- `.gitignore` 加 `secret.yaml`

**改动 4：.env 文件**

- `docker-compose/.env` 已在 `.gitignore` 中，开发者已有本地副本，无需改动
- `docker-compose/.env.example` 中的密码占位符改为 `change_me`

**文件清单：**

| 文件 | 操作 |
|------|------|
| 18 个 `application-dev.yml` | 修改密码行 |
| `JwtProperties.java` | 删除默认 secret |
| `secret.yaml` | 删除 |
| `secret.yaml.example` | 新增 |
| `.gitignore` | 追加 secret.yaml |

---

### 1.4 PaymentDubboService 修复

**问题：** `PaymentDubboService` 接口在 common-dubbo-api 定义，`PaymentServiceImpl` 未实现。支付服务无法通过 Dubbo RPC 调用。

**方案：单文件改动**

```java
// PaymentServiceImpl.java 改前
@Service
public class PaymentServiceImpl implements PaymentService {

// 改后
@DubboService(interfaceClass = PaymentDubboService.class)
@Service
public class PaymentServiceImpl implements PaymentService, PaymentDubboService {
```

三个方法（`createPayment`/`handleCallback`/`getByPayNo`）已有实现，仅需接口声明。

**文件清单：**

| 文件 | 操作 |
|------|------|
| `service-payment/.../impl/PaymentServiceImpl.java` | 改 2 行 |

---

## 二、P1 · 功能堵漏

### 2.1 服务端缺失接口补齐

**A 组：阻塞业务流程**

| # | 服务 | 缺失 | 新增端点 | 关键实现细节 |
|---|------|------|---------|------------|
| 1 | service-order | `listByShop()` 无 HTTP | `GET /api/order/list/shop/{shopId}` | 分页查询，按时间倒序 |
| 2 | service-order | `shopId=1L` 硬编码 | 无需新增端点 | `CreateOrderRequest` 中携带 shopId（购物车商品按店铺分组后分别创建子订单，shopId 来自商品所属店铺） |
| 3 | service-seckill | 无订单创建消费者 | 无 HTTP 端点（内部消费者） | 新增 `SeckillOrderConsumer`，消费秒杀成功 Topic 消息，组装 `CreateOrderRequest` 调用 `OrderDubboService.createOrder()` |

**B 组：功能不完整**

| # | 服务 | 新增端点 | 说明 |
|---|------|---------|------|
| 4 | service-member | `POST /api/member/points/deduct` | params: userId, points, reason |
| 5 | service-file | `GET /api/file/{id}/download` | 返回 `ResponseEntity<byte[]>`，设置 Content-Disposition |
| 6 | service-notify | `POST /api/notify/send` | 来源校验：检查 `X-Source-Service` 头部 |
| 7 | service-product | `PUT /api/product/spu/{spuId}` + `PUT /api/product/sku/{skuId}` | 更新商品基本信息 + SKU 价格/库存/状态 |

**文件清单（A 组）：**

| 文件 | 操作 |
|------|------|
| `OrderController.java` | 新增 1 个端点 |
| `CreateOrderRequest.java` | 确认 shopId 字段可透传 |
| `OrderServiceImpl.java` | 移除 shopId=1L 硬编码 |
| `SeckillOrderConsumer.java` | 新增 |

**文件清单（B 组）：**

| 文件 | 操作 |
|------|------|
| `MemberController.java` | 新增 1 个端点 |
| `FileController.java` | 新增 1 个端点 |
| `NotifyController.java` | 新增 1 个端点 |
| `ProductController.java` + `ProductService.java` | 新增 2 个端点 |

---

### 2.2 统一错误码体系

**方案：枚举 + 接口（IResultCode），按业务域分段**

```
enum ResultCode implements IResultCode:
  USER      10001~10099   用户/认证/会员/地址
  PRODUCT   20001~20099   商品/类目/库存/评价
  ORDER     30001~30099   订单/购物车/支付
  MARKETING 40001~40099   优惠券/秒杀
  SHOP      50001~50099   商家/店铺
  PLATFORM  60001~60099   平台/风控
  FILE      70001~70099   文件/通知
  SYSTEM    90001~90099   系统通用
```

**接口定义：**

```java
public interface IResultCode {
    int getCode();
    String getMessage();
}
```

**BizException 变更：**

```java
// 保留已有构造器兼容
public BizException(int code, String message) { ... }

// 新增枚举构造器
public BizException(ResultCode rc) { super(rc.getCode(), rc.getMessage()); }
```

**渐进迁移策略：** 先建枚举定义全局错误码，存量代码不强制改写。新增代码使用枚举。核心链路（登录/下单/支付）优先迁移。

**文件清单：**

| 文件 | 操作 |
|------|------|
| `common-core/.../IResultCode.java` | 新增 |
| `common-core/.../ResultCode.java` | 新增 |
| `common-core/.../BizException.java` | 修改（新增构造器） |

---

### 2.3 B2C 前端页面补全

**页面补全顺序与内容：**

| 排序 | 路由 | 目标行数 | 核心功能 |
|------|------|---------|---------|
| 1 | `/user/addresses` | ~220 | 新增/编辑/删除地址对话框，设为默认按钮，收货人/电话/省市区/详细地址表单 |
| 2 | `/order/:no` | ~250 | 订单商品明细、物流追踪时间线、支付倒计时组件、取消/确认收货操作按钮 |
| 3 | `/user/orders` | ~220 | 状态筛选 Tab（全部/待付款/待发货/待收货/已完成）、分页、查看详情/取消/确认收货 |
| 4 | `/order/result` | ~80 | 支付状态轮询、成功/失败提示、查看订单/返回首页按钮 |
| 5 | `/user/coupons` | ~200 | 领券面板（可用券列表+一键领取）、券列表（可用/已用/已过期 Tab）、去使用跳转 |
| 6 | `/user/center` | ~180 | 用户信息编辑表单（头像/昵称）、会员等级卡片+积分进度条、快捷入口 |
| 7 | `/seckill` | ~200 | 场次选择 Tab、商品卡片（倒计时+抢购按钮）、已抢进度条 |
| 8 | `/user/reviews` | ~180 | 评价列表（星级+图文）、新建评价表单（评分+图片+文字）、追评 |
| 9 | `/user/notifications` | ~120 | 分页列表、点击标记已读/全部已读、通知详情弹窗 |

**新增 Pinia Store（4 个）：**

| Store | 管理状态 | 使用页面 |
|-------|---------|---------|
| `useAddressStore` | 地址列表、默认地址 CRUD | addresses / order-confirm |
| `useOrderStore` | 订单列表、当前订单详情 | orders / order/:no / order/result |
| `useCouponStore` | 可用券、我的券、领券 | coupons / cart / order-confirm |
| `useNotifyStore` | 通知列表、未读数 | notifications / Default.vue header |

**前端测试补齐：**

| 范围 | 工具 | 目标 |
|------|------|------|
| `packages/utils/` | Vitest | formatPrice/formatDate/SSO 函数单元测试 |
| `packages/stores/` | Vitest + pinia-testing | Store 逻辑测试 |
| `packages/ui/` | Vitest + @vue/test-utils | 10 个组件渲染测试 |
| `app-b2c` 核心页面 | @vue/test-utils | 首页/商品详情/购物车渲染测试 |

---

## 三、P2 · 生产就绪

### 3.1 可观测性接入

**Actuator：** 18 个服务 pom.xml 加 `spring-boot-starter-actuator`，application.yml 暴露 `health,info,metrics`。Gateway 已有。

**SkyWalking 追踪：**

| 环境 | 挂载方式 |
|------|---------|
| Docker Compose（开发） | Volume 挂载 `skywalking-agent.jar`，通过 `JAVA_TOOL_OPTIONS` 注入 |
| K8s（生产） | Init Container 下载 agent，共享 Volume |

20 个 Dockerfile 不做改动（不内置 agent jar）。

**Micrometer/Prometheus：**

- 所有服务 pom.xml 加 `micrometer-registry-prometheus`
- `docker-compose/prometheus/prometheus.yml` 取消注释 `spring-boot-apps` scrape 配置
- 按服务名标签区分指标来源

**Grafana 仪表盘：** `grafana/provisioning/dashboards/` 预置 4 块：
1. JVM 概览（堆内存/GC/线程）
2. Dubbo RPC 指标（QPS/延迟/错误率）
3. 业务 QPS（按服务+接口）
4. 错误率 TOP10

---

### 3.2 K8s 生产清单补齐

| 资源 | 数量 | 说明 |
|------|------|------|
| Service (ClusterIP) | 19 个 | 18 服务 + Gateway，提供集群内 DNS |
| Ingress | 1 个 | Nginx Ingress → Gateway Service :8999，唯一入口 |
| HPA | 5 个 | Gateway + user/order/product/payment 核心服务，CPU >70%，min 2 / max 10 |
| StatefulSet | 2 个 | MySQL 主从 + Redis Sentinel，均挂 PVC |
| NetworkPolicy | 1 个 | 最小权限：Ingress→Gateway→服务 Dubbo 端口 |

---

### 3.3 CI/CD 优化

| 项目 | 改动 |
|------|------|
| 并行构建 | `.gitlab-ci.yml` build 阶段改为 `parallel: matrix`（`SERVICE_NAME` 维度） |
| Maven 缓存 | GitLab CI cache 配置 `~/.m2/repository` |
| 安全扫描 | 新增 `security` 阶段：Trivy 镜像扫描 + `maven-dependency-check` |
| 优雅关闭 | 20 个 Dockerfile 追加 `-Dspring.lifecycle.timeout-per-shutdown-phase=30s` |

---

## 四、P3 · 长期迭代

| 编号 | 项目 | PRD 优先级 | 方案概要 |
|------|------|----------|---------|
| 4.1 | 真实支付网关 | P0 | 对接支付宝/微信 SDK，`PayChannel` 接口 + 策略模式，mock 实现改为 AliPayChannel/WechatPayChannel |
| 4.2 | 积分体系 | P1 | 签到（连续签到奖励递增）、兑换（积分换商品/优惠券）、积分过期策略（年度清零） |
| 4.3 | 满减活动 | P1 | `PromotionRule` 引擎：条件（满 X 元/满 X 件）+ 动作（减 Y 元/打 Z 折），叠加策略：互斥/优先级 |
| 4.4 | 搜索高级功能 | P1 | ES 聚合分面（品牌/价格/类目）、拼音搜索、拼写纠正（edit-distance 3） |
| 4.5 | ShardingSphere | P2 | 订单表按 `user_id` 哈希分片 4 库 × 4 表，商品表按 `category_id` |
| 4.6 | OAuth 第三方登录 | P2 | JustAuth 集成微信/QQ，复用 JWT 签发逻辑 |
| 4.7 | 商品推荐 | P2 | 离线协同过滤（Spark MLlib）+ 在线 Redis 缓存推荐结果 |

---

## 五、实施方案

```
第 1 周（P0 安全底线）
├── RBAC 双层防护（gateway + common-web）
├── 密钥脱敏（18 yml + JwtProperties + K8s + .gitignore）
├── 输入校验第 1-2 批（auth → user → payment → order）
└── PaymentDubboService 修复

第 2 周（P1 服务端 + P0 收尾）
├── A 组缺失接口（listByShop / shopId / SeckillOrderConsumer）
├── B 组缺失接口（deductPoints / download / send / product update）
├── 统一错误码枚举
├── 输入校验第 3-4 批
└── 前端测试基础（packages/utils + stores）

第 3~4 周（P1 前端补全）
├── 第 1 批：addresses → order/:no → user/orders → order/result
├── 第 2 批：coupons → center → seckill → reviews → notifications
├── 4 个 Pinia Store
└── UI 组件测试 + B2C 核心页面测试

第 5~6 周（P2 生产就绪）
├── Actuator + SkyWalking + Micrometer + Grafana
├── K8s Service × 19 / Ingress / HPA / StatefulSet / NetworkPolicy
├── CI/CD 并行化 + 安全扫描 + 优雅关闭
└── 全链路集成测试（网关 → 下单 → 支付 → 回调）
```

---

## 六、工作量估算

| 阶段 | 人天 |
|------|------|
| P0 安全底线 | 5~7 |
| P1 服务端堵漏 | 5~7 |
| P1 前端补全 | 8~12 |
| P2 生产就绪 | 8~12 |
| **合计** | **26~38** |
