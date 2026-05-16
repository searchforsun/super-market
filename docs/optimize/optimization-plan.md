# 超级市场电商平台 — 功能优化方案

> 基于 2026-05-16 全栈评测 | 当前评分 6.5/10

---

## 一、优先修复（P0 · 阻断生产部署）

### 1.1 RBAC 权限控制（安全阻断）

**现状：** JWT 角色（ROLE_ADMIN / ROLE_MERCHANT / ROLE_USER）在 AuthService 中生成并写入 Token，Gateway 下发到 `X-User-Roles` 请求头，但所有服务端 Controller 均未读取或校验该头部。任何登录用户都能调用管理员接口。

**方案：**
- 在 `super-market-gateway` 中新增 `RoleBasedFilter`（`Ordered = -90`，在 AuthGlobalFilter 之后执行），按路径前缀匹配角色：
  - `/api/platform/admin/**` → ROLE_ADMIN
  - `/api/*/admin/**` → ROLE_ADMIN 或 ROLE_MERCHANT
  - `/api/shop/merchant/**` → ROLE_MERCHANT
  - 其余 → ROLE_USER 或以上
- 同时在 `common-web` 中新增 `RoleInterceptor`，通过 `HandlerInterceptor` 读取 `X-User-Roles` 做二次校验（网关校验 + 服务端校验双层防护）

**影响范围：** gateway + common-web（各服务自动生效）

---

### 1.2 输入验证补齐

**现状：** 18 个 Controller 中仅 1 个（ProductController）使用了 `@Valid` / `@NotBlank`。登录接口 `phone`/`password`、支付接口 `orderNo`/`amount` 均无校验。

**方案：**
- 对全部 Controller 的 `@RequestParam` / `@RequestBody` 加 `@Valid` / `@NotBlank` / `@Pattern` / `@Min` / `@Max` 注解
- 逐服务执行：`AuthController` → `UserController` → `PaymentController` → `OrderController` → `CartController` → ... 依次完成
- `common-web/GlobalExceptionHandler` 已支持 `MethodArgumentNotValidException` 和 `ConstraintViolationException`，只需补齐后者即可

**影响范围：** 18 个 service 的 Controller + DTO 类

---

### 1.3 密钥脱敏

**现状：** `application-dev.yml` 中 `root123`/`redis123` 明文硬编码并提交仓库。JWT secret 在 `JwtProperties.java` 中有默认明文值。K8s `secret.yaml` 含所有密码的 base64（已提交）。

**方案：**
- 将 `.env.example` 中的密码改为占位符 `change_me`，`.env` 加入 `.gitignore`
- `application-dev.yml` 中密码改读 env：`${MYSQL_PASSWORD:}`（无默认值，启动时强制传）
- 删除 `JwtProperties.java` 中的默认 secret，改为启动时必填
- `secret.yaml` 从仓库移除，新增 `secret.yaml.example` 含占位符
- 不影响现有开发——开发者只需确保 `.env` 文件存在（已存在则无感切换）

**影响范围：** 18 个 application-dev.yml + JwtProperties + .gitignore + 1 个 K8s 文件

---

### 1.4 Service-Payment 实现 PaymentDubboService

**现状：** `PaymentDubboService` 接口在 `common-dubbo-api` 中定义了 3 个方法，但 `PaymentServiceImpl` 未实现该接口。其他服务无法通过 Dubbo 调用支付服务。

**方案：**
```java
// PaymentServiceImpl 加 @DubboService
@DubboService(interfaceClass = PaymentDubboService.class)
public class PaymentServiceImpl implements PaymentService, PaymentDubboService {
    // 三个方法已有实现，只需接口对接
}
```

**影响范围：** service-payment/PaymentServiceImpl.java（单文件改动）

---

## 二、高优补全（P1 · 功能严重缺失）

### 2.1 B2C 前端页面补全（约 ~1500 行新增）

