# 前端联调 Bug 修复方案评审

> 整理时间：2026-05-16
> 范围：app-admin / app-b2b / app-b2c + packages/api

---

## 一、后端 API 不匹配（Critical）

### 1.1 `getMerchantOrderList` 调用了不存在的后端端点

- **文件**: `frontend/packages/api/src/modules/order.ts`
- **问题**: `GET /order/list/shop/{shopId}` — 后端 OrderController 没有这个端点
- **后端实际端点**: `GET /order/list/user/{userId}`（按用户查）、`GET /order/admin/list`（管理员查全部）
- **修复**: 暂时改为调用 `/order/list/user/{userId}`，等后端补充 `/order/list/shop/{shopId}` 后再改回
- **影响页面**: B2B OrdersPage、B2B DashboardPage

### 1.2 B2B OrdersPage 写死 shopId=1

- **文件**: `frontend/app-b2b/src/pages/orders/OrdersPage.vue:151`
- **原代码**: `getMerchantOrderList(1, params)`
- **修复**: 从 localStorage 读取 userId，传入 API

### 1.3 B2B ListPage 上下架请求格式错误

- **文件**: `frontend/app-b2b/src/pages/products/ListPage.vue:154`
- **原代码**: `request.put('/product/spu/${id}/status', { status: newStatus })`
  - 路径错误：后端是 `/shelf` 不是 `/status`
  - 参数方式错误：后端用 `@RequestParam` 接收，不是 `@RequestBody`
- **后端实际端点**: `PUT /api/product/spu/{spuId}/shelf? shelfStatus=1`
- **修复**: 改为 `request.put('/product/spu/${id}/shelf', null, { params: { shelfStatus: newStatus } })`

### 1.4 B2B ShopPage 调用不存在的 `/shop/info`

- **文件**: `frontend/app-b2b/src/pages/shop/ShopPage.vue:118`
- **后端实际端点**: `GET /api/shop/merchant/{merchantId}` — 按商家ID查店铺
- **修复**: 改用 `getShopByMerchantId(userId)` 从 localStorage 获取 userId 作为 merchantId
- **注意**: 店铺保存（`PUT /shop`）后端暂无对应端点，保存功能暂时无法生效

---

## 二、字段名 / 参数类型不一致

### 2.1 B2B DashboardPage 订单状态字段名

- **文件**: `frontend/app-b2b/src/pages/dashboard/DashboardPage.vue:28`
- **原代码**: `row.status`
- **后端返回字段**: `orderStatus`
- **修复**: 改为 `row.orderStatus`

### 2.2 B2B SettlementPage 用了错误的 localStorage key

- **文件**: `frontend/app-b2b/src/pages/settlement/SettlementPage.vue:298,336`
- **原代码**: `localStorage.getItem('user_id')`
- **实际 key**: `'userId'`（定义在 `USER_ID_KEY` 常量）
- **修复**: 改为 `localStorage.getItem(USER_ID_KEY)`

### 2.3 B2C OrdersPage 状态标签与 ORDER_STATUS 常量不一致

- **文件**: `frontend/app-b2c/src/pages/user/OrdersPage.vue:40-47`
- **原标签**: 全部 / 待付款 / **已付款** / **已发货** / 已完成 / 已取消
- **ORDER_STATUS 常量**: 1=待付款, 2=**待发货**, 3=**待收货**, 4=已完成, 5=已取消, 6=已退款
- **修复**: 直接从 ORDER_STATUS 常量生成标签列表，增加"已退款"筛选项

### 2.4 B2C SearchPage query 参数类型

- **文件**: `frontend/app-b2c/src/pages/search/SearchPage.vue:76-81`
- **问题**: `route.query` 所有值都是字符串，直接展开传给 API，后端期望 `page`、`categoryId` 等为数字
- **修复**: 显式提取参数并做 `Number()` 转换

---

## 三、缺失 Import / 组件使用错误

### 3.1 B2B SeckillPage 缺少 ElMessage / ElMessageBox 导入

