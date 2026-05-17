# 平台域 — 业务链路场景

> 涉及服务: service-platform, service-search
> Gateway 路径前缀: `/api/platform`, `/api/search`

---

## A1: Banner 完整生命周期

**场景描述**: 创建 → 管理员列表 → 前台列表 → 更新 → 删除 → 验证删除

### Step 1: 创建 Banner

```http
POST /api/platform/admin/banner
Content-Type: application/json

{
  "title": "618大促",
  "imageUrl": "https://img.example.com/banner1.jpg",
  "linkUrl": "https://www.example.com/618",
  "sortOrder": 1,
  "position": "HOME_TOP",
  "status": 1,
  "startTime": "2026-06-01T00:00:00",
  "endTime": "2026-06-30T23:59:59"
}
```

**响应**:
```json
{
  "code": 0,
  "data": { "id": 1, "title": "618大促", "position": "HOME_TOP" }
}
```
> 提取: `$.data.id` → `bannerId`
> `status`: 1=启用, 0=禁用

### Step 2: 管理员列表

```http
GET /api/platform/admin/banners?page=1&size=20
```
> 验证: `$.data.records[0].title` = "618大促"

### Step 3: 前台 Banner 列表（按位置）

```http
GET /api/platform/banners?position=HOME_TOP
```
> 验证: `$.data[0].title` = "618大促"
> 仅返回启用且在有效期内的 Banner

### Step 4: 更新 Banner

```http
PUT /api/platform/admin/banner/1
Content-Type: application/json

{ "title": "618年中大促", "sortOrder": 2 }
```

**响应**: `$.data.title` = "618年中大促"

### Step 5: 删除 Banner

```http
DELETE /api/platform/admin/banner/1
```

**响应**: `{"code": 0, "message": "success"}`

### Step 6: 验证删除（列表为空）

```http
GET /api/platform/admin/banners?page=1&size=20
```
> 验证: `$.data.total` = 0

---

## A2: 广告位管理

### Step 1: 创建广告位

```http
POST /api/platform/admin/position
Content-Type: application/json

{
  "code": "HOME_BANNER",
  "name": "首页Banner位",
  "description": "首页顶部轮播广告位",
  "status": 1
}
```

**响应**: `$.data.id` → `positionId`

### Step 2: 广告位列表

```http
GET /api/platform/admin/positions
```

**响应**:
```json
{
  "code": 0,
  "data": [{ "code": "HOME_BANNER", "name": "首页Banner位", "status": 1 }]
}
```

---

## R1: 风控规则+评估+日志

### Step 1: 创建风控规则

```http
POST /api/platform/admin/risk/rule
Content-Type: application/json

{
  "name": "高频下单检测",
  "type": 1,
  "config": "{\"threshold\":100,\"score\":50}",
  "status": 1
}
```

**响应**: `$.data.id` → `ruleId`

### Step 2: 风控规则列表

```http
GET /api/platform/admin/risk/rules
```
> 验证: `$.data` 非空

### Step 3: 风险评估

```http
POST /api/platform/admin/risk/evaluate?userId=10001&targetId=ORDER_20260516001&riskType=1
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "totalScore": 0,
    "action": "PASS",
    "actionDesc": "通过"
  }
}
```

### Step 4: 风控日志

```http
GET /api/platform/admin/risk/logs?riskType=1&page=1&size=20
```
> 验证: `$.data.records` 非空

---

## R2: 搜索全流程

### Step 1: 索引单个商品

```http
POST /api/search/internal/sync
Content-Type: application/json

{
  "spuId": 100,
  "name": "SuperTech X100 智能手机",
  "shopId": 1,
  "categoryId": 10,
  "brandName": "SuperTech",
  "minPrice": 4999.00,
  "maxPrice": 6999.00,
  "shelfStatus": 1,
  "skuList": [{ "skuId": 1001, "price": 4999.00 }]
}
```

### Step 2: 关键词搜索

```http
GET /api/search/product?keyword=智能手机&page=1&size=20
```
> 验证: `$.data.total` >= 1

### Step 3: 分类筛选搜索

```http
GET /api/search/product?keyword=智能手机&categoryId=10&page=1&size=20
```

### Step 4: 价格区间搜索

```http
GET /api/search/product?keyword=智能手机&minPrice=4000&maxPrice=8000&sort=price_asc&page=1&size=20
```

### Step 5: 品牌筛选搜索

```http
GET /api/search/product?brand=SuperTech&page=1&size=20
```

### Step 6: 删除索引

```http
DELETE /api/search/internal/sync/100
```

---

## 批量索引与搜索

### 批量索引多个商品

```http
POST /api/search/internal/sync/batch
Content-Type: application/json

[
  { "spuId": 100, "name": "SuperTech X100 智能手机", "categoryId": 10, "minPrice": 4999, "maxPrice": 6999 },
  { "spuId": 101, "name": "SuperTech Pad 平板电脑", "categoryId": 10, "minPrice": 2999, "maxPrice": 3999 }
]
```

### 搜索所有索引商品（无关键词）

```http
GET /api/search/product?page=1&size=20
```
> 不传 keyword 时返回所有已索引商品

---

## 搜索参数说明

| 参数 | 类型 | 说明 |
|---|---|---|
| keyword | String | 搜索关键词（可选，不传返回全部） |
| categoryId | Long | 分类 ID 筛选 |
| brand | String | 品牌筛选 |
| minPrice | BigDecimal | 最低价格 |
| maxPrice | BigDecimal | 最高价格 |
| sort | String | 排序：`price_asc` / `price_desc` |
| page | int | 页码（默认 1） |
| size | int | 每页数量（默认 20） |
