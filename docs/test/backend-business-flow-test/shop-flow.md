# 商家域 — 业务链路场景

> 涉及服务: service-shop
> Gateway 路径前缀: `/api/shop`

---

## M1: 商家入驻→审核→开店

**场景描述**: 提交入驻申请 → 查询待审核 → 平台审核通过 → 查询店铺 → 分页列表

### Step 1: 提交商家入驻申请

```http
POST /api/shop/merchant/apply
Content-Type: application/json

{
  "userId": 1001,
  "companyName": "测试科技股份有限公司",
  "businessLicense": "91440101MA5XXXXXX",
  "legalPerson": "张三",
  "idCard": "110101199001011234",
  "contactPhone": "13800138000"
}
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "companyName": "测试科技股份有限公司",
    "auditStatus": 0
  }
}
```
> 提取: `$.data.id` → `merchantId`
> `auditStatus`: 0=待审核

### Step 2: 平台审核通过

```http
PUT /api/shop/merchant/1/audit?auditStatus=1&reason=资质审核通过
```

**响应**: `{"code": 0, "message": "success"}`
> `auditStatus`: 1=通过, 2=驳回
> 审核通过后自动创建店铺

### Step 3: 查询商家详情（含店铺信息）

```http
GET /api/shop/merchant/1
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "merchantId": 1,
    "shopId": 1,
    "shopName": "测试科技股份有限公司旗舰店",
    "auditStatus": 1
  }
}
```
> 提取: `$.data.shopId` → `shopId`, `$.data.shopName` → `shopName`

### Step 4: 查询店铺详情

```http
GET /api/shop/1
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "shopName": "测试科技股份有限公司旗舰店"
  }
}
```

### Step 5: 商家分页列表

```http
GET /api/shop/merchant/page?page=1&size=10
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "records": [{ "id": 1, "auditStatus": 1 }],
    "total": 1
  }
}
```

### Step 6 (异常): 重复申请

```http
POST /api/shop/merchant/apply
Content-Type: application/json

{ "userId": 1001, "companyName": "测试科技股份有限公司", ... }
```

**响应** :
```json
{
  "code": 50003,
  "message": "该用户已提交入驻申请"
}
```

---

## 商家状态码

| 审核状态 | 值 | 说明 |
|---|---|---|
| 待审核 | 0 | 初始提交 |
| 通过 | 1 | 自动创建店铺 |
| 驳回 | 2 | 需重新提交 |
