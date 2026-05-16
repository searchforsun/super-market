# 前端设计全面优化 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将三个前端应用从共享同一套 B2C 设计系统重构为三套独立的差异化设计系统（B2C 编辑级零售 / B2B 工业工具 / Admin 极简权威），替换字体、添加动效、支持主题切换。

**Architecture:** 每个 app 拥有独立的 `design.css`（色系 + 字体 + 动效 CSS 变量），共享 UI 包新增 ThemeToggle 组件，所有页面根元素加 `page-enter` 实现 staggered reveal，B2B/Admin 通过 `data-theme` 属性支持浅深切换。

**Tech Stack:** Vue 3 + Element Plus + pnpm monorepo

---

## 文件结构

```
frontend/
├── app-b2c/src/
│   ├── styles/design.css          # 重写：暖奶油 + 波尔多红，仅浅色
│   ├── App.vue                    # 加 <Transition> 路由过渡
│   ├── layouts/Default.vue        # 噪点纹理 + footer 分隔线装饰
│   └── pages/**/*.vue            # 16 页加 page-enter
├── app-b2b/src/
│   ├── styles/design.css          # 重写：石灰白/石板黑 + 琥珀，浅深双主题
│   ├── App.vue                    # 加 <Transition> 路由过渡
│   ├── layouts/Default.vue        # ThemeToggle + 侧边栏装饰 + 网格底纹
│   └── pages/**/*.vue            # 14 页加 page-enter
├── app-admin/src/
│   ├── styles/design.css          # 重写：纯白/深海蓝 + 钢蓝，浅深双主题
│   ├── App.vue                    # 加 <Transition> 路由过渡
│   ├── layouts/Default.vue        # ThemeToggle + sidebar 加宽
│   └── pages/**/*.vue            # 9 页加 page-enter
└── packages/
    ├── ui/src/
    │   ├── ThemeToggle.vue         # 新增：☀/🌙 切换按钮
    │   ├── index.ts                # 导出 ThemeToggle
    │   ├── StatCard.vue            # 左上角强调色条
    │   └── DataTable.vue           # 行 hover 微交互
    └── utils/src/
        └── useRevealOnScroll.ts    # 新增：IntersectionObserver composable
```

---

### Task 1: B2C design.css 重写

**Files:**
- Modify: `frontend/app-b2c/src/styles/design.css`

- [ ] **Step 1: 替换 design.css 完整内容**

将当前文件内容替换为以下完整 CSS：

```css
/* ===== Super Market B2C — Editorial Retail Aesthetic ===== */

@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,500;0,600;0,700;1,500&family=DM+Sans:wght@400;500;600;700&display=swap');

:root {
  /* Typography */
  --font-display: 'Playfair Display', Georgia, 'Times New Roman', serif;
  --font-body: 'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;

  /* Palette — warm cream + charcoal + bordeaux red */
  --color-bg: #faf7f2;
  --color-surface: #ffffff;
  --color-surface-hover: #f5f1ea;
  --color-border: #e7e2d8;
  --color-border-light: #f0ece5;

  --color-text-primary: #1c1917;
  --color-text-secondary: #6b625a;
  --color-text-muted: #a09890;
  --color-text-inverse: #ffffff;

  --color-accent: #b91c1c;
  --color-accent-hover: #991b1b;
  --color-accent-light: #fef2f2;
  --color-accent-soft: #fce4e8;

  --color-success: #2d6a4f;
  --color-warning: #d97706;
  --color-info: #457b9d;

  /* Spacing */
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 16px;
  --space-lg: 24px;
  --space-xl: 32px;
  --space-2xl: 48px;
  --space-3xl: 64px;
  --space-4xl: 96px;

  /* Layout */
  --max-width: 1280px;
  --header-height: 72px;
  --nav-height: 44px;

  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(28, 25, 23, 0.04);
  --shadow-md: 0 4px 12px rgba(28, 25, 23, 0.06);
  --shadow-lg: 0 12px 40px rgba(28, 25, 23, 0.08);
  --shadow-xl: 0 24px 64px rgba(28, 25, 23, 0.12);
  --shadow-card-hover: 0 8px 30px rgba(28, 25, 23, 0.10);

  /* Borders */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-full: 9999px;

  /* Transitions */
  --ease-out: cubic-bezier(0.25, 0.46, 0.45, 0.94);
  --ease-in-out: cubic-bezier(0.65, 0, 0.35, 1);
  --duration-fast: 150ms;
  --duration-base: 250ms;
  --duration-slow: 400ms;
}

/* ===== Global Reset & Base ===== */
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

html {
  font-size: 16px;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

body {
  font-family: var(--font-body);
  color: var(--color-text-primary);
  background: var(--color-bg);
  line-height: 1.6;
  overflow-x: hidden;
}

a { color: inherit; text-decoration: none; }
img { max-width: 100%; display: block; }
button { font-family: inherit; cursor: pointer; border: none; background: none; }

/* ===== Utility Classes ===== */
.sr-only {
  position: absolute; width: 1px; height: 1px;
  padding: 0; margin: -1px; overflow: hidden;
  clip: rect(0,0,0,0); border: 0;
}

.container { max-width: var(--max-width); margin: 0 auto; padding: 0 var(--space-lg); }

/* ===== Scrollbar ===== */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--color-border); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--color-text-muted); }

/* ===== Page Enter Animation ===== */
.page-enter > * {
  opacity: 0;
  transform: translateY(16px);
  animation: fadeUp 0.5s var(--ease-out) forwards;
}
.page-enter > *:nth-child(1) { animation-delay: 0s; }
.page-enter > *:nth-child(2) { animation-delay: 0.06s; }
.page-enter > *:nth-child(3) { animation-delay: 0.12s; }
.page-enter > *:nth-child(4) { animation-delay: 0.18s; }
.page-enter > *:nth-child(5) { animation-delay: 0.24s; }
.page-enter > *:nth-child(6) { animation-delay: 0.30s; }
.page-enter > *:nth-child(7) { animation-delay: 0.36s; }
.page-enter > *:nth-child(8) { animation-delay: 0.42s; }

@keyframes fadeUp {
  to { opacity: 1; transform: translateY(0); }
}

/* ===== Noise Texture Overlay ===== */
body::before {
  content: '';
  position: fixed; inset: 0; z-index: 9999;
  pointer-events: none;
  opacity: 0.025;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}
```

