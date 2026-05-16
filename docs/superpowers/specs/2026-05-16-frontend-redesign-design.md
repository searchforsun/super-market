# 前端设计全面优化 — 方案设计

> 2026-05-16 | 基于 [frontend-design-audit.md](../../frontend-design-audit.md) 审计报告
> 三应用同步重设计：app-b2c、app-b2b、app-admin

---

## 一、问题回顾

| 问题 | 严重程度 | 状态 |
|------|---------|------|
| 三个 app 的 `design.css` 完全一样（逐字复制，注释均写 `B2C Design System`） | P0 | 解决 |
| `Inter` 字体被 frontend-design 规范明确列为应避免的泛化字体 | P0 | 解决 |
| 全代码库仅 1 处 `@keyframes`，几乎无动效 | P1 | 解决 |
| 布局常规、背景单调，缺少氛围营造 | P2 | 解决 |
| 三应用零差异化，不同用户角色共用同一套设计语言 | P0 | 解决 |

---

## 二、设计总览：方案 A — 差异化专业风格

| 维度 | B2C 商城 | B2B 商家后台 | Admin 运营后台 |
|------|---------|-------------|---------------|
| **调性** | 编辑级零售美学 | 工业工具 | 极简权威 |
| **主题支持** | 仅浅色 | 浅色 + 深色切换 | 浅色 + 深色切换 |
| **展示字体** | Playfair Display | Geist | Geist |
| **正文字体** | DM Sans | Geist Mono (数据区) | Geist |
| **动效** | page-enter + 路由过渡 + scroll-reveal | page-enter + 路由过渡 | page-enter + 路由过渡 |
| **氛围** | 噪点纹理 + 渐变装饰 | 几何网格 + 数据工业感 | 大留白 + 几乎无装饰 |

---

## 三、色系系统

### 3.1 B2C — 编辑级零售美学（仅浅色）

| Token | 值 | 用途 |
|-------|-----|------|
| `--color-bg` | `#faf7f2` | 页面底色（暖奶油） |
| `--color-surface` | `#ffffff` | 卡片/面板 |
| `--color-surface-hover` | `#f5f1ea` | 面板 hover |
| `--color-text-primary` | `#1c1917` | 主文字 |
| `--color-text-secondary` | `#6b625a` | 次要文字 |
| `--color-text-muted` | `#a09890` | 弱化文字 |
| `--color-text-inverse` | `#ffffff` | 反色文字 |
| `--color-accent` | `#b91c1c` | 主强调色（波尔多红） |
| `--color-accent-hover` | `#991b1b` | hover 加深 |
| `--color-accent-light` | `#fef2f2` | 红底淡色 |
| `--color-accent-soft` | `#fce4e8` | 更淡装饰 |
| `--color-border` | `#e7e2d8` | 边框 |
| `--color-border-light` | `#f0ece5` | 浅边框 |
| `--color-success` | `#2d6a4f` | 成功 |
| `--color-warning` | `#d97706` | 警告 |
| `--color-info` | `#457b9d` | 信息 |

### 3.2 B2B — 工业工具（浅色 + 深色）

**浅色主题（默认）：**

| Token | 值 | 用途 |
|-------|-----|------|
| `--color-bg` | `#f5f5f0` | 页面底色（石灰白） |
| `--color-surface` | `#ffffff` | 卡片/面板 |
| `--color-surface-hover` | `#f0efe8` | 面板 hover |
| `--color-text-primary` | `#1a1a18` | 主文字 |
| `--color-text-secondary` | `#5c5a55` | 次要文字 |
| `--color-text-muted` | `#8c8a83` | 弱化文字 |
| `--color-text-inverse` | `#ffffff` | 反色文字 |
| `--color-accent` | `#d97706` | 主强调色（琥珀） |
| `--color-accent-hover` | `#b45309` | hover 加深 |
| `--color-accent-light` | `#fffbeb` | 琥珀淡色 |
| `--color-accent-soft` | `#fef3c7` | 更淡装饰 |
| `--color-border` | `#e0ded6` | 边框 |
| `--color-border-light` | `#edebe3` | 浅边框 |
| `--color-success` | `#059669` | 成功 |
| `--color-warning` | `#d97706` | 警告 |
| `--color-info` | `#0284c7` | 信息 |

