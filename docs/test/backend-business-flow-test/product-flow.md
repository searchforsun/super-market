# 商品域 — 业务链路场景

> 涉及服务: service-product, service-category, service-inventory, service-review, service-search
> Gateway 路径前缀: `/api/product`, `/api/category`, `/api/inventory`, `/api/review`, `/api/search`

---

## P1: 商品完整生命周期

**场景描述**: 创建商品(SPU+SKU) → 查询 → 审核 → 上架 → 多维度列表 → 搜索 → 更新

### Step 1: 创建商品 (SPU + SKUs)

```http
POST /api/product/spu
Content-Type: application/json

{
  "shopId": 1,
  "categoryId": 1,
  "name": "旗舰智能手机",
  "subtitle": "2026年度旗舰机型",
  "mainImage": "https://example.com/phone.jpg",
  "images": ["https://example.com/phone_1.jpg", "https://example.com/phone_2.jpg"],
  "description": "这是一款旗舰智能手机，搭载最新处理器。",
  "skus": [
    { "specName": "标准版", "price": 99.99, "marketPrice": 129.99, "weight": 500 },
    { "specName": "Pro版", "price": 199.99, "marketPrice": 259.99, "weight": 500 }
  ]
}
```

**响应**:
```json
{
  "code": 0,
  "data": { "id": 100, "name": "旗舰智能手机", "shopId": 1 }
}
```
> 提取: `$.data.id` → `spuId`

### Step 2: 查询 SPU 详情

```http
GET /api/product/spu/100
```
> 验证: `$.data.name` = "旗舰智能手机"

### Step 3: 查询 SKU 列表

```http
GET /api/product/spu/100/skus
```
> 验证: `$.data.length()` = 2

### Step 4: 审核通过

```http
PUT /api/product/spu/100/audit?auditStatus=1&reason=商品信息完整，审核通过
```
> `auditStatus`: 0=待审核, 1=通过, 2=驳回

### Step 5: 上架商品

```http
PUT /api/product/spu/100/shelf?shelfStatus=1
```
> `shelfStatus`: 1=上架, 0=下架

### Step 6: 按店铺列表

```http
GET /api/product/list/shop/1?page=1&size=20
```

### Step 7: 按分类列表

```http
GET /api/product/list/category/1?page=1&size=20
```

### Step 8: 关键词搜索

```http
GET /api/product/search?keyword=旗舰&page=1&size=20
```
> 验证: `$.data.records[0].name` = "旗舰智能手机"

### Step 9: 管理员列表（按审核状态过滤）

```http
GET /api/product/list?auditStatus=1&page=1&size=20
```

### Step 10: 更新 SPU

```http
PUT /api/product/spu/100
Content-Type: application/json

{ "name": "旗舰智能手机-2026款", "subtitle": "2026年度旗舰机型-升级版" }
```

### Step 10b: 验证更新持久化

```http
GET /api/product/spu/100
```
> 验证: `$.data.name` = "旗舰智能手机-2026款"

### Step 11: 更新 SKU 价格

```http
PUT /api/product/sku/1001
Content-Type: application/json

{ "price": 89.99, "status": 1 }
```

---

## P2: 最小字段创建商品

```http
POST /api/product/spu
Content-Type: application/json

{
  "shopId": 2,
  "categoryId": 3,
  "name": "简约商品"
}
```

```http
GET /api/product/spu/{id}
```
> 验证: `$.data.name` = "简约商品", 可选字段均为 null

---

## P3: 审核状态筛选

### Step 1: 创建待审核商品

```http
POST /api/product/spu
Content-Type: application/json

{ "shopId": 1, "categoryId": 2, "name": "待审核商品" }
```

### Step 2: 按审核状态筛选

```http
GET /api/product/list?auditStatus=0
```
> 验证: 列表中包含待审核商品

---

## P4: 搜索不存在关键词

```http
GET /api/product/search?keyword=NONEXISTENT_PRODUCT_XYZ_98765
```
> 验证: `$.data.records.length()` = 0

---

## P5: 库存完整生命周期

**场景描述**: 初始化库存 → 查询 → 扣减 → 超额扣减拒绝 → 验证不变

### Step 1: 初始化库存

```http
POST /api/inventory/init?skuId=100&totalStock=100&safetyStock=10
```