- [ ] **Step 2: 验证文件写入成功**

```bash
wc -l frontend/app-b2c/src/styles/design.css
```

- [ ] **Step 3: Commit**

```bash
git add frontend/app-b2c/src/styles/design.css
git commit -m "feat: rewrite B2C design system — warm cream + bordeaux red, DM Sans font, page-enter animation, noise texture"
```

---

### Task 2: B2B design.css 重写

**Files:**
- Modify: `frontend/app-b2b/src/styles/design.css`

- [ ] **Step 1: 替换 design.css 完整内容**

```css
/* ===== Super Market B2B — Industrial Tool Aesthetic ===== */

@import url('https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&family=Geist+Mono:wght@400;500;600&display=swap');

:root {
  /* Typography */
  --font-body: 'Geist', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-mono: 'Geist Mono', 'Courier New', monospace;

  /* Palette — limestone white / slate black + amber */
  --color-bg: #f5f5f0;
  --color-surface: #ffffff;
  --color-surface-hover: #f0efe8;
  --color-border: #e0ded6;
  --color-border-light: #edebe3;

  --color-text-primary: #1a1a18;
  --color-text-secondary: #5c5a55;
  --color-text-muted: #8c8a83;
  --color-text-inverse: #ffffff;

  --color-accent: #d97706;
  --color-accent-hover: #b45309;
  --color-accent-light: #fffbeb;
  --color-accent-soft: #fef3c7;

  --color-success: #059669;
  --color-warning: #d97706;
  --color-info: #0284c7;

  /* Spacing */
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 16px;
  --space-lg: 24px;
  --space-xl: 32px;
  --space-2xl: 48px;
  --space-3xl: 64px;
  --space-4xl: 96px;

  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(26, 26, 24, 0.04);
  --shadow-md: 0 4px 12px rgba(26, 26, 24, 0.06);
  --shadow-lg: 0 12px 40px rgba(26, 26, 24, 0.08);
  --shadow-xl: 0 24px 64px rgba(26, 26, 24, 0.12);

  /* Borders */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-full: 9999px;

  /* Transitions */
  --ease-out: cubic-bezier(0.25, 0.46, 0.45, 0.94);
  --ease-in-out: cubic-bezier(0.65, 0, 0.35, 1);
  --duration-fast: 150ms;
  --duration-base: 250ms;
  --duration-slow: 400ms;
}

/* ===== Dark Theme ===== */
[data-theme="dark"] {
  --color-bg: #1a1a18;
  --color-surface: #242422;
  --color-surface-hover: #2e2e2a;
  --color-border: #33312c;
  --color-border-light: #2a2824;

  --color-text-primary: #ededeb;
  --color-text-secondary: #9c9a93;
  --color-text-muted: #6c6a63;
  --color-text-inverse: #1a1a18;

  --color-accent: #f59e0b;
  --color-accent-hover: #d97706;
  --color-accent-light: #422006;
  --color-accent-soft: #331a04;

  --color-success: #34d399;
  --color-warning: #fbbf24;
  --color-info: #38bdf8;

  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.15);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.2);
  --shadow-lg: 0 12px 40px rgba(0, 0, 0, 0.3);
}

/* ===== Global Reset & Base ===== */
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

html {
  font-size: 16px;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

body {
  font-family: var(--font-body);
  color: var(--color-text-primary);
  background: var(--color-bg);
  line-height: 1.6;
  overflow-x: hidden;
}

a { color: inherit; text-decoration: none; }
img { max-width: 100%; display: block; }
button { font-family: inherit; cursor: pointer; border: none; background: none; }

/* ===== Utility Classes ===== */
.sr-only {
  position: absolute; width: 1px; height: 1px;
  padding: 0; margin: -1px; overflow: hidden;
  clip: rect(0,0,0,0); border: 0;
}

.container { max-width: 1280px; margin: 0 auto; padding: 0 var(--space-lg); }

/* ===== Scrollbar ===== */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--color-border); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--color-text-muted); }

/* ===== Page Enter Animation ===== */
.page-enter > * {
  opacity: 0;
  transform: translateY(16px);
  animation: fadeUp 0.5s var(--ease-out) forwards;
}
.page-enter > *:nth-child(1) { animation-delay: 0s; }
.page-enter > *:nth-child(2) { animation-delay: 0.06s; }
.page-enter > *:nth-child(3) { animation-delay: 0.12s; }
.page-enter > *:nth-child(4) { animation-delay: 0.18s; }
.page-enter > *:nth-child(5) { animation-delay: 0.24s; }
.page-enter > *:nth-child(6) { animation-delay: 0.30s; }
.page-enter > *:nth-child(7) { animation-delay: 0.36s; }
.page-enter > *:nth-child(8) { animation-delay: 0.42s; }

@keyframes fadeUp {
  to { opacity: 1; transform: translateY(0); }
}

/* ===== Geometric Grid Texture (for dashboard pages) ===== */
.bg-grid {
  background-image:
    linear-gradient(rgba(26,26,24,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(26,26,24,0.03) 1px, transparent 1px);
  background-size: 24px 24px;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/app-b2b/src/styles/design.css
git commit -m "feat: rewrite B2B design system — limestone/slate + amber, Geist font, dark theme, page-enter animation"
```