**深色主题（`[data-theme="dark"]`）：**

| Token | 值 |
|-------|-----|
| `--color-bg` | `#1a1a18` |
| `--color-surface` | `#242422` |
| `--color-surface-hover` | `#2e2e2a` |
| `--color-text-primary` | `#ededeb` |
| `--color-text-secondary` | `#9c9a93` |
| `--color-text-muted` | `#6c6a63` |
| `--color-text-inverse` | `#1a1a18` |
| `--color-accent` | `#f59e0b` |
| `--color-accent-hover` | `#d97706` |
| `--color-accent-light` | `#422006` |
| `--color-accent-soft` | `#331a04` |
| `--color-border` | `#33312c` |
| `--color-border-light` | `#2a2824` |
| `--color-success` | `#34d399` |
| `--color-warning` | `#fbbf24` |
| `--color-info` | `#38bdf8` |

### 3.3 Admin — 极简权威（浅色 + 深色）

**浅色主题（默认）：**

| Token | 值 | 用途 |
|-------|-----|------|
| `--color-bg` | `#ffffff` | 页面底色（纯白） |
| `--color-surface` | `#f8fafc` | 卡片/面板 |
| `--color-surface-hover` | `#f1f5f9` | 面板 hover |
| `--color-text-primary` | `#0f172a` | 主文字 |
| `--color-text-secondary` | `#475569` | 次要文字 |
| `--color-text-muted` | `#94a3b8` | 弱化文字 |
| `--color-text-inverse` | `#ffffff` | 反色文字 |
| `--color-accent` | `#2563eb` | 主强调色（钢蓝） |
| `--color-accent-hover` | `#1d4ed8` | hover 加深 |
| `--color-accent-light` | `#eff6ff` | 蓝底淡色 |
| `--color-accent-soft` | `#dbeafe` | 更淡装饰 |
| `--color-border` | `#e2e8f0` | 边框 |
| `--color-border-light` | `#f1f5f9` | 浅边框 |
| `--color-success` | `#059669` | 成功 |
| `--color-warning` | `#d97706` | 警告 |
| `--color-info` | `#2563eb` | 信息 |

**深色主题（`[data-theme="dark"]`）：**

| Token | 值 |
|-------|-----|
| `--color-bg` | `#0f172a` |
| `--color-surface` | `#1e293b` |
| `--color-surface-hover` | `#334155` |
| `--color-text-primary` | `#f1f5f9` |
| `--color-text-secondary` | `#94a3b8` |
| `--color-text-muted` | `#64748b` |
| `--color-text-inverse` | `#0f172a` |
| `--color-accent` | `#3b82f6` |
| `--color-accent-hover` | `#2563eb` |
| `--color-accent-light` | `#1e3a5f` |
| `--color-accent-soft` | `#172554` |
| `--color-border` | `#334155` |
| `--color-border-light` | `#1e293b` |
| `--color-success` | `#34d399` |
| `--color-warning` | `#fbbf24` |
| `--color-info` | `#60a5fa` |

---

## 四、字体系统

### 4.1 B2C — 编辑级

| 用途 | 字体 | 引入方式 |
|------|------|---------|
| 展示标题 | `Playfair Display` | Google Fonts（现有，保留） |
| 正文/UI | `DM Sans` | Google Fonts（替换 Inter） |

**CSS 变量：**
```css
--font-display: 'Playfair Display', Georgia, 'Times New Roman', serif;
--font-body: 'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
```

### 4.2 B2B — 工业工具

| 用途 | 字体 | 引入方式 |
|------|------|---------|
| 全局正文 | `Geist` | Google Fonts（替换 Inter） |
| 数据/代码 | `Geist Mono` | Google Fonts（用于价格、库存、订单号等数据展示） |