**响应**:
```json
{
  "code": 0,
  "data": { "skuId": 100, "totalStock": 100, "availableStock": 100 }
}
```

### Step 2: 查询库存

```http
GET /api/inventory/sku/100
```
> 验证: `$.data.totalStock` = 100, `$.data.availableStock` = 100

### Step 3: 扣减 10 件

```http
POST /api/inventory/deduct?skuId=100&quantity=10
```
> 验证: `$.data` = true

### Step 3b: 验证剩余库存

```http
GET /api/inventory/sku/100
```
> 验证: `$.data.availableStock` = 90

### Step 4 (异常): 超额扣减

```http
POST /api/inventory/deduct?skuId=100&quantity=999
```

**响应** (200, 业务失败):
```json
{ "code": 20008, "message": "库存不足" }
```

### Step 4b: 验证库存未变

```http
GET /api/inventory/sku/100
```
> 验证: `$.data.availableStock` = 90（未改变）

---

## P6: 库存异常场景

### 场景 A: 重复初始化

```http
POST /api/inventory/init?skuId=200&totalStock=50
```

```http
POST /api/inventory/init?skuId=200&totalStock=100
```
> 响应: `{"code": 20009}` — 该SKU库存已初始化

### 场景 B: 扣减不存在的 SKU

```http
POST /api/inventory/deduct?skuId=99999&quantity=1
```
> 响应: `{"code": 20008, "message": "库存不足"}`

### 场景 C: 查询不存在的 SKU

```http
GET /api/inventory/sku/99999
```
> 响应: `{"code": 20010, "message": "库存信息不存在"}`

---

## P7: 评价完整流程

**场景描述**: 创建评价 → SPU列表 → 用户列表 → 追评 → 商家回复 → 评分汇总 → 评分范围过滤

### Step 1: 创建评价

```http
POST /api/review
Content-Type: application/json

{
  "userId": 1001,
  "spuId": 200,
  "skuId": 300,
  "orderNo": "ORD202605160001",
  "rating": 5,
  "content": "手机质量非常好，拍照清晰，运行流畅！",
  "isAnonymous": 0
}
```

**响应**:
```json
{
  "code": 0,
  "data": { "id": 100, "rating": 5, "content": "手机质量非常好..." }
}
```

### Step 2: 按 SPU 列出评价

```http
GET /api/review/list/spu/200?page=1&size=10
```
> 验证: `$.data.records.length()` = 1

### Step 3: 按用户列出评价

```http
GET /api/review/list/user/1001?page=1&size=10
```

### Step 4: 追评

```http
PUT /api/review/100/append?userId=1001&content=使用一周后，续航表现优秀
```

### Step 5: 商家回复

```http
PUT /api/review/100/reply?content=感谢您的评价，我们会继续努力！
```

### Step 6: 评分汇总

```http
GET /api/review/rating/200
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "avgRating": 5.0,
    "distribution": { "5": 1 }
  }
}
```

### Step 7: 评分范围过滤

```http
GET /api/review/page?minRating=4&maxRating=5&spuId=200&page=1&size=10
```
> 验证: `$.data.records.length()` = 1

---

## P8: 多评价聚合

### Step 1-2: 创建两个不同评分评价

```http
POST /api/review
Content-Type: application/json

{
  "userId": 1002, "spuId": 201, "skuId": 301,
  "orderNo": "ORD202605160002", "rating": 5, "content": "Excellent!"
}
```

```http
POST /api/review
Content-Type: application/json

{
  "userId": 1003, "spuId": 201, "skuId": 301,
  "orderNo": "ORD202605160003", "rating": 4, "content": "Good"
}
```

### Step 3: 验证评分聚合

```http
GET /api/review/rating/201
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "avgRating": 4.5,
    "distribution": { "4": 1, "5": 1 }
  }
}
```

---

## 评价异常场景

### 评分超出范围

```http
POST /api/review
Content-Type: application/json

{ "rating": 6, "content": "Invalid" }
```
> 响应: `{"code": 80002}` — 评分必须在1-5之间

### 缺少订单号

```http
POST /api/review
Content-Type: application/json

{ "rating": 5, "content": "No order no" }
```
> 响应: `{"code": 80003}` — 订单号不能为空