---

### Task 3: Admin design.css 重写

**Files:**
- Modify: `frontend/app-admin/src/styles/design.css`

- [ ] **Step 1: 替换 design.css 完整内容**

```css
/* ===== Super Market Admin — Minimalist Authority Aesthetic ===== */

@import url('https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&family=Geist+Mono:wght@400;500;600&display=swap');

:root {
  /* Typography */
  --font-body: 'Geist', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-mono: 'Geist Mono', 'Courier New', monospace;

  /* Palette — pure white / deep navy + steel blue */
  --color-bg: #ffffff;
  --color-surface: #f8fafc;
  --color-surface-hover: #f1f5f9;
  --color-border: #e2e8f0;
  --color-border-light: #f1f5f9;

  --color-text-primary: #0f172a;
  --color-text-secondary: #475569;
  --color-text-muted: #94a3b8;
  --color-text-inverse: #ffffff;

  --color-accent: #2563eb;
  --color-accent-hover: #1d4ed8;
  --color-accent-light: #eff6ff;
  --color-accent-soft: #dbeafe;

  --color-success: #059669;
  --color-warning: #d97706;
  --color-info: #2563eb;

  /* Spacing */
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 16px;
  --space-lg: 24px;
  --space-xl: 32px;
  --space-2xl: 48px;
  --space-3xl: 64px;
  --space-4xl: 96px;

  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(15, 23, 42, 0.04);
  --shadow-md: 0 4px 12px rgba(15, 23, 42, 0.06);
  --shadow-lg: 0 12px 40px rgba(15, 23, 42, 0.08);
  --shadow-xl: 0 24px 64px rgba(15, 23, 42, 0.12);

  /* Borders */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-full: 9999px;

  /* Transitions */
  --ease-out: cubic-bezier(0.25, 0.46, 0.45, 0.94);
  --ease-in-out: cubic-bezier(0.65, 0, 0.35, 1);
  --duration-fast: 150ms;
  --duration-base: 250ms;
  --duration-slow: 400ms;
}

/* ===== Dark Theme ===== */
[data-theme="dark"] {
  --color-bg: #0f172a;
  --color-surface: #1e293b;
  --color-surface-hover: #334155;
  --color-border: #334155;
  --color-border-light: #1e293b;

  --color-text-primary: #f1f5f9;
  --color-text-secondary: #94a3b8;
  --color-text-muted: #64748b;
  --color-text-inverse: #0f172a;

  --color-accent: #3b82f6;
  --color-accent-hover: #2563eb;
  --color-accent-light: #1e3a5f;
  --color-accent-soft: #172554;

  --color-success: #34d399;
  --color-warning: #fbbf24;
  --color-info: #60a5fa;

  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.15);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.2);
  --shadow-lg: 0 12px 40px rgba(0, 0, 0, 0.3);
}

/* ===== Global Reset & Base ===== */
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

html {
  font-size: 16px;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

body {
  font-family: var(--font-body);
  color: var(--color-text-primary);
  background: var(--color-bg);
  line-height: 1.6;
  overflow-x: hidden;
}

a { color: inherit; text-decoration: none; }
img { max-width: 100%; display: block; }
button { font-family: inherit; cursor: pointer; border: none; background: none; }

/* ===== Utility Classes ===== */
.sr-only {
  position: absolute; width: 1px; height: 1px;
  padding: 0; margin: -1px; overflow: hidden;
  clip: rect(0,0,0,0); border: 0;
}

.container { max-width: 1280px; margin: 0 auto; padding: 0 var(--space-lg); }

/* ===== Scrollbar ===== */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--color-border); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--color-text-muted); }

/* ===== Page Enter Animation ===== */
.page-enter > * {
  opacity: 0;
  transform: translateY(16px);
  animation: fadeUp 0.5s var(--ease-out) forwards;
}
.page-enter > *:nth-child(1) { animation-delay: 0s; }
.page-enter > *:nth-child(2) { animation-delay: 0.06s; }
.page-enter > *:nth-child(3) { animation-delay: 0.12s; }
.page-enter > *:nth-child(4) { animation-delay: 0.18s; }
.page-enter > *:nth-child(5) { animation-delay: 0.24s; }
.page-enter > *:nth-child(6) { animation-delay: 0.30s; }
.page-enter > *:nth-child(7) { animation-delay: 0.36s; }
.page-enter > *:nth-child(8) { animation-delay: 0.42s; }

@keyframes fadeUp {
  to { opacity: 1; transform: translateY(0); }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/app-admin/src/styles/design.css
git commit -m "feat: rewrite Admin design system — pure white/deep navy + steel blue, Geist font, dark theme, page-enter animation"
```