- **文件**: `frontend/app-b2b/src/pages/seckill/SeckillPage.vue`
- **修复**: 添加 `import { ElMessage, ElMessageBox } from 'element-plus'`

### 3.2 B2C CouponPage 用 OrderStatusTag 显示优惠券状态

- **文件**: `frontend/app-b2c/src/pages/user/CouponPage.vue`
- **问题**: 优惠券状态（未使用/已使用/已过期）语义与订单状态完全不同
- **修复**: 替换为 `el-tag` + 优惠券专用 status 映射函数

### 3.3 B2C Router 导入了未使用的 ROLE_USER

- **文件**: `frontend/app-b2c/src/router/index.ts`
- **修复**: 移除未使用的导入

---

## 四、逻辑缺陷

### 4.1 B2C SeckillPage 秒杀失败未正确处理

- **文件**: `frontend/app-b2c/src/pages/seckill/SeckillPage.vue:41-43`
- **问题**: `executeSeckill` 在后端返回非 200 时，axios 拦截器会 reject Promise，`res.success` 分支永远走不到
- **修复**: 添加 try/catch，失败时由拦截器统一处理错误提示

### 4.2 B2B ListPage 编辑按钮跳转不存在的路由

- **文件**: `frontend/app-b2b/src/pages/products/ListPage.vue:142`
- **原代码**: `router.push('/merchant/products/${row.spuId}/edit')`
- **路由表**: 没有 `/merchant/products/:id/edit` 路由
- **修复**: 改为 `router.push({ name: 'productCreate', query: { spuId: row.spuId } })`

---

## 五、待后端配合的问题

以下问题前端无法独立修复，需后端新增或调整接口：

| 问题 | 说明 | 建议 |
|---|---|---|
| 商家订单列表 | 无 `/order/list/shop/{shopId}` | 新增端点，按 shopId 分页查询该店铺收到的订单 |
| 店铺信息更新 | 无 `PUT /shop` 端点 | 新增端点，支持更新店铺名称、Logo、联系方式等 |
| 商品编辑 | 无 `GET /product/spu/{id}` 的编辑回填 + `PUT` 更新端点 | CreatePage 目前只能创建，无法编辑已有商品 |

---

## 六、变更文件清单

```
frontend/packages/api/src/modules/order.ts          # getMerchantOrderList 端点修正
frontend/app-b2b/src/pages/dashboard/DashboardPage.vue  # row.status → row.orderStatus
frontend/app-b2b/src/pages/orders/OrdersPage.vue        # 去掉写死的 shopId=1
frontend/app-b2b/src/pages/products/ListPage.vue        # shelf 端点 + 编辑按钮路由
frontend/app-b2b/src/pages/seckill/SeckillPage.vue      # 补充 import
frontend/app-b2b/src/pages/settlement/SettlementPage.vue # user_id → userId
frontend/app-b2b/src/pages/shop/ShopPage.vue            # /shop/info → getShopByMerchantId
frontend/app-b2c/src/pages/search/SearchPage.vue        # query 参数类型转换
frontend/app-b2c/src/pages/seckill/SeckillPage.vue      # try/catch
frontend/app-b2c/src/pages/user/CouponPage.vue          # OrderStatusTag → el-tag
frontend/app-b2c/src/pages/user/OrdersPage.vue          # 标签对齐 ORDER_STATUS
frontend/app-b2c/src/router/index.ts                    # 移除未用 import
```

---

## 七、后端配置修复（新增）

### 7.1 Nacos Discovery 不一致（网关 lb:// 路由失效）

**问题**: 8 个服务在 `bootstrap.yml` 中设置 `spring.cloud.nacos.discovery.enabled: false`：
address、auth、category、inventory、member、product、shop、user

**影响**: 网关所有 18 条路由都使用 `lb://service-name`，依赖 Spring Cloud Nacos Discovery 解析。
discovery 禁用后，Spring Cloud LoadBalancer 无法找到这些服务实例，网关路由直接 503。

**修复**: 全部 18 个服务统一设置 `discovery.enabled: true`。

