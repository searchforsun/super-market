# 业务链路场景清单 & API 调用说明

> 所有场景均基于真实业务流测试代码提取，每个场景包含完整的 API 调用链、请求参数、响应结构和数据传递关系。

## 目录

| 业务域 | 场景数 | API 端点 | 文档 |
|---|---|---|---|
| [用户域](#用户域) | 4 | 8 | [user-flow.md](user-flow.md) |
| [商品域](#商品域) | 8 | 14 | [product-flow.md](product-flow.md) |
| [交易域](#交易域) | 9 | 11 | [order-flow.md](order-flow.md) |
| [营销域](#营销域) | 2 | 10 | [marketing-flow.md](marketing-flow.md) |
| [商家域](#商家域) | 1 | 5 | [shop-flow.md](shop-flow.md) |
| [平台域](#平台域) | 4 | 14 | [platform-flow.md](platform-flow.md) |

---

## 测试账号

| 角色 | 手机号 | 密码 | 说明 |
|---|---|---|---|
| 运营后台管理员 | `13800000000` | `123456` | 平台运营、商家审核、类目管理、活动配置 |
| 普通用户 | `13800000001` ~ `13800000099` | `Pass1234` | C端用户，注册/登录/下单 |

> Gateway 入口: `http://localhost:8999`

---

## 通用约定

### 响应格式

所有 API 返回统一结构：
```json
{
  "code": 0,
  "message": "success",
  "data": { },
  "timestamp": 1715875200000
}
```

| code | 含义 |
|---|---|
| 0 | 成功 |
| 90001 | 系统错误 |
| 100xx | 用户域错误（详见 ResultCode） |
| 200xx | 商品域错误 |
| 300xx | 交易域错误 |
| 400xx | 营销域错误 |
| 500xx | 商家域错误 |
| 600xx | 平台域错误 |
| 700xx | 文件/通知域错误 |

### 分页响应

```json
{
  "code": 0,
  "data": {
    "records": [ ],
    "total": 100,
    "size": 10,
    "current": 1
  }
}
```

### 数据传递（链式调用）

场景中后续步骤使用的数据从前一步响应的 `$.data` 中提取：
- `$.data.id` → 实体 ID
- `$.data.orderNo` → 订单号
- `$.data.payNo` → 支付单号
- `$.data.accessToken` / `$.data.refreshToken` → JWT Token

### 测试环境

- Gateway: `http://localhost:8999`
- 所有路径前缀: `/api/{service}`
- 需要 JWT 认证的接口在 Header 中携带 `Authorization: Bearer {accessToken}`

### 重要 API 行为说明（基于真实链路测试）

**认证要求：** 以下端点需要 JWT Bearer Token（不在 Gateway 白名单中，测试文档中的用法需补充 Authorization Header）：
- `/api/user/info` — 需要 `Authorization: Bearer {accessToken}`
- `/api/member/*` — 需要 JWT Bearer
- `/api/auth/refresh` — 需要 `Authorization: Bearer {accessToken}` + `?refreshToken={token}`
- `/api/platform/banners` — 需要 JWT Bearer（非公开接口）

**日期格式：** `startTime`/`endTime` 字段使用 `yyyy-MM-dd HH:mm:ss`（空格分隔），**不要**使用 ISO 8601 `T` 分隔符。

**秒杀商品创建：** `POST /api/seckill/admin/product` 需要：
- `sessionId` 作为 **Query 参数**：`?sessionId={id}`
- Body 字段名为 `spuId`、`skuId`、`seckillPrice`、`seckillStock`、`limitPerUser`

**下单必填字段：** `POST /api/order/create` 的 `addressSnapshot` 字段为**必填**，不能省略。

**更新类接口：** `PUT` 更新操作返回 `data: null` 但变更已持久化，需通过查询接口验证。

**积分扣减：** 超额扣减返回 `code: 0` 但积分不变（非预期行为，建议返回错误码）。

---

## 用户域

### 场景列表（4 个）

| # | 场景 | API 步数 | 文档链接 |
|---|---|---|---|
| U1 | 注册→登录→个人信息 | 6 | [user-flow.md](user-flow.md#u1-注册登录个人信息) |
| U2 | 地址 CRUD + 默认地址 | 10 | [user-flow.md](user-flow.md#u2-地址管理完整流程) |
| U3 | 登录→Token 刷新 | 3 | [user-flow.md](user-flow.md#u3-认证登录token刷新) |
| U4 | 会员积分加减+升级 | 9 | [user-flow.md](user-flow.md#u4-会员积分流程) |

### API 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/user/register` | 用户注册 |
| POST | `/api/user/login` | 用户登录 |
| GET | `/api/user/info` | 获取用户信息 |
| POST | `/api/auth/login` | 认证登录（返回 JWT） |
| POST | `/api/auth/refresh` | 刷新 Token |
| POST | `/api/address` | 创建地址 |
| PUT | `/api/address` | 更新地址 |
| DELETE | `/api/address/{id}` | 删除地址 |
| GET | `/api/address/list` | 查询地址列表 |
| PUT | `/api/address/{id}/default` | 设为默认地址 |
| GET | `/api/member/{userId}` | 获取会员信息 |
| POST | `/api/member/points/add` | 增加积分 |
| POST | `/api/member/points/deduct` | 扣减积分 |

---

## 商品域

### 场景列表（8 个）

| # | 场景 | API 步数 | 文档链接 |
|---|---|---|---|
| P1 | 商品完整生命周期 | 12 | [product-flow.md](product-flow.md#p1-商品完整生命周期) |
| P2 | 最小字段创建商品 | 2 | [product-flow.md](product-flow.md#p2-最小字段创建商品) |
| P3 | 审核状态筛选 | 2 | [product-flow.md](product-flow.md#p3-审核状态筛选) |
| P4 | 搜索不存在关键词 | 1 | [product-flow.md](product-flow.md#p4-搜索不存在关键词) |
| P5 | 库存完整生命周期 | 5 | [product-flow.md](product-flow.md#p5-库存完整生命周期) |
| P6 | 库存异常场景 | 3 | [product-flow.md](product-flow.md#p6-库存异常场景) |
| P7 | 评价完整流程 | 7 | [product-flow.md](product-flow.md#p7-评价完整流程) |
| P8 | 多评价聚合 | 3 | [product-flow.md](product-flow.md#p8-多评价聚合) |

### API 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/product/spu` | 创建商品 SPU |
| GET | `/api/product/spu/{id}` | 查询 SPU 详情 |
| PUT | `/api/product/spu/{id}` | 更新 SPU |
| GET | `/api/product/spu/{id}/skus` | 查询 SPU 下 SKU 列表 |
| PUT | `/api/product/sku/{id}` | 更新 SKU |
| PUT | `/api/product/spu/{id}/audit` | 审核商品 |
| PUT | `/api/product/spu/{id}/shelf` | 上架/下架商品 |
| GET | `/api/product/list/shop/{shopId}` | 店铺商品列表 |
| GET | `/api/product/list/category/{categoryId}` | 类目商品列表 |
| GET | `/api/product/list` | 管理员商品列表 |
| GET | `/api/product/search` | 关键词搜索 |
| POST | `/api/inventory/init` | 初始化库存 |
| GET | `/api/inventory/sku/{skuId}` | 查询库存 |
| POST | `/api/inventory/deduct` | 扣减库存 |
| POST | `/api/review` | 创建评价 |
| GET | `/api/review/list/spu/{spuId}` | 商品评价列表 |
| GET | `/api/review/list/user/{userId}` | 用户评价列表 |
| PUT | `/api/review/{id}/append` | 追评 |
| PUT | `/api/review/{id}/reply` | 商家回复 |
| GET | `/api/review/rating/{spuId}` | 评分汇总 |
| GET | `/api/review/page` | 评价分页查询 |
| POST | `/api/search/internal/sync` | 索引商品 |
| POST | `/api/search/internal/sync/batch` | 批量索引 |
| DELETE | `/api/search/internal/sync/{spuId}` | 删除索引 |
| GET | `/api/search/product` | 搜索商品 |

---

## 交易域

### 场景列表（9 个）

| # | 场景 | API 步数 | 文档链接 |
|---|---|---|---|
| O1 | 订单完整生命周期 | 9 | [order-flow.md](order-flow.md#o1-订单完整生命周期) |
| O2 | 订单取消流程 | 4 | [order-flow.md](order-flow.md#o2-订单取消流程) |
| O3 | 订单多维度查询 | 7 | [order-flow.md](order-flow.md#o3-订单多维度查询) |
| O4 | 订单边界校验 | 7 | [order-flow.md](order-flow.md#o4-订单边界校验) |
| O5 | 取消回补库存 | 2 | [order-flow.md](order-flow.md#o5-取消回补库存) |
| PAY1 | 支付完整生命周期 | 6 | [order-flow.md](order-flow.md#pay1-支付完整生命周期) |
| PAY2 | 支付边界校验 | 3 | [order-flow.md](order-flow.md#pay2-支付边界校验) |
| PAY3 | 回调幂等 | 4 | [order-flow.md](order-flow.md#pay3-回调幂等) |
| PAY4 | 未支付退款拒绝 | 2 | [order-flow.md](order-flow.md#pay4-未支付退款拒绝) |

### API 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/order/create` | 创建订单 |
| GET | `/api/order/{orderNo}` | 订单详情 |
| GET | `/api/order/{orderNo}/items` | 订单商品明细 |
| PUT | `/api/order/{orderNo}/cancel` | 取消订单 |
| PUT | `/api/order/{orderNo}/ship` | 订单发货 |
| PUT | `/api/order/{orderNo}/receive` | 确认收货 |
| GET | `/api/order/list/user/{userId}` | 用户订单列表 |
| GET | `/api/order/list/shop/{shopId}` | 店铺订单列表 |
| GET | `/api/order/admin/list` | 管理员订单列表 |
| POST | `/api/payment/pay` | 发起支付 |
| GET | `/api/payment/{payNo}` | 查询支付单 |
| POST | `/api/payment/callback/mock` | 模拟支付回调 |
| POST | `/api/payment/refund` | 发起退款 |

---

## 营销域

### 场景列表（2 个）

| # | 场景 | API 步数 | 文档链接 |
|---|---|---|---|
| S1 | 秒杀全流程 | 10 | [marketing-flow.md](marketing-flow.md#s1-秒杀全流程) |
| C1 | 优惠券完整流程 | 6 | [marketing-flow.md](marketing-flow.md#c1-优惠券完整流程) |

### API 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/seckill/admin/session` | 创建秒杀场次 |
| POST | `/api/seckill/admin/product` | 创建秒杀商品 |
| POST | `/api/seckill/admin/preheat/{id}` | 预热秒杀库存 |
| GET | `/api/seckill/sessions` | 秒杀场次列表 |
| GET | `/api/seckill/products` | 秒杀商品列表 |
| POST | `/api/seckill/execute` | 执行秒杀 |
| POST | `/api/coupon/admin/template` | 创建优惠券模板 |
| POST | `/api/coupon/admin/distribute` | 发放优惠券 |
| POST | `/api/coupon/claim` | 用户领取优惠券 |
| GET | `/api/coupon/my` | 我的优惠券列表 |

---

## 商家域

### 场景列表（1 个）

| # | 场景 | API 步数 | 文档链接 |
|---|---|---|---|
| M1 | 商家入驻→审核→开店 | 6 | [shop-flow.md](shop-flow.md#m1-商家入驻审核开店) |

### API 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/shop/merchant/apply` | 商家入驻申请 |
| PUT | `/api/shop/merchant/{id}/audit` | 审核商家 |
| GET | `/api/shop/merchant/{id}` | 查询商家详情 |
| GET | `/api/shop/{id}` | 查询店铺详情 |
| GET | `/api/shop/merchant/page` | 商家分页列表 |

---

## 平台域

### 场景列表（4 个）

| # | 场景 | API 步数 | 文档链接 |
|---|---|---|---|
| A1 | Banner 完整 CRUD | 6 | [platform-flow.md](platform-flow.md#a1-banner-完整生命周期) |
| A2 | 广告位管理 | 2 | [platform-flow.md](platform-flow.md#a2-广告位管理) |
| R1 | 风控规则+评估+日志 | 4 | [platform-flow.md](platform-flow.md#r1-风控规则评估日志) |
| R2 | 搜索全流程 | 6 | [platform-flow.md](platform-flow.md#r2-搜索全流程) |

### API 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/platform/admin/banner` | 创建 Banner |
| GET | `/api/platform/admin/banners` | 管理员 Banner 列表 |
| GET | `/api/platform/banners` | 前台 Banner 列表 |
| PUT | `/api/platform/admin/banner/{id}` | 更新 Banner |
| DELETE | `/api/platform/admin/banner/{id}` | 删除 Banner |
| POST | `/api/platform/admin/position` | 创建广告位 |
| GET | `/api/platform/admin/positions` | 广告位列表 |
| POST | `/api/platform/admin/risk/rule` | 创建风控规则 |
| GET | `/api/platform/admin/risk/rules` | 风控规则列表 |
| POST | `/api/platform/admin/risk/evaluate` | 风险评估 |
| GET | `/api/platform/admin/risk/logs` | 风控日志 |

---

## 状态码映射

| 订单状态 | 值 | 支付状态 | 值 | 审核状态 | 值 |
|---|---|---|---|---|---|
| 待付款 | 1 | 待支付 | 1 | 待审核 | 0 |
| 已付款 | 2 | 已支付 | 2 | 审核通过 | 1 |
| 已发货 | 3 | 退款中 | 3 | 审核驳回 | 2 |
| 已完成 | 4 | 已退款 | 4 | | |
| 已取消 | 5 | | | | |

## 会员等级

| 等级 | 值 | 所需积分 |
|---|---|---|
| 普通 | 0 | 0 |
| 青铜 | 1 | >= 0 |
| 白银 | 2 | >= 1000 |
| 黄金 | 3 | >= 5000 |
| 钻石 | 4 | >= 20000 |