---

### Task 4: ThemeToggle 共享组件

**Files:**
- Create: `frontend/packages/ui/src/ThemeToggle.vue`
- Modify: `frontend/packages/ui/src/index.ts`

- [ ] **Step 1: 创建 ThemeToggle.vue**

```vue
<template>
  <button class="theme-toggle" :title="isDark ? '切换浅色模式' : '切换深色模式'" @click="toggle">
    <svg v-if="isDark" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
      <circle cx="12" cy="12" r="5"/>
      <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
    </svg>
    <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
    </svg>
  </button>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const KEY = 'theme'
const isDark = ref(false)

function apply(t: 'light' | 'dark') {
  if (t === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark')
    isDark.value = true
  } else {
    document.documentElement.removeAttribute('data-theme')
    isDark.value = false
  }
}

function toggle() {
  const next = isDark.value ? 'light' : 'dark'
  localStorage.setItem(KEY, next)
  apply(next)
}

onMounted(() => {
  const saved = localStorage.getItem(KEY)
  apply(saved === 'dark' ? 'dark' : 'light')
})
</script>

<style scoped>
.theme-toggle {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-secondary, #5c5a55);
  background: none; border: none;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.theme-toggle:hover {
  color: var(--color-text-primary, #1a1a18);
  background: var(--color-surface-hover, #f0efe8);
}
</style>
```

- [ ] **Step 2: 导出 ThemeToggle**

在 `frontend/packages/ui/src/index.ts` 末尾追加一行：
```ts
export { default as ThemeToggle } from './ThemeToggle.vue'
```

- [ ] **Step 3: Commit**

```bash
git add frontend/packages/ui/src/ThemeToggle.vue frontend/packages/ui/src/index.ts
git commit -m "feat: add ThemeToggle component for light/dark mode switching"
```

---

### Task 5: App.vue 路由过渡（三个 app）

**Files:**
- Modify: `frontend/app-b2c/src/App.vue`
- Modify: `frontend/app-b2b/src/App.vue`
- Modify: `frontend/app-admin/src/App.vue`

- [ ] **Step 1: 更新 B2C App.vue**

修改 `frontend/app-b2c/src/App.vue`：

```vue
<template>
  <router-view v-slot="{ Component }">
    <Transition name="route" mode="out-in">
      <component :is="Component" />
    </Transition>
  </router-view>
</template>

<script setup lang="ts">
import { useUserStore, useCartStore } from '@supermarket/stores'

const userStore = useUserStore()
const cartStore = useCartStore()

if (userStore.isLoggedIn) {
  userStore.fetchUserInfo()
  cartStore.fetchCount()
}
</script>

<style>
.route-enter-active,
.route-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.route-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.route-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
```

- [ ] **Step 2: 更新 B2B App.vue**

修改 `frontend/app-b2b/src/App.vue`：

```vue
<template>
  <router-view v-slot="{ Component }">
    <Transition name="route" mode="out-in">
      <component :is="Component" />
    </Transition>
  </router-view>
</template>

<script setup lang="ts"></script>

<style>
.route-enter-active,
.route-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.route-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.route-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
```