### 7.2 Bootstrap.yml 缺少 spring.config.import

**问题**: 10 个服务的 `bootstrap.yml` 缺少 `spring.config.import: optional:nacos:...`：
cart、coupon、file、notify、order、payment、platform、review、search、seckill

**修复**: 全部 18 个服务统一添加 `spring.config.import`。

### 7.3 Dubbo config-center 缺失

**问题**: 9 个服务的 `application-dev.yml` 没有 `dubbo.config-center` 配置块：
cart、coupon、file、notify、payment、platform、review、search、seckill
order 服务有 config-center 但缺少 `highest-priority: false`

**修复**: 全部补充 `dubbo.config-center` 配置块，包含 `highest-priority: false`。

### 7.4 MyBatis-Plus type-aliases-package 复制粘贴错误

**问题**: 9 个服务错误地指向 `com.supermarket.user.entity`，应该是各自服务的 entity 包：

| 服务 | 错误值 | 正确值 |
|---|---|---|
| cart | user.entity | cart.entity |
| coupon | user.entity | coupon.entity |
| file | user.entity | file.entity |
| notify | user.entity | notify.entity |
| payment | user.entity | payment.entity |
| platform | user.entity | platform.entity |
| review | user.entity | review.entity |
| search | user.entity | search.entity |
| seckill | user.entity | seckill.entity |

**修复**: 全部改为正确的 entity 包路径。

### 7.5 Gateway 端口硬编码

**问题**: `super-market-gateway/application.yml` 中 `server.port: 8999` 硬编码，其他服务都使用 `${SERVER_PORT:xxxx}` 变量。

**修复**: 改为 `${SERVER_PORT:8999}`。

### 7.6 后端配置变更文件清单

```
super-market-gateway/src/main/resources/application.yml                        # port 8999 → ${SERVER_PORT:8999}

# bootstrap.yml: discovery.enabled: false → true (8 files)
super-market-services/service-address/src/main/resources/bootstrap.yml
super-market-services/service-auth/src/main/resources/bootstrap.yml
super-market-services/service-category/src/main/resources/bootstrap.yml
super-market-services/service-inventory/src/main/resources/bootstrap.yml
super-market-services/service-member/src/main/resources/bootstrap.yml
super-market-services/service-product/src/main/resources/bootstrap.yml
super-market-services/service-shop/src/main/resources/bootstrap.yml
super-market-services/service-user/src/main/resources/bootstrap.yml

# bootstrap.yml: 添加 spring.config.import (10 files)
super-market-services/service-cart/src/main/resources/bootstrap.yml
super-market-services/service-coupon/src/main/resources/bootstrap.yml
super-market-services/service-file/src/main/resources/bootstrap.yml
super-market-services/service-notify/src/main/resources/bootstrap.yml
super-market-services/service-order/src/main/resources/bootstrap.yml
super-market-services/service-payment/src/main/resources/bootstrap.yml
super-market-services/service-platform/src/main/resources/bootstrap.yml
super-market-services/service-review/src/main/resources/bootstrap.yml
super-market-services/service-search/src/main/resources/bootstrap.yml
super-market-services/service-seckill/src/main/resources/bootstrap.yml

# application-dev.yml: 添加 dubbo.config-center (9 + 1 order 补充 highest-priority)
# application-dev.yml: 修复 type-aliases-package (9 files)
super-market-services/service-cart/src/main/resources/application-dev.yml
super-market-services/service-coupon/src/main/resources/application-dev.yml
super-market-services/service-file/src/main/resources/application-dev.yml
super-market-services/service-notify/src/main/resources/application-dev.yml
super-market-services/service-order/src/main/resources/application-dev.yml
super-market-services/service-payment/src/main/resources/application-dev.yml
super-market-services/service-platform/src/main/resources/application-dev.yml
super-market-services/service-review/src/main/resources/application-dev.yml
super-market-services/service-search/src/main/resources/application-dev.yml
super-market-services/service-seckill/src/main/resources/application-dev.yml
```
