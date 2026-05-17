# 用户域 — 业务链路场景

> 涉及服务: service-user, service-auth, service-address, service-member
> Gateway 路径前缀: `/api/user`, `/api/auth`, `/api/address`, `/api/member`

---

## U1: 注册→登录→个人信息

**场景描述**: 用户注册 → 登录 → 查看个人信息 → 重复注册报错 → 错误密码登录报错 → 不存在用户报错

### Step 1: 用户注册

```http
POST /api/user/register
Content-Type: application/json

{
  "phone": "18800000001",
  "password": "Pass1234"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "phone": "18800000001",
    "createdAt": "2026-05-17T00:00:00"
  }
}
```
> 提取: `$.data.id` → `userId`

### Step 2: 用户登录

```http
POST /api/user/login
Content-Type: application/json

{
  "phone": "18800000001",
  "password": "Pass1234"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "phone": "18800000001",
    "lastLoginAt": "2026-05-17T00:00:01"
  }
}
```
> 验证: `$.data.id` = `userId`

### Step 3: 获取用户信息

```http
GET /api/user/info?userId=1
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "phone": "18800000001"
  }
}
```

### Step 4 (异常): 重复注册

```http
POST /api/user/register
Content-Type: application/json

{
  "phone": "18800000001",
  "password": "Pass1234"
}
```

**响应** :
```json
{
  "code": 10002,
  "message": "手机号已注册"
}
```

### Step 5 (异常): 密码错误

```http
POST /api/user/login
Content-Type: application/json

{
  "phone": "18800000001",
  "password": "WrongPass1"
}
```

**响应**:
```json
{
  "code": 10003,
  "message": "密码错误"
}
```

### Step 6 (异常): 用户不存在

```http
GET /api/user/info?userId=99999999
```

**响应** :
```json
{
  "code": 10001,
  "message": "用户不存在"
}
```

---

## U2: 地址管理完整流程

**场景描述**: 创建3个地址 → 查看列表 → 设为默认 → 更新默认地址 → 删除非默认 → 验证最终列表

### Step 1a: 创建家庭地址

```http
POST /api/address
Content-Type: application/json

{
  "userId": 10001,
  "receiverName": "张三",
  "receiverPhone": "13800001001",
  "province": "广东省",
  "city": "深圳市",
  "district": "南山区",
  "detail": "科技园南路88号"
}
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "receiverName": "张三",
    "isDefault": 1
  }
}
```
> 提取: `$.data.id` → `homeId`（首个地址自动为默认）

### Step 1b: 创建工作地址

```http
POST /api/address
Content-Type: application/json

{
  "userId": 10001,
  "receiverName": "张三",
  "receiverPhone": "13800001002",
  "province": "广东省",
  "city": "深圳市",
  "district": "福田区",
  "detail": "深南大道100号"
}
```

**响应**: `$.data.id` → `workId`

### Step 1c: 创建父母地址

```http
POST /api/address
Content-Type: application/json

{
  "userId": 10001,
  "receiverName": "张伟",
  "receiverPhone": "13800001003",
  "province": "广东省",
  "city": "广州市",
  "district": "天河区",
  "detail": "天河路200号"
}
```

**响应**: `$.data.id` → `parentsId`

### Step 2: 列出全部地址

```http
GET /api/address/list?userId=10001
```

**响应**:
```json
{
  "code": 0,
  "data": [
    { "id": 1, "receiverName": "张三", "isDefault": 1 },
    { "id": 2, "receiverName": "张三", "isDefault": 0 },
    { "id": 3, "receiverName": "张伟", "isDefault": 0 }
  ]
}
```
> 验证: `$.data.length()` = 3

### Step 3: 将工作地址设为默认

```http
PUT /api/address/2/default?userId=10001
```

**响应**: `{"code": 0, "message": "success"}`

### Step 3b: 再次列出，验证默认变更

```http
GET /api/address/list?userId=10001
```
> 验证: `$.data[0].id` = `workId`, `$.data[0].isDefault` = 1

### Step 4: 更新默认地址

```http
PUT /api/address
Content-Type: application/json

{
  "id": 2,
  "receiverName": "张三（公司）",
  "receiverPhone": "13800001002",
  "province": "广东省",
  "city": "深圳市",
  "district": "福田区",
  "detail": "深南大道100号"
}
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 2,
    "receiverName": "张三（公司）",
    "isDefault": 1
  }
}
```

### Step 5: 删除非默认地址（父母地址）

```http
DELETE /api/address/3?userId=10001
```

**响应**: `{"code": 0, "message": "success"}`

### Step 6: 最终验证（仅剩2条，默认不变）

```http
GET /api/address/list?userId=10001
```
> 验证: `$.data.length()` = 2, `$.data[0].id` = `workId`, `$.data[0].isDefault` = 1

---

## U3: 认证登录→Token 刷新

**场景描述**: 登录获取 JWT → 刷新 Token → 再次刷新

### Step 1: 登录获取 Token

```http
POST /api/auth/login
Content-Type: application/json

{
  "phone": "13800000001",
  "password": "Pass1234"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": "1",
    "accessToken": "eyJhbG...",
    "refreshToken": "eyJhbG...",
    "roles": "ROLE_USER"
  }
}
```
> 提取: `$.data.accessToken` → `accessToken`, `$.data.refreshToken` → `refreshToken`

### Step 2: 刷新 Token

```http
POST /api/auth/refresh?refreshToken=eyJhbG...
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbG...（新）",
    "refreshToken": "eyJhbG...（新）"
  }
}
```
> 提取: `$.data.accessToken` → 验证可解析，`userId`=1

### Step 3: 用新 refreshToken 再次刷新（验证轮换）

```http
POST /api/auth/refresh?refreshToken={新的refreshToken}
```

**响应**: `{"code": 0, "data": {"accessToken": "...", "refreshToken": "..."}}`

---

## U4: 会员积分流程

**场景描述**: 获取会员 → 加积分 → 扣积分 → 升级 → 超额扣减拒绝

### Step 1: 获取会员信息（自动创建）

```http
GET /api/member/20001
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "userId": 20001,
    "points": 0,
    "totalPoints": 0,
    "level": 0
  }
}
```

### Step 2: 加 100 积分

```http
POST /api/member/points/add?userId=20001&points=100
```

**响应**: `{"code": 0, "message": "success"}`

### Step 3: 验证积分

```http
GET /api/member/20001
```
> 验证: `points`=100, `totalPoints`=100, `level`=1（青铜）

### Step 4: 扣 30 积分

```http
POST /api/member/points/deduct?userId=20001&points=30
```

### Step 5: 验证扣减

```http
GET /api/member/20001
```
> 验证: `points`=70, `totalPoints`=100（总积分不变）

### Step 6: 加 950 积分（触发升级）

```http
POST /api/member/points/add?userId=20001&points=950
```

### Step 6b: 验证升级

```http
GET /api/member/20001
```
> 验证: `points`=1020, `totalPoints`=1050, `level`=2（白银，>=1000）

### Step 7 (异常): 超额扣减

```http
POST /api/member/points/deduct?userId=20001&points=99999
```
> 验证: 积分不变，仍为 1020