- [ ] **Step 3: 更新 Admin App.vue**

修改 `frontend/app-admin/src/App.vue`：

```vue
<template>
  <router-view v-slot="{ Component }">
    <Transition name="route" mode="out-in">
      <component :is="Component" />
    </Transition>
  </router-view>
</template>

<style>
.route-enter-active,
.route-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.route-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.route-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
```

- [ ] **Step 4: Commit**

```bash
git add frontend/app-b2c/src/App.vue frontend/app-b2b/src/App.vue frontend/app-admin/src/App.vue
git commit -m "feat: add route transition animation to all three apps"
```

---

### Task 6: B2B Default.vue 集成 ThemeToggle + 工业装饰

**Files:**
- Modify: `frontend/app-b2b/src/layouts/Default.vue`

- [ ] **Step 1: 更新 B2B Default.vue**

在 `<script setup>` 中导入 ThemeToggle：
```ts
import { ThemeToggle } from '@supermarket/ui'
```

在 `<nav class="header-nav">` 中，退出按钮之前插入：
```html
<ThemeToggle />
```

完整 header-nav 变为：
```html
<nav class="header-nav">
  <a href="http://localhost:5173" target="_self" class="nav-link">&#x1F6CD; 商城</a>
  <a href="http://localhost:5175" target="_self" class="nav-link">&#x2699; 管理</a>
  <ThemeToggle />
  <button class="nav-link logout-btn" @click="doLogout">退出</button>
</nav>
```

在 `<style scoped>` 中替换 `.b2b-sidebar` 样式块，增加顶部装饰线：

将：
```css
.b2b-sidebar {
  width: 200px; background: var(--color-surface, #fff);
  border-right: 1px solid var(--color-border-light, #f0ece6);
  padding: var(--space-md, 16px) 0;
  flex-shrink: 0;
}
```

替换为：
```css
.b2b-sidebar {
  width: 200px; background: var(--color-surface, #fff);
  border-right: 1px solid var(--color-border-light, #f0ece6);
  padding: 0; flex-shrink: 0;
  display: flex; flex-direction: column;
}
.sidebar-decor {
  height: 4px;
  background: linear-gradient(90deg, var(--color-accent, #d97706) 0%, var(--color-accent-soft, #fef3c7) 100%);
  flex-shrink: 0;
}
.sidebar-nav { padding: var(--space-md, 16px) 0; }
```

在 `<aside class="b2b-sidebar">` 内部，将 `<router-link>` 列表包裹在 `<nav class="sidebar-nav">` 中，并在最前面加 `<div class="sidebar-decor"></div>`：

```html
<aside class="b2b-sidebar">
  <div class="sidebar-decor"></div>
  <nav class="sidebar-nav">
    <router-link to="/merchant" class="sidebar-item">&#x1F4CA; 首页看板</router-link>
    <router-link to="/merchant/products" class="sidebar-item">&#x1F4E6; 商品管理</router-link>
    <router-link to="/merchant/inventory" class="sidebar-item">&#x1F4CB; 库存管理</router-link>
    <router-link to="/merchant/orders" class="sidebar-item">&#x1F4DD; 订单处理</router-link>
    <router-link to="/merchant/shop/settings" class="sidebar-item">&#x1F3EA; 店铺设置</router-link>
    <router-link to="/merchant/coupons" class="sidebar-item">&#x1F4AC; 优惠券</router-link>
    <router-link to="/merchant/seckill" class="sidebar-item">&#x26A1; 秒杀活动</router-link>
    <router-link to="/merchant/reviews" class="sidebar-item">&#x2B50; 评价管理</router-link>
    <router-link to="/merchant/settlement" class="sidebar-item">&#x1F4B0; 结算查询</router-link>
  </nav>
</aside>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/app-b2b/src/layouts/Default.vue
git commit -m "feat: B2B layout — add ThemeToggle, sidebar amber accent bar, sidebar nav wrapper"
```

---

### Task 7: Admin Default.vue 集成 ThemeToggle + 布局调整

**Files:**
- Modify: `frontend/app-admin/src/layouts/Default.vue`

- [ ] **Step 1: 更新 Admin Default.vue**

在 `<script setup>` 中导入 ThemeToggle：
```ts
import { ThemeToggle } from '@supermarket/ui'
```

在 `<nav class="header-nav">` 中，退出按钮之前插入：
```html
<ThemeToggle />
```

将 sidebar 宽度从 `200px` 改为 `220px`：

```css
.admin-sidebar {
  width: 220px;
  /* ... 其余不变 */
}
```

同步更新 content 的 max-width：
```css
.admin-content {
  flex: 1; padding: var(--space-lg, 24px);
  max-width: calc(100vw - 220px);
}
```

并将 sidebar 的 `border-right` 从 `1px solid` 改为更细的 `0.5px solid`，透明度降低：

