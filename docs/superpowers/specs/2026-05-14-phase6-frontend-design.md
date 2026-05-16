# Phase 6 — 前端应用 方案设计

> 2026-05-14 | 基于 PRD · 18 后端微服务全部就绪

## 一、总览

### 1.1 三个前端应用

| 应用 | 中文名称 | 色系 | 页面数 | 路由前缀 |
|------|----------|------|--------|----------|
| app-b2c | 用户前台 | 京东红 #e1251b | 15 | `/` |
| app-b2b | 商家后台 | 商务蓝 #3b82f6 | 12 | `/merchant` |
| app-admin | 运营后台 | 紫晶 #8b5cf6 | 8 | `/admin` |

### 1.2 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.x | 前端框架 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.6.x | UI 组件库 |
| Pinia | 2.1.x | 状态管理 |
| Vue Router | 4.3.x | 路由管理 |
| Axios | 1.6.x | HTTP 客户端 |
| pnpm | 9.x | 包管理器 + Workspace |

### 1.3 Monorepo 结构

```
frontend/
├── pnpm-workspace.yaml          # pnpm workspace 配置
├── package.json                  # 根 package.json (scripts)
├── .npmrc                        # pnpm 配置
├── packages/                     # 公共包
│   ├── ui/                       # 共享组件库 (10 个组件)
│   │   ├── ProductCard.vue
│   │   ├── SearchBar.vue
│   │   ├── OrderStatusTag.vue
│   │   ├── PriceDisplay.vue
│   │   ├── SkuSelector.vue
│   │   ├── RatingStars.vue
│   │   ├── DataTable.vue
│   │   ├── StatCard.vue
│   │   ├── FileUploader.vue
│   │   └── Countdown.vue
│   ├── api/                      # Axios 封装 + API 接口
│   │   ├── request.ts            # Axios 实例 (拦截器/Token)
│   │   ├── modules/              # 按域拆分
│   │   │   ├── user.ts
│   │   │   ├── product.ts
│   │   │   ├── order.ts
│   │   │   ├── cart.ts
│   │   │   ├── payment.ts
│   │   │   ├── coupon.ts
│   │   │   ├── seckill.ts
│   │   │   ├── search.ts
│   │   │   ├── review.ts
│   │   │   └── platform.ts
│   │   └── index.ts
│   ├── stores/                   # Pinia 全局状态
│   │   ├── useUserStore.ts       # 用户登录/Token
│   │   └── useCartStore.ts       # 购物车数量
│   └── utils/                    # 工具函数
│       ├── constants.ts          # 全局常量
│       └── format.ts             # 格式化 (金额/日期)
├── app-b2c/                      # 用户前台
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── router/index.ts
│       ├── layouts/Default.vue
│       └── pages/ (15 pages)
├── app-b2b/                      # 商家后台
│   └── ... (同上结构)
└── app-admin/                    # 运营后台
    └── ... (同上结构)
```

---

## 二、前端开发阶段拆分

### Phase 6A: B2C 用户前台（~15 页）

> 目标：完成核心交易闭环，B2C 用户可完成从浏览到下单的完整流程

| # | 页面 | 路由 | 后端 API | 复杂度 |
|---|------|------|----------|--------|
| 1 | 首页 | `/` | banners, category/tree, product/list, seckill/sessions | ⭐⭐⭐ |
| 2 | 商品搜索/列表 | `/search?q=` | search/product, category/tree | ⭐⭐⭐ |
| 3 | 商品详情 | `/product/:id` | product/spu/{id}, skus, review/list, rating | ⭐⭐⭐⭐ |
| 4 | 购物车 | `/cart` | cart/list, add, update, select, delete | ⭐⭐ |
| 5 | 确认订单 | `/order/confirm` | address/list, coupon/available, cart/selected | ⭐⭐⭐ |
| 6 | 下单结果 | `/order/result` | order/create, payment/pay | ⭐⭐ |
| 7 | 我的订单 | `/user/orders` | order/list/user, cancel | ⭐⭐ |
| 8 | 订单详情 | `/order/:no` | order/{no}, review (POST) | ⭐⭐ |
| 9 | 登录/注册 | `/login` | user/register, auth/login | ⭐ |
| 10 | 个人中心 | `/user/center` | user/info, member/{id} | ⭐ |
| 11 | 收货地址 | `/user/addresses` | address CRUD | ⭐ |
| 12 | 我的优惠券 | `/user/coupons` | coupon/my | ⭐ |
| 13 | 我的评价 | `/user/reviews` | review/list/user | ⭐ |
| 14 | 秒杀会场 | `/seckill` | seckill/sessions, products, execute | ⭐⭐⭐⭐ |
| 15 | 通知中心 | `/user/notifications` | notify/list, unread-count, read | ⭐ |

### Phase 6B: B2B 商家后台（~12 页）

