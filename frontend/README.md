# Super Market 前端

基于 pnpm monorepo 的三端前端应用，共享组件库和 API 层。

## 技术栈

| 技术 | 版本 |
|------|------|
| Vue | 3.4.x |
| Vite | 5.x |
| Element Plus | 2.6.x |
| Pinia | 2.1.x |
| Vue Router | 4.3.x |
| Axios | 1.6.x |
| TypeScript | 5.x |
| pnpm | 9.x |

## 项目结构

```
frontend/
├── packages/
│   ├── ui/         # 10 个共享组件
│   ├── api/        # Axios 封装 + 14 个 API 模块
│   ├── stores/     # useUserStore / useCartStore
│   └── utils/      # 常量、格式化工具
├── app-b2c/        # 用户前台 (15 页)
├── app-b2b/        # 商家后台 (12 页)
└── app-admin/      # 运营后台 (8 页)
```

## 快速开始

### 前置条件

- Node.js >= 18
- pnpm >= 9 (`npm i -g pnpm`)
- 后端服务已启动（Gateway :8999 + 中间件 middleware-docker）

### 安装依赖

```bash
cd frontend
pnpm install
```

### 启动开发服务器

```bash
# 用户前台 (B2C) — http://localhost:5173
pnpm dev:b2c

# 商家后台 (B2B) — http://localhost:5174
pnpm dev:b2b

# 运营后台 (Admin) — http://localhost:5175
pnpm dev:admin
```

也可以进入各 app 目录直接启动：

```bash
cd app-b2c && pnpm dev
cd app-b2b && pnpm dev
cd app-admin && pnpm dev
```

### 构建生产版本

```bash
pnpm build:b2c       # 输出到 app-b2c/dist/
pnpm build:b2b       # 输出到 app-b2b/dist/
pnpm build:admin     # 输出到 app-admin/dist/
pnpm build           # 构建全部
```

## API 代理

开发模式下所有 `/api/*` 请求自动代理到 `http://localhost:8999`（Spring Cloud Gateway）。

配置在各 app 的 `vite.config.ts` 中：

```ts
server: {
  proxy: { '/api': 'http://localhost:8999' }
}
```

## 三端差异

| | B2C 用户前台 | B2B 商家后台 | Admin 运营后台 |
|------|-------------|-------------|---------------|
| 端口 | 5173 | 5174 | 5175 |
| 色系 | 京东红 #e1251b | 商务蓝 #3b82f6 | 紫晶 #8b5cf6 |
| 页面数 | 15 | 12 | 8 |
| 登录要求 | 下单/个人中心需登录 | 全部需登录 | 全部需登录 |

## 开发说明

- 公共组件在 `packages/ui/` 修改，所有 app 自动生效
- API 模块在 `packages/api/src/modules/` 增加，按业务域拆分
- 全局状态在 `packages/stores/` 管理
- 新页面在各 app 的 `src/pages/` 下创建，并注册路由