```css
.admin-sidebar {
  width: 220px; background: var(--color-surface, #fff);
  border-right: 0.5px solid var(--color-border, #e2e8f0);
  padding: var(--space-md, 16px) 0; flex-shrink: 0;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/app-admin/src/layouts/Default.vue
git commit -m "feat: Admin layout — add ThemeToggle, widen sidebar to 220px"
```

---

### Task 8: StatCard 组件增强

**Files:**
- Modify: `frontend/packages/ui/src/StatCard.vue`

- [ ] **Step 1: 更新 StatCard.vue**

将 `<style scoped>` 替换为：

```css
.stat-card {
  background: var(--color-surface, #fff);
  border-radius: var(--radius-md, 8px);
  padding: 16px 20px;
  border: 1px solid var(--color-border, #eee);
  min-width: 160px;
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 3px; height: 100%;
  background: var(--color-accent, #d97706);
  border-radius: 3px 0 0 3px;
}
.stat-label { font-size: 12px; color: var(--color-text-muted, #999); margin-bottom: 6px; }
.stat-value { font-size: 24px; font-weight: 700; color: var(--color-text-primary, #333); }
.stat-trend { font-size: 11px; margin-top: 4px; }
.stat-card.up .stat-trend { color: #10b981; }
.stat-card.down .stat-trend { color: #ef4444; }
```

- [ ] **Step 2: Commit**

```bash
git add frontend/packages/ui/src/StatCard.vue
git commit -m "feat: StatCard — add left accent color bar via ::before pseudo-element"
```

---

### Task 9: DataTable 行交互增强

**Files:**
- Modify: `frontend/packages/ui/src/DataTable.vue`

- [ ] **Step 1: 更新 DataTable.vue 样式**

在 `<style scoped>` 末尾追加：

```css
:deep(.el-table__body tr) {
  transition: background-color 0.15s ease;
}
:deep(.el-table__body tr:hover) {
  background-color: var(--color-surface-hover, #f5f1ea) !important;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/packages/ui/src/DataTable.vue
git commit -m "feat: DataTable — add row hover background transition"
```

---

### Task 10: B2C 全部页面加 page-enter（16 页）

**Files (all add `class="page-enter"` to root div):**

| # | 文件 | 根元素当前 class |
|---|------|-----------------|
| 1 | `app-b2c/src/pages/home/HomePage.vue` | `class="home"` |
| 2 | `app-b2c/src/pages/search/SearchPage.vue` | — |
| 3 | `app-b2c/src/pages/product/ProductPage.vue` | — |
| 4 | `app-b2c/src/pages/cart/CartPage.vue` | — |
| 5 | `app-b2c/src/pages/order/ConfirmPage.vue` | — |
| 6 | `app-b2c/src/pages/order/ResultPage.vue` | — |
| 7 | `app-b2c/src/pages/order/DetailPage.vue` | — |
| 8 | `app-b2c/src/pages/user/OrdersPage.vue` | — |
| 9 | `app-b2c/src/pages/user/CenterPage.vue` | — |
| 10 | `app-b2c/src/pages/user/AddressPage.vue` | — |
| 11 | `app-b2c/src/pages/user/CouponPage.vue` | — |
| 12 | `app-b2c/src/pages/user/ReviewPage.vue` | — |
| 13 | `app-b2c/src/pages/notify/NotifyPage.vue` | — |
| 14 | `app-b2c/src/pages/seckill/SeckillPage.vue` | — |
| 15 | `app-b2c/src/pages/login/LoginPage.vue` | — |
| 16 | `app-b2c/src/pages/login/RegisterPage.vue` | — |

- [ ] **Step 1: 批量读取 16 个文件，确认根元素结构**

对每个文件，读取前 5 行确认 `<template>` 根元素。

Run:
```bash
for f in frontend/app-b2c/src/pages/*/*.vue frontend/app-b2c/src/pages/*.vue; do
  echo "=== $f ==="
  head -5 "$f"
done
```

- [ ] **Step 2: 为每个页面的根 `<div>` 添加 `class="page-enter"`**

规则：
- 如果根 `<div>` 已有 class（如 HomePage 的 `class="home"`），改为 `class="home page-enter"`
- 如果根 `<div>` 无 class，添加 `class="page-enter"`
- LoginPage 根元素是 `class="login-page"`，改为 `class="login-page page-enter"`
- 如果根元素不是 `<div>`（如果有），包裹一层 `<div class="page-enter">`

逐个修改。以 HomePage 为例，将：
```html
<div class="home">
```
改为：
```html
<div class="home page-enter">
```

- [ ] **Step 3: 验证所有 16 个文件都含 page-enter**

```bash
grep -r "page-enter" frontend/app-b2c/src/pages/ | wc -l
```
Expected: >= 16

- [ ] **Step 4: Commit**