| # | 页面 | 路由 | 后端 API |
|---|------|------|----------|
| 1 | 商家首页/看板 | `/merchant/dashboard` | order/list/shop, product/list/shop |
| 2 | 商品管理 | `/merchant/products` | product/spu, product/list/shop |
| 3 | 商品发布 | `/merchant/products/create` | product/spu, file/upload, category/tree |
| 4 | 库存管理 | `/merchant/inventory` | inventory/init, inventory/sku/{id} |
| 5 | 订单处理 | `/merchant/orders` | order/list/shop, order/{no}/ship |
| 6 | 订单详情 | `/merchant/orders/:no` | order/{no} |
| 7 | 店铺设置 | `/merchant/shop/settings` | shop/{id} |
| 8 | 优惠券管理 | `/merchant/coupons` | coupon/admin/template, coupon/admin/distribute |
| 9 | 秒杀活动 | `/merchant/seckill` | seckill/admin/session, seckill/admin/product |
| 10 | 评价管理 | `/merchant/reviews` | review/list/spu, review/{id}/reply |
| 11 | 通知中心 | `/merchant/notifications` | notify/list |
| 12 | 结算查询 | `/merchant/settlement` | （预留） |

### Phase 6C: Admin 运营后台（~8 页）

| # | 页面 | 路由 | 后端 API |
|---|------|------|----------|
| 1 | 运营看板 | `/admin/dashboard` | report/gmv, report/orders, report/users |
| 2 | 商家审核 | `/admin/merchants` | shop/merchant/apply, audit |
| 3 | 商品审核 | `/admin/products/audit` | product/spu/{id}/audit |
| 4 | 类目管理 | `/admin/categories` | category CRUD, category/tree |
| 5 | Banner/广告位 | `/admin/banners` | platform/admin/banner CRUD, file/upload |
| 6 | 风控管理 | `/admin/risk` | platform/admin/risk/rules, risk/logs |
| 7 | 数据报表 | `/admin/reports` | platform/admin/report/* |
| 8 | 通知模板 | `/admin/notify-templates` | notify/admin/template |

---

## 三、B2C 京东风格布局设计

### 3.1 全局布局 (Default.vue)

```
┌──────────────────────────────────────────────┐
│ 顶部工具栏: 你好请登录 | 我的订单 | 客户服务    │  bg: #2d2d2d
├──────────────────────────────────────────────┤
│ Logo | 🔍 搜索框 (flex) | 🛒 购物车           │  bg: #e1251b
├──────────────────────────────────────────────┤
│ 全部商品分类 | 服装 | 家电 | 超市 | 手机...     │  bg: #e1251b
├──────────────────────────────────────────────┤
│                                              │
│              <router-view />                  │
│                                              │
├──────────────────────────────────────────────┤
│ 底部: 关于我们 | 帮助中心 | 版权信息            │  bg: #f5f5f5
└──────────────────────────────────────────────┘
```

### 3.2 配色体系

| 用途 | 颜色 | CSS 变量 |
|------|------|----------|
| 品牌主色 | `#e1251b` | `--color-primary` |
| 促销红 | `#c81623` | `--color-danger` |
| 价格红 | `#f30213` | `--color-price` |
| 成功绿 | `#10b981` | `--color-success` |
| 文字主色 | `#333333` | `--text-primary` |
| 文字次要 | `#999999` | `--text-secondary` |
| 背景灰 | `#f4f4f4` | `--bg-page` |

### 3.3 关键页面交互

**商品详情页**：SPU 主图轮播 → SKU 选择器（规格联动） → 价格实时切换 → 加入购物车/立即购买

**秒杀会场**：倒计时组件 → Redis Lua 秒杀按钮（防重复点击） → 结果模态框（成功/失败/已抢光）

**购物车**：全选/单选 → 实时价格汇总 → 删除确认 → 去结算

---

## 四、pnpm 依赖管理

### pnpm-workspace.yaml

```yaml
packages:
  - 'packages/*'
  - 'app-b2c'
  - 'app-b2b'
  - 'app-admin'
```

### 公共依赖版本（根 package.json）

```json
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "axios": "^1.6.0",
    "element-plus": "^2.6.0",
    "@element-plus/icons-vue": "^2.3.0"
  },
  "devDependencies": {
    "vite": "^5.0.0",
    "@vitejs/plugin-vue": "^5.0.0",
    "typescript": "^5.3.0",
    "vue-tsc": "^2.0.0",
    "unplugin-auto-import": "^0.17.0",
    "unplugin-vue-components": "^0.26.0",
    "sass": "^1.70.0"
  }
}
```

---

## 五、验收标准

- [ ] pnpm monorepo 搭建完成，3 个 app + 4 个公共包可正常引用
- [ ] B2C 首页 + 搜索 + 商品详情 + 购物车 + 下单完整链路可走通
- [ ] B2B 商品管理 + 订单处理 + 发货流程可走通
- [ ] Admin 审核 + Banner + 报表可走通
- [ ] 所有页面响应式适配（PC 端 1200px+）
- [ ] Axios 拦截器正确注入 Token + 处理 401
- [ ] 三端独立打包，`pnpm build` 通过

---

## 六、开发阶段汇总

| Phase | 名称 | 页面 | 依赖 |
|-------|------|------|------|
| **Phase 6A** | B2C 用户前台 | 15 页 | packages/ui, packages/api, packages/stores |
| **Phase 6B** | B2B 商家后台 | 12 页 | packages/ui, packages/api |
| **Phase 6C** | Admin 运营后台 | 8 页 | packages/ui, packages/api |
