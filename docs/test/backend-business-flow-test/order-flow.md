# 交易域 — 业务链路场景

> 涉及服务: service-order, service-payment
> Gateway 路径前缀: `/api/order`, `/api/payment`

---

## 下单请求体结构

```json
{
  "userId": 1,
  "shopId": 1,
  "addressSnapshot": "{\"name\":\"张三\",\"phone\":\"13800000001\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}",
  "remark": "请尽快发货",
  "couponId": null,
  "items": [
    {
      "skuId": 1,
      "skuName": "旗舰智能手机 标准版",
      "skuPrice": 99.00,
      "quantity": 2
    }
  ]
}
```

---

## O1: 订单完整生命周期

**场景描述**: 创建 → 查详情 → 查明细 → 支付 → 发货 → 收货（完整状态流转）

### Step 1: 创建订单

```http
POST /api/order/create
Content-Type: application/json

{ 见上方下单请求体 }
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "orderNo": "ORD20260517000001",
    "orderStatus": 1,
    "totalAmount": 198.00
  }
}
```
> 提取: `$.data.orderNo` → `orderNo`
> 状态: 1 = 待付款

### Step 2: 查订单详情

```http
GET /api/order/ORD20260517000001
```
> 验证: `$.data.orderStatus` = 1（待付款）

### Step 3: 查订单商品明细

```http
GET /api/order/ORD20260517000001/items
```
> 验证: `$.data[0].skuName` = "旗舰智能手机 标准版", `$.data[0].quantity` = 2, `$.data[0].skuPrice` = 99.00

### Step 4: 支付成功（内部服务调用）

> 由 `orderService.paySuccess(orderNo, payNo)` 执行，触发状态变更

### Step 5: 验证已付款

```http
GET /api/order/ORD20260517000001
```
> 验证: `$.data.orderStatus` = 2（已付款）

### Step 6: 发货

```http
PUT /api/order/ORD20260517000001/ship
```
> 验证: `$.data.orderStatus` = 3（已发货）, `$.data.shippedAt` 不为空

### Step 7: 确认收货

```http
PUT /api/order/ORD20260517000001/receive
```

### Step 8: 最终验证

```http
GET /api/order/ORD20260517000001
```
> 验证: `$.data.orderStatus` = 4（已完成）, `$.data.receivedAt` 不为空

---

## O2: 订单取消流程

**场景描述**: 创建 → 取消 → 验证已取消 → 尝试发货（拒绝）

### Step 1: 创建订单

```http
POST /api/order/create
Content-Type: application/json

{
  "userId": 100, "shopId": 1,
  "items": [{ "skuId": 1, "skuName": "Test", "skuPrice": 99.00, "quantity": 2 }]
}
```
> 提取: `$.data.orderNo` → `orderNo`

### Step 2: 取消订单

```http
PUT /api/order/{orderNo}/cancel?reason=Changed my mind
```

### Step 3: 验证已取消

```http
GET /api/order/{orderNo}
```
> 验证: `$.data.orderStatus` = 5（已取消）

### Step 4 (异常): 尝试发货已取消订单

```http
PUT /api/order/{orderNo}/ship
```
> 响应: `{"code": 30004}` — 仅待发货订单可发货

---

## O3: 订单多维度查询

**场景描述**: 创建3个订单（不同状态）→ 用户列表 → 状态筛选 → 店铺列表 → 管理员列表

### Step 1: 创建 3 个订单

```http
# 订单1
POST /api/order/create  { "userId": 200, "shopId": 10, "items": [...] }
# → orderNo1

# 订单2
POST /api/order/create  { "userId": 200, "shopId": 10, "items": [...] }
# → orderNo2

# 订单3
POST /api/order/create  { "userId": 200, "shopId": 10, "items": [...] }
# → orderNo3
```

> 将 orderNo1 支付成功（状态→2），其余保持待付款（状态=1）

### Step 2: 用户订单列表（全部）

```http
GET /api/order/list/user/200
```
> 验证: `$.data.total` = 3

### Step 3: 用户订单列表（待付款筛选）

```http
GET /api/order/list/user/200?status=1
```
> 验证: `$.data.total` = 2

### Step 4: 用户订单列表（已付款筛选）

```http
GET /api/order/list/user/200?status=2
```
> 验证: `$.data.total` = 1

### Step 5: 店铺订单列表

```http
GET /api/order/list/shop/10
```
> 验证: `$.data.total` = 3

### Step 6: 管理员列表

```http
GET /api/order/admin/list
```
> 验证: `$.data.total` 是数字

---

## O4: 订单边界校验

**场景描述**: 创建 → 未付款先收货（拒绝）→ 支付 → 发货 → 重复发货（拒绝）→ 收货 → 取消已完成（拒绝）

### Step 1: 创建

```http
POST /api/order/create  { "userId": 300, "shopId": 1, "items": [...] }
```
> 提取: `$.data.orderNo` → `orderNo`

### Step 2 (异常): 未付款先收货

```http
PUT /api/order/{orderNo}/receive
```
> 响应: `{"code": 30005}` — 仅待收货订单可确认

### Step 3: 支付

> 内部服务调用 `orderService.paySuccess(orderNo, payNo)`

### Step 4: 第一次发货（成功）

```http
PUT /api/order/{orderNo}/ship
```
> 响应: `{"code": 0}`