```bash
git add frontend/app-b2c/src/pages/
git commit -m "feat: B2C — add page-enter class to all 16 pages for staggered reveal animation"
```

---

### Task 11: B2B 全部页面加 page-enter（14 页）

**Files:**

| # | 文件 |
|---|------|
| 1 | `app-b2b/src/pages/dashboard/DashboardPage.vue` |
| 2 | `app-b2b/src/pages/products/ListPage.vue` |
| 3 | `app-b2b/src/pages/products/CreatePage.vue` |
| 4 | `app-b2b/src/pages/inventory/InventoryPage.vue` |
| 5 | `app-b2b/src/pages/orders/OrdersPage.vue` |
| 6 | `app-b2b/src/pages/orders/DetailPage.vue` |
| 7 | `app-b2b/src/pages/shop/ShopPage.vue` |
| 8 | `app-b2b/src/pages/coupons/CouponsPage.vue` |
| 9 | `app-b2b/src/pages/seckill/SeckillPage.vue` |
| 10 | `app-b2b/src/pages/reviews/ReviewsPage.vue` |
| 11 | `app-b2b/src/pages/notifications/NotificationsPage.vue` |
| 12 | `app-b2b/src/pages/settlement/SettlementPage.vue` |
| 13 | `app-b2b/src/pages/login/LoginPage.vue` |
| 14 | `app-b2b/src/pages/login/RegisterPage.vue` |

- [ ] **Step 1: 为每个页面的根 `<div>` 添加 `class="page-enter"`**

同 Task 10 规则。已有 class 的追加 `page-enter`，无 class 的新增 `class="page-enter"`。

- [ ] **Step 2: 验证**

```bash
grep -r "page-enter" frontend/app-b2b/src/pages/ | wc -l
```
Expected: >= 14

- [ ] **Step 3: Commit**

```bash
git add frontend/app-b2b/src/pages/
git commit -m "feat: B2B — add page-enter class to all 14 pages for staggered reveal animation"
```

---

### Task 12: Admin 全部页面加 page-enter（9 页）

**Files:**

| # | 文件 |
|---|------|
| 1 | `app-admin/src/pages/dashboard/DashboardPage.vue` |
| 2 | `app-admin/src/pages/merchants/MerchantPage.vue` |
| 3 | `app-admin/src/pages/products/AuditPage.vue` |
| 4 | `app-admin/src/pages/categories/CategoryPage.vue` |
| 5 | `app-admin/src/pages/banners/BannerPage.vue` |
| 6 | `app-admin/src/pages/risk/RiskPage.vue` |
| 7 | `app-admin/src/pages/reports/ReportPage.vue` |
| 8 | `app-admin/src/pages/notify/TemplatePage.vue` |
| 9 | `app-admin/src/pages/login/LoginPage.vue` |

- [ ] **Step 1: 为每个页面的根 `<div>` 添加 `class="page-enter"`**

同 Task 10 规则。

- [ ] **Step 2: 验证**

```bash
grep -r "page-enter" frontend/app-admin/src/pages/ | wc -l
```
Expected: >= 9

- [ ] **Step 3: Commit**

```bash
git add frontend/app-admin/src/pages/
git commit -m "feat: Admin — add page-enter class to all 9 pages for staggered reveal animation"
```

---

### Task 13: B2C 氛围深化 — Hero + Footer

**Files:**
- Modify: `frontend/app-b2c/src/pages/home/HomePage.vue`
- Modify: `frontend/app-b2c/src/layouts/Default.vue`

- [ ] **Step 1: 更新 B2C HomePage hero 背景**

在 `frontend/app-b2c/src/pages/home/HomePage.vue` 的 `<style scoped>` 中，将 `.hero` 背景替换：

```css
.hero {
  background:
    linear-gradient(160deg, #faf7f2 0%, #f0e8dc 35%, #e8dcc8 100%);
  position: relative; overflow: hidden;
  padding: var(--space-3xl) 0;
}
```

将 `.hero-dot-pattern` 替换为：
```css
.hero-decor {
  position: absolute; right: -60px; top: -60px;
  width: 360px; height: 360px;
  border: 2px solid rgba(185, 28, 28, 0.06);
  border-radius: 50%;
  pointer-events: none;
}
.hero-decor::after {
  content: '';
  position: absolute; inset: 40px;
  border: 2px solid rgba(185, 28, 28, 0.04);
  border-radius: 50%;
}
```

在 template 中将 `<div class="hero-dot-pattern"></div>` 替换为 `<div class="hero-decor"></div>`。

- [ ] **Step 2: 更新 B2C Default.vue footer 分隔线**

在 `frontend/app-b2c/src/layouts/Default.vue` 的 `<style scoped>` 中，将 `.site-footer` 改为：

