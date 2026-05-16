<template>
  <div class="b2b-layout">
    <!-- Top Header -->
    <header class="b2b-header">
      <div class="header-inner">
        <router-link to="/merchant" class="b2b-logo">
          <span class="logo-mark">S</span>
          <span>Super Market <em>商家中心</em></span>
        </router-link>
        <nav class="header-nav">
          <a href="http://localhost:5173" target="_self" class="nav-link">&#x1F6CD; 商城</a>
          <a href="http://localhost:5175" target="_self" class="nav-link">&#x2699; 管理</a>
          <ThemeToggle />
          <button class="nav-link logout-btn" @click="doLogout">退出</button>
        </nav>
      </div>
    </header>

    <!-- Body -->
    <div class="b2b-body">
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
      <main class="b2b-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { clearAuth } from '@supermarket/utils'
import { ThemeToggle } from '@supermarket/ui'

const router = useRouter()
function doLogout() {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.b2b-layout {
  min-height: 100vh;
  background: var(--color-bg, #faf8f5);
  font-family: var(--font-body, 'Inter', sans-serif);
}
.b2b-header {
  background: var(--color-text-primary, #1a1816);
  padding: 0 var(--space-lg, 24px);
  position: sticky; top: 0; z-index: 100;
}
.header-inner {
  max-width: 1400px; margin: 0 auto;
  display: flex; align-items: center;
  height: 56px; justify-content: space-between;
}
.b2b-logo {
  display: flex; align-items: center; gap: 10px;
  color: var(--color-text-inverse, #fff);
  text-decoration: none; font-size: 16px; font-weight: 600;
}
.b2b-logo em { font-style: normal; font-weight: 400; color: rgba(255,255,255,0.6); font-size: 13px; margin-left: 4px; }
.logo-mark {
  width: 32px; height: 32px;
  background: var(--color-accent, #c41e3a);
  color: #fff; border-radius: var(--radius-sm, 4px);
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-display, 'Playfair Display', serif);
  font-weight: 700; font-size: 18px;
}
.header-nav { display: flex; align-items: center; gap: var(--space-lg, 24px); }
.nav-link {
  color: rgba(255,255,255,0.7); font-size: 13px;
  text-decoration: none; transition: color 0.2s;
  background: none; border: none; cursor: pointer;
}
.nav-link:hover { color: var(--color-text-inverse, #fff); }
.logout-btn { color: rgba(255,255,255,0.5); padding: 0; }

.b2b-body { display: flex; min-height: calc(100vh - 56px); }
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
.sidebar-item {
  display: block; padding: 10px 24px;
  font-size: 13px; color: var(--color-text-secondary, #6b6560);
  text-decoration: none; transition: all 0.15s;
  border-left: 2px solid transparent;
}
.sidebar-item:hover {
  background: var(--color-surface-hover, #f6f3ee);
  color: var(--color-text-primary, #1a1816);
}
.sidebar-item.router-link-active {
  background: var(--color-accent-light, #fef0f3);
  color: var(--color-accent, #c41e3a);
  border-left-color: var(--color-accent, #c41e3a);
  font-weight: 500;
}
.b2b-content {
  flex: 1; padding: var(--space-lg, 24px);
  max-width: calc(100vw - 200px);
}
</style>
