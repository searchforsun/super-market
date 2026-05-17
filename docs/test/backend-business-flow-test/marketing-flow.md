# 营销域 — 业务链路场景

> 涉及服务: service-seckill, service-coupon
> Gateway 路径前缀: `/api/seckill`, `/api/coupon`

---

## S1: 秒杀全流程

**场景描述**: 创建场次 → 创建秒杀商品 → 预热库存 → 查看场次/商品 → 执行秒杀(成功) → 用户限购 → 库存耗尽

### Step 1: 创建秒杀场次

```http
POST /api/seckill/admin/session
Content-Type: application/json

{
  "name": "618限时秒杀",
  "startTime": "2026-06-18T00:00:00",
  "endTime": "2026-06-19T00:00:00"
}
```

**响应**:
```json
{
  "code": 0,
  "data": { "id": 1, "name": "618限时秒杀" }
}
```
> 提取: `$.data.id` → `sessionId`

### Step 2: 创建秒杀商品

```http
POST /api/seckill/admin/product
Content-Type: application/json

{
  "sessionId": 1,
  "spuId": 10001,
  "skuId": 20001,
  "seckillPrice": 99.99,
  "seckillStock": 10,
  "limitPerUser": 2
}
```

**响应**:
```json
{
  "code": 0,
  "data": { "id": 1, "sessionId": 1, "seckillStock": 10 }
}
```
> 提取: `$.data.id` → `productId`

### Step 3: 预热库存到 Redis

```http
POST /api/seckill/admin/preheat/1
```

**响应**: `{"code": 0, "message": "success"}`
> 内部将库存写入 Redis，秒杀时直接从 Redis 扣减

### Step 4: 查看场次列表

```http
GET /api/seckill/sessions
```

**响应**:
```json
{
  "code": 0,
  "data": [{ "id": 1, "name": "618限时秒杀", "startTime": "2026-06-18T00:00:00" }]
}
```

### Step 5: 查看场次下商品

```http
GET /api/seckill/products?sessionId=1
```

**响应**:
```json
{
  "code": 0,
  "data": [{ "id": 1, "seckillPrice": 99.99, "seckillStock": 10 }]
}
```

### Step 6: 用户1 首次秒杀（成功）

```http
POST /api/seckill/execute?userId=1&seckillProductId=1&quantity=1
```

**响应**:
```json
{
  "code": 0,
  "data": { "success": true, "message": "秒杀成功" }
}
```

### Step 7: 用户1 第二次秒杀（成功，未超限）

```http
POST /api/seckill/execute?userId=1&seckillProductId=1&quantity=1
```
> 验证: `$.data.success` = true（每人限购 2 件，第2次仍在限购内）

### Step 8 (异常): 用户1 第三次秒杀（超限购）

```http
POST /api/seckill/execute?userId=1&seckillProductId=1&quantity=1
```

**响应** :
```json
{
  "code": 40011,
  "message": "已达每人限购数量"
}
```

### Step 9: 用户2~6 各秒杀 2 件（消耗剩余库存）

```http
# 用户2
POST /api/seckill/execute?userId=2&seckillProductId=1&quantity=2
# 用户3
POST /api/seckill/execute?userId=3&seckillProductId=1&quantity=2
# 用户4
POST /api/seckill/execute?userId=4&seckillProductId=1&quantity=2
# 用户5
POST /api/seckill/execute?userId=5&seckillProductId=1&quantity=2
# 用户6
POST /api/seckill/execute?userId=6&seckillProductId=1&quantity=2
```
> 用户1用2件 + 用户2~6各2件 = 12件 > 库存10 → 用户6的第2件将失败

### Step 10 (异常): 库存耗尽

```http
POST /api/seckill/execute?userId=7&seckillProductId=1&quantity=1
```

**响应** :
```json
{
  "code": 40010,
  "message": "秒杀库存不足"
}
```

---

## 秒杀状态码

| code | 含义 |
|---|---|
| 0 | 秒杀成功 (`$.data.success` = true) |
| 400xx | 业务失败（库存不足 / 限购 / 未预热 / 锁获取失败） |

---

## C1: 优惠券完整流程

**场景描述**: 创建模板 → 发放给用户 → 用户领取 → 查看我的优惠券 → 使用 → 重复使用拒绝

### Step 1: 创建优惠券模板

```http
POST /api/coupon/admin/template
Content-Type: application/json

{
  "name": "满100减20",
  "type": 1,
  "discountValue": 20.00,
  "minAmount": 100.00,
  "totalStock": 100,
  "perUserLimit": 2,
  "validDays": 30,
  "status": 1
}
```

**响应**:
```json
{
  "code": 0,
  "data": { "id": 1, "name": "满100减20", "totalStock": 100 }
}
```
> 提取: `$.data.id` → `templateId`
> `type`: 1=满减券, 2=折扣券

### Step 2: 管理员发放优惠券

```http
POST /api/coupon/admin/distribute?templateId=1
Content-Type: application/json

[101, 102]
```

**响应**:
```json
{
  "code": 0,
  "data": { "id": 1, "quantity": 2 }
}
```
> 向 userId=101 和 102 各发放1张

### Step 3: 用户领取优惠券

```http
POST /api/coupon/claim?userId=103&templateId=1
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "userId": 103,
    "couponCode": "CPN20260517000001",
    "status": 1,
    "discountValue": 20.00,
    "minAmount": 100.00
  }
}
```
> 提取: `$.data.id` → `userCouponId`, `$.data.couponCode` → `couponCode`
> `status`: 1=未使用

### Step 4: 查看我的优惠券

```http
GET /api/coupon/my?userId=103&page=1&size=10
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "records": [{ "id": 1, "couponCode": "CPN20260517000001", "status": 1 }],
    "total": 1
  }
}
```

### Step 5: 使用优惠券

> 内部服务调用 `couponService.useCoupon(103, userCouponId, "TEST_ORDER_001")`
> 验证: 返回的 `UserCoupon.status` = 2（已使用）, `orderNo` = "TEST_ORDER_001"

### Step 6 (异常): 重复使用

> 内部服务调用 `couponService.useCoupon(103, userCouponId, "TEST_ORDER_002")`
> 验证: 抛出 BizException，消息包含"不可用"

---

## 优惠券状态码

| 状态 | 值 | 说明 |
|---|---|---|
| 未使用 | 1 | 可正常使用 |
| 已使用 | 2 | 绑定订单号 |

## 优惠券类型

| 类型 | 值 | 说明 |
|---|---|---|
| 满减券 | 1 | 满 `minAmount` 减 `discountValue` |
| 折扣券 | 2 | 按 `discountValue` 折扣 |