```css
.site-footer {
  background: var(--color-text-primary);
  color: rgba(255,255,255,0.7);
  margin-top: var(--space-3xl);
  border-top: 1px solid transparent;
  background-image:
    linear-gradient(to right, rgba(185,28,28,0.4), rgba(185,28,28,0.05) 50%, transparent 70%),
    linear-gradient(var(--color-text-primary), var(--color-text-primary));
  background-position: top, top;
  background-repeat: no-repeat;
  background-size: 100% 1px, 100% 100%;
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/app-b2c/src/pages/home/HomePage.vue frontend/app-b2c/src/layouts/Default.vue
git commit -m "feat: B2C atmosphere — deepen hero gradient with ring decor, footer accent separator line"
```

---

### Task 14: useRevealOnScroll composable（滚动渐显）

**Files:**
- Create: `frontend/packages/utils/src/useRevealOnScroll.ts`
- Modify: `frontend/packages/utils/src/index.ts`

- [ ] **Step 1: 创建 useRevealOnScroll.ts**

```ts
import { ref, onMounted, onUnmounted, type Ref } from 'vue'

export function useRevealOnScroll(
  targetRef: Ref<HTMLElement | null>,
  options?: { threshold?: number; rootMargin?: string }
) {
  const revealed = ref(false)
  let observer: IntersectionObserver | null = null

  onMounted(() => {
    if (!targetRef.value) return
    observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          revealed.value = true
          observer?.unobserve(entry.target)
        }
      },
      { threshold: options?.threshold ?? 0.1, rootMargin: options?.rootMargin ?? '0px 0px -40px 0px' }
    )
    observer.observe(targetRef.value)
  })

  onUnmounted(() => observer?.disconnect())

  return { revealed }
}
```

- [ ] **Step 2: 在 index.ts 中导出**

在 `frontend/packages/utils/src/index.ts` 末尾追加：
```ts
export { useRevealOnScroll } from './useRevealOnScroll'
```

- [ ] **Step 3: Commit**

```bash
git add frontend/packages/utils/src/useRevealOnScroll.ts frontend/packages/utils/src/index.ts
git commit -m "feat: add useRevealOnScroll composable for scroll-triggered reveal animations"
```

---

### Task 15: B2B Dashboard 网格底纹

**Files:**
- Modify: `frontend/app-b2b/src/pages/dashboard/DashboardPage.vue`

- [ ] **Step 1: 给 B2B Dashboard 根元素加网格背景**

读取 `DashboardPage.vue`，确认根元素 class。如果根 div 已有 class（如 `class="dashboard page-enter"`），不变。然后在 `<style scoped>` 中给根元素追加 `bg-grid` 样式引用：

```css
.dashboard {
  background-image:
    linear-gradient(rgba(26,26,24,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(26,26,24,0.03) 1px, transparent 1px);
  background-size: 24px 24px;
}
```

> 注：`design.css` 已定义 `.bg-grid` 工具类，此处直接使用或内联样式。

- [ ] **Step 2: Commit**

```bash
git add frontend/app-b2b/src/pages/dashboard/DashboardPage.vue
git commit -m "feat: B2B — add grid texture background to dashboard page"
```

---

### Task 16: PriceDisplay 色值对齐新色系

**Files:**
- Modify: `frontend/packages/ui/src/PriceDisplay.vue`

- [ ] **Step 1: 更新价格颜色为设计系统中性色**

在 `<style scoped>` 中将 `.price-display` 的 `color` 从 `#f30213` 改为 `var(--color-accent, #b91c1c)`：

```css
.price-display { font-weight: 700; color: var(--color-accent, #b91c1c); }
```

- [ ] **Step 2: Commit**

```bash
git add frontend/packages/ui/src/PriceDisplay.vue
git commit -m "fix: PriceDisplay — use CSS variable for price color instead of hardcoded red"
```

---

### Task 17: 最终验证 — pnpm build

- [ ] **Step 1: 安装依赖并构建**

```bash
cd frontend && pnpm install && pnpm build
```

Expected: 三个 app 构建全部通过，无 CSS/TS 编译错误。

- [ ] **Step 2: 验证三个 design.css 各不相同**

```bash
diff frontend/app-b2c/src/styles/design.css frontend/app-b2b/src/styles/design.css | head -5
diff frontend/app-b2b/src/styles/design.css frontend/app-admin/src/styles/design.css | head -5
```

Expected: 两个 diff 都有差异输出。

- [ ] **Step 3: 验证 Font Awesome 图标引用无残留（SearchBar / Default.vue）**

确认 Default.vue 中的 emoji/unicode 图标仍然正确显示。

```bash
grep -r "font-awesome\|fa-" frontend/packages/ frontend/app-*/src/ || echo "No Font Awesome references found"
```

---

### Task 18: 最终 Commit

- [ ] **Step 1: 确认所有改动已提交**

```bash
git status
```

- [ ] **Step 2: 如无未提交改动，完成**

```bash
git log --oneline -10
```