**CSS 变量：**
```css
--font-body: 'Geist', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
--font-mono: 'Geist Mono', 'Courier New', monospace;
```

### 4.3 Admin — 极简权威

| 用途 | 字体 | 引入方式 |
|------|------|---------|
| 全局 | `Geist` | Google Fonts（替换 Inter） |

**CSS 变量：**
```css
--font-body: 'Geist', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
--font-mono: 'Geist Mono', 'Courier New', monospace;
```

---

## 五、主题切换机制（B2B + Admin）

### 5.1 切换方式

- Header 右侧放置 `<ThemeToggle />` 图标按钮
- 点击切换 `<html>` 元素上的 `data-theme` 属性
- 持久化：`localStorage.setItem('theme', ...)`，页面加载时读取

### 5.2 CSS 组织

各 app 的 `design.css` 中，`:root` 定义默认（浅色）变量，`[data-theme="dark"]` 覆盖深色变量。

### 5.3 涉及文件

| 文件 | 改动 |
|------|------|
| `packages/ui/src/ThemeToggle.vue` | 新增共享组件 |
| `packages/ui/src/index.ts` | 导出 ThemeToggle |
| `app-b2b/src/styles/design.css` | 添加 `[data-theme="dark"]` 块 |
| `app-b2b/src/layouts/Default.vue` | header 引入 ThemeToggle |
| `app-admin/src/styles/design.css` | 添加 `[data-theme="dark"]` 块 |
| `app-admin/src/layouts/Default.vue` | header 引入 ThemeToggle |

---

## 六、动效系统

### 6.1 页面加载 — Staggered Reveal

每个页面根容器加 `class="page-enter"`，子元素渐显：

```css
.page-enter > * {
  opacity: 0;
  transform: translateY(16px);
  animation: fadeUp 0.5s var(--ease-out) forwards;
}
.page-enter > *:nth-child(1) { animation-delay: 0s; }
.page-enter > *:nth-child(2) { animation-delay: 0.08s; }
.page-enter > *:nth-child(3) { animation-delay: 0.16s; }
/* 依此类推 */

@keyframes fadeUp {
  to { opacity: 1; transform: translateY(0); }
}
```

**应用范围：** 所有页面的根 `<div>` 加 `class="page-enter"`。

### 6.2 路由过渡

三个 app 的 `App.vue` 中加 `<Transition>` 包裹 `<router-view>`：

```html
<router-view v-slot="{ Component }">
  <Transition name="route" mode="out-in">
    <component :is="Component" />
  </Transition>
</router-view>
```

```css
.route-enter-active, .route-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.route-enter-from { opacity: 0; transform: translateY(8px); }
.route-leave-to { opacity: 0; transform: translateY(-8px); }
```

### 6.3 滚动渐显 — Scroll Reveal

`packages/utils` 中新增 `useRevealOnScroll` composable：

```ts
// IntersectionObserver 包装
// 元素进入视口时加 .revealed class
// CSS: .reveal-on-scroll { opacity: 0; transform: translateY(12px); transition: ... }
//       .reveal-on-scroll.revealed { opacity: 1; transform: translateY(0); }
```

**应用范围：** ProductCard 网格、订单/商品列表等滚动区域（B2C + B2B）。

### 6.4 微交互深化

- StatCard 数字进入动画（从 0 递增）
- 卡片 hover：`translateY(-4px)` + shadow 提升（现有 ProductCard 已有，统一到 DataTable、StatCard）

### 6.5 动效分布

| 动效 | B2C | B2B | Admin |
|------|:---:|:---:|:-----:|
| page-enter | ✓ | ✓ | ✓ |
| 路由过渡 | ✓ | ✓ | ✓ |
| scroll-reveal | ✓ | ✓ | — |
| 微交互 | ✓ | ✓ | — |

---

## 七、氛围与装饰

### 7.1 B2C — 编辑级零售氛围