### Step 5 (异常): 重复发货

```http
PUT /api/order/{orderNo}/ship
```
> 响应: `{"code": 30004}` — 仅待发货订单可发货

### Step 6: 收货

```http
PUT /api/order/{orderNo}/receive
```
> 响应: `{"code": 0}` — 状态→4（已完成）

### Step 7 (异常): 取消已完成订单

```http
PUT /api/order/{orderNo}/cancel?reason=Too late
```
> 响应: `{"code": 30003}` — 仅待付款订单可取消

---

## O5: 取消回补库存

**场景描述**: 创建订单（触发库存扣减）→ 取消订单（触发库存回补）

### Step 1: 创建订单

```http
POST /api/order/create  { "userId": 400, "shopId": 1, "items": [{..., "quantity": 2}] }
```
> 验证: 库存 Dubbo 服务 `deduct(skuId, 2)` 被调用

### Step 2: 取消订单

```http
PUT /api/order/{orderNo}/cancel?reason=Cancel for rollback test
```
> 验证: 库存 Dubbo 服务 `restore(skuId, 2)` 被调用

---

## 订单状态码

| 状态 | 值 | 允许操作 |
|---|---|---|
| 待付款 | 1 | 支付、取消 |
| 已付款 | 2 | 发货 |
| 已发货 | 3 | 确认收货 |
| 已完成 | 4 | — |
| 已取消 | 5 | — |

---

## PAY1: 支付完整生命周期

**场景描述**: 发起支付 → 查询 → 回调 → 查询(已支付) → 退款 → 查询(已退款)

### Step 1: 发起支付

```http
POST /api/payment/pay?orderNo=ORD-FLOW-TEST-001&userId=1&amount=198.00&payMethod=1
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "payNo": "PAY20260517000001",
    "payStatus": 1,
    "amount": 198.00
  }
}
```
> 提取: `$.data.payNo` → `payNo`
> `payMethod`: 1=支付宝, 2=微信

### Step 2: 查询支付单

```http
GET /api/payment/PAY20260517000001
```
> 验证: `$.data.payStatus` = 1（待支付）

### Step 3: 模拟支付回调

```http
POST /api/payment/callback/mock?requestId=REQ-1715875200&payNo=PAY20260517000001
```
> 验证: `$.data` = "success", 订单状态→2（已付款）

### Step 4: 验证回调后状态

```http
GET /api/payment/PAY20260517000001
```
> 验证: `$.data.payStatus` = 2（已支付）, `$.data.thirdPayNo` 不为空, `$.data.paidAt` 不为空

### Step 5: 发起退款

```http
POST /api/payment/refund?orderNo=ORD-FLOW-TEST-001&refundAmount=198.00&reason=Product quality issue
```
> 验证: `$.data.refundNo` 不为空, `$.data.refundAmount` = 198.00, `$.data.refundStatus` = 2

### Step 6: 验证已退款

```http
GET /api/payment/PAY20260517000001
```
> 验证: `$.data.payStatus` = 4（已退款）

---

## PAY2: 支付边界校验

### Step 1: 发起支付

```http
POST /api/payment/pay?orderNo=ORD-EDGE-001&userId=2&amount=99.00&payMethod=1
```

### Step 2 (异常): 重复支付

```http
POST /api/payment/pay?orderNo=ORD-EDGE-001&userId=2&amount=99.00&payMethod=1
```
> 响应: `{"code": 30007}` — 该订单已创建支付单

### Step 3 (异常): 查询不存在支付单

```http
GET /api/payment/PAY-NOT-EXIST
```
> 响应: `{"code": 30008}` — 支付单不存在

---

## PAY3: 回调幂等

**场景描述**: 发起支付 → 第一次回调 → 相同 requestId 第二次回调 → 验证仅处理一次

### Step 1: 发起支付

```http
POST /api/payment/pay?orderNo=ORD-IDEM-001&userId=3&amount=50.00&payMethod=2
```
> 提取: `$.data.payNo` → `payNo`

### Step 2: 第一次回调

```http
POST /api/payment/callback/mock?requestId=IDEM-REQ-001&payNo={payNo}
```
> 验证: `orderDubboService.updateStatus(orderNo, 2)` 被调用 1 次

### Step 3: 第二次回调（相同 requestId）

```http
POST /api/payment/callback/mock?requestId=IDEM-REQ-001&payNo={payNo}
```
> 验证: `orderDubboService.updateStatus` 仍仅调用 1 次（幂等）

### Step 4: 验证状态未变

```http
GET /api/payment/{payNo}
```
> 验证: `$.data.payStatus` = 2（未变）

---

## PAY4: 未支付退款拒绝

### Step 1: 发起支付

```http
POST /api/payment/pay?orderNo=ORD-REFUND-ERR-001&userId=4&amount=75.00&payMethod=1
```

### Step 2 (异常): 未支付就退款

```http
POST /api/payment/refund?orderNo=ORD-REFUND-ERR-001&refundAmount=75.00&reason=Test refund on unpaid
```
> 响应: `{"code": 30010}` — 仅已支付订单可退款

---

## 支付状态码

| 状态 | 值 | 说明 |
|---|---|---|
| 待支付 | 1 | 初始状态 |
| 已支付 | 2 | 回调成功 |
| 退款中 | 3 | 退款申请 |
| 已退款 | 4 | 退款成功 |