| 页面 | 当前行数 | 目标行数 | 要补的内容 |
|--------|-----|-----|-----|
| `/order/:no` | 33 | ~250 | 订单商品明细、物流追踪时间线、支付倒计时、取消/确认收货按钮 |
| `/user/orders` | 89 | ~220 | 状态筛选 Tab（全部/待付款/待发货/待收货/已完成）、分页、操作按钮 |
| `/user/center` | 49 | ~180 | 用户信息编辑表单、会员等级+积分展示、快捷入口 |
| `/user/addresses` | 34 | ~220 | 新增/编辑/删除对话框、设为默认地址按钮 |
| `/user/coupons` | 18 | ~200 | 领取优惠券面板、状态筛选（可用/已用/已过期）、去使用按钮 |
| `/user/reviews` | 18 | ~180 | 评价表单、追评、删除 |
| `/user/notifications` | 19 | ~120 | 分页、标记已读、详情弹窗 |
| `/seckill` | 51 | ~200 | 场次选择器、商品卡片、倒计时抢购 |
| `/order/result` | 11 | ~80 | 支付状态查询、返回首页/查看订单按钮 |

---

### 2.2 服务端缺失接口补齐

| 服务 | 缺失项 | 方案 |
|------|------|------|
| service-order | `listByShop()` 无 HTTP | 新增 `GET /api/order/list/shop/{shopId}` |
| service-order | `shopId=1L` 硬编码 | 从 `CreateOrderRequest` 中获取 shopId（购物车商品自带 shopId） |
| service-member | `deductPoints` 无 HTTP | 新增 `POST /api/member/points/deduct` |
| service-file | `download` 无 HTTP | 新增 `GET /api/file/{id}/download`，返回字节流 |
| service-notify | `send`/`sendDirect` 无 HTTP | 新增 `POST /api/notify/send`（内部调用，可限制来源） |
| service-seckill | 无 RocketMQ 订单创建消费者 | 新增 `SeckillOrderConsumer`，消费秒杀成功后消息并调用 OrderDubboService 创建订单 |
| service-product | 无更新 SPU/SKU 端点 | 新增 `PUT /api/product/spu/{spuId}` 和 `PUT /api/product/sku/{skuId}` |

---

### 2.3 统一错误码体系

**现状：** 错误码以魔数（400/404/429/500）散落在各服务。

**方案：**
- 在 `common-core` 中新增 `enum ResultCode implements IResultCode`，按域分段：
  ```
  USER_      10001~10099
  PRODUCT_   20001~20099
  ORDER_     30001~30099
  PAYMENT_   40001~40099
  MARKETING_ 50001~50099
  SHOP_      60001~60099
  SYSTEM_    90001~90099
  ```
- `BizException` 接受 `ResultCode` 代替裸 `int code`
- 不影响现有代码——保留 `BizException(int code, String msg)` 构造器兼容

---

## 三、中优补全（P2 · 生产就绪必须）

### 3.1 可观测性接入

| 项目 | 方案 |
|------|------|
| **Actuator** | 所有 18 个服务 pom.xml 加 `spring-boot-starter-actuator`，application.yml 暴露 `health,info,metrics`。网关已有。 |
| **SkyWalking 追踪** | 各服务 Dockerfile 加 `-javaagent:skywalking-agent.jar` + `SW_AGENT_NAME` / `SW_AGENT_COLLECTOR_BACKEND_SERVICES` 环境变量 |
| **Micrometer/Prometheus** | 所有服务加 `micrometer-registry-prometheus` 依赖；prometheus.yml 取消注释并配置服务发现 |
| **Grafana 仪表盘** | `grafana/provisioning/dashboards/` 下新增 4 块预置仪表盘：JVM 概览 / Dubbo 指标 / 业务 QPS / 错误率 TOP10 |

---

### 3.2 K8s 生产清单补齐

| 缺失资源 | 用途 |
|---------|------|
| `apps/service-*.yaml` | 各服务专属 Service（ClusterIP），提供集群内 DNS |
| `base/ingress.yaml` | Nginx Ingress → Gateway（暴露唯一入口） |
| `apps/hpa-*.yaml` | 按 CPU/Memory 的 HPA（min 2, max 10） |
| `statefulset/mysql.yaml` | MySQL 主从 StatefulSet + Headless Service + PVC |
| `statefulset/redis.yaml` | Redis 哨兵 StatefulSet |
| `base/network-policy.yaml` | 最小权限网络策略（仅 Gateway 可被 Ingress 访问，服务间用 Dubbo 端口互通） |

---

### 3.3 前端状态管理 & 测试