- **噪点纹理：** `body::before` 叠加极淡 SVG noise 滤镜，给暖奶油底色增加纸张质感
- **Hero 深化：** 对角线渐变 + 错位几何色块装饰（替换当前纯 dot pattern）
- **Footer 顶部线：** `linear-gradient` 装饰分隔线，取代纯色 `border-top`
- **自定义光标：** 可点击卡片使用自定义 cursor

### 7.2 B2B — 工业工具感

- **侧边栏顶部：** 细线条几何装饰（纯 CSS `background-image` 图案）
- **内容区网格底纹：** 看板/空状态页加极淡网格 `background-size: 24px 24px`
- **数据卡片：** StatCard 左上角 `:before` 伪元素 3px 宽强调色条

### 7.3 Admin — 极简克制

- **加大留白：** sidebar 宽度 200px → 220px，内容区 max-width 收紧
- **分割线降级：** 边框减细、透明度降低
- **无装饰纹理：** 仅路由过渡 + StatCard 渐显

---

## 八、实施计划

### 阶段拆分

| 阶段 | 内容 | 涉及文件数 | 预计工作量 |
|------|------|-----------|-----------|
| **S1** | 三套 `design.css` 重写（色系 + 字体 + 动效基础 + 主题切换） | 6 | 核心 |
| **S2** | `App.vue` 路由过渡 + `Default.vue` 布局适配 + ThemeToggle | 6 | 核心 |
| **S3** | 共享组件动效（ProductCard、StatCard、DataTable、Card hover） | ~6 | UI 包 |
| **S4** | 所有页面加 `page-enter`（B2C 15页 + B2B 12页 + Admin 8页） | ~35 | 批量 |
| **S5** | B2C 氛围（噪点纹理、hero 深化、footer 装饰） | ~3 | 细节 |
| **S6** | B2B 氛围（侧边栏装饰、网格底纹、StatCard 色条） | ~4 | 细节 |

### 涉及文件汇总

```
frontend/
├── app-b2c/src/
│   ├── styles/design.css          # 重写：色系 + 字体 + 动效
│   ├── App.vue                    # 加路由 Transition
│   ├── layouts/Default.vue        # 氛围装饰样式
│   └── pages/**/*.vue            # 各页加 page-enter
├── app-b2b/src/
│   ├── styles/design.css          # 重写：浅色 + 深色主题
│   ├── App.vue                    # 加路由 Transition
│   ├── layouts/Default.vue        # header 加 ThemeToggle + 装饰
│   └── pages/**/*.vue            # 各页加 page-enter
├── app-admin/src/
│   ├── styles/design.css          # 重写：浅色 + 深色主题
│   ├── App.vue                    # 加路由 Transition
│   ├── layouts/Default.vue        # header 加 ThemeToggle + 布局调整
│   └── pages/**/*.vue            # 各页加 page-enter
└── packages/
    ├── ui/src/
    │   ├── ThemeToggle.vue         # 新增
    │   ├── index.ts                # 导出 ThemeToggle
    │   ├── StatCard.vue            # 左上角色条 + 数字动画
    │   ├── ProductCard.vue         # 微调 hover
    │   └── DataTable.vue           # 行 hover 微交互
    └── utils/src/
        └── useRevealOnScroll.ts    # 新增
```

---

## 九、验收标准

- [ ] B2C design.css 重写，字体从 Inter → DM Sans，色板使用新暖奶油 + 波尔多红
- [ ] B2B design.css 重写，字体从 Inter → Geist，支持浅/深主题切换
- [ ] Admin design.css 重写，字体从 Inter → Geist，支持浅/深主题切换
- [ ] 三个 design.css 互不相同，各自有独立色系和设计语言
- [ ] ThemeToggle 组件可切换主题并持久化到 localStorage
- [ ] 路由切换有过渡动画（三个 app 均生效）
- [ ] 页面首次加载有 staggered reveal 效果
- [ ] B2C hero 区域有噪点纹理氛围
- [ ] B2B StatCard 有强调色条
- [ ] `pnpm build` 三个 app 构建通过