| 项目 | 方案 |
|------|------|
| **新增 Pinia Store** | `useOrderStore`、`useAddressStore`、`useCouponStore`、`useNotifyStore` |
| **前端测试** | `packages/utils/` 和 `packages/stores/` 加 Vitest 单元测试；`app-b2c/src/pages/` 核心页面（首页/商品详情/购物车）加 `@vue/test-utils` 组件测试 |
| **API 模块补齐** | `product.ts` 补充 `createProduct`/`updateStatus` 封装 |

---

### 3.4 CI/CD 优化

| 项目 | 方案 |
|------|------|
| **并行构建** | `.gitlab-ci.yml` 中 `build` 阶段改用 `parallel:matrix` 替代串行 `for` 循环 |
| **层缓存** | 各 Dockerfile 加 `RUN --mount=type=cache` 或 GitLab CI `cache` 配置 |
| **安全扫描** | 新增 `security` 阶段：Trivy 镜像扫描 + Maven dependency-check |
| **优雅关闭** | 全部 20 个 Dockerfile 追加 `-Dspring.lifecycle.timeout-per-shutdown-phase=30s` |

---

## 四、长期优化（P3 · 按需推进）

| 编号 | 项目 | PRD 优先级 | 说明 |
|------|------|----------|------|
| 4.1 | 真实支付网关对接（支付宝/微信） | P0 | 当前仅 mock 回调，需对接 SDK |
| 4.2 | 积分体系完整链路（签到→积分→兑换） | P1 | 消费返积分已存在，签到和兑换未实现 |
| 4.3 | 满减活动（阶梯满减+优惠叠加规则） | P1 | 当前仅优惠券，无满减促销 |
| 4.4 | Seckill 预热优化（多级缓存+令牌桶） | P1 | 基础 Lua 脚本已完成，需加缓存预热调度 |
| 4.5 | 搜索高级功能（聚合/分面/拼写纠正） | P1 | ES 基础搜索已通，高级查询未做 |
| 4.6 | ShardingSphere 实际接入 | P2 | 依赖声明但未配置分片规则 |
| 4.7 | OAuth 2.0 第三方登录（微信/QQ） | P2 | 未启动 |
| 4.8 | 商品个性化推荐引擎 | P2 | 未启动 |
| 4.9 | 商家对账结算完整流程 | P1 | 仅前端页面，后端逻辑基本为空 |
| 4.10 | 实名认证（身份证+人脸） | P1 | 数据库字段已有，逻辑完全未写 |

---

## 五、实施路线图

```
第 1 周（安全底线）
├── RBAC 角色拦截（gateway + common-web）
├── 密钥脱敏（18 个 yml + JwtProperties + K8s secret）
├── 输入校验（auth → user → payment → order → ... 逐个 Controller）
└── PaymentDubboService 修复

第 2 周（功能堵漏）
├── 服务端 7 项缺失接口补齐
├── 统一错误码枚举
├── service-order shopId 硬编码修复
└── service-seckill 消费者实现

第 3~4 周（前端补全）
├── B2C 9 个页面补全（按上表顺序）
├── 4 个 Pinia Store 新增
├── 前端 Vitest 测试
└── API 模块补齐

第 5~6 周（生产就绪）
├── Actuator + SkyWalking + Micrometer 接入
├── K8s 清单补齐（Service/Ingress/HPA/PVC/NetworkPolicy）
├── CI/CD 并行化 + 安全扫描 + 优雅关闭
├── Grafana 仪表盘预置
└── 集成测试补充（网关 → 下单 → 支付 → 回调 全链路）
```

---

## 六、工作量估算

| 阶段 | 内容 | 预估人天 |
|------|------|---------|
| P0 · 安全底线 | RBAC + 密钥 + 校验 + PaymentDubbo | 5~7 天 |
| P1 · 功能堵漏 | 7 项接口 + 错误码 + 硬编码修复 + 消费者 | 5~7 天 |
| P1 · 前端补全 | 9 页面 + 4 Store + 测试 | 8~12 天 |
| P2 · 生产就绪 | 观测性 + K8s + CI/CD + Grafana | 8~12 天 |
| P3 · 长期迭代 | 支付/积分/满减/搜索/推荐/ShardingSphere | 按需分配 |
| **合计（P0~P2）** | | **26~38 人天** |
