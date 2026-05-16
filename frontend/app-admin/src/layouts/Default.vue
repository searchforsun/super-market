<template>
  <div class="admin-layout">
    <!-- Top Header -->
    <header class="admin-header">
      <div class="header-inner">
        <router-link to="/admin" class="admin-logo">
          <span class="logo-mark">S</span>
          <span>Super Market <em>运营管理</em></span>
        </router-link>
        <nav class="header-nav">
          <a href="http://localhost:5173" target="_self" class="nav-link">&#x1F6CD; 商城</a>
          <a href="http://localhost:5174" target="_self" class="nav-link">&#x1F3EA; 商家</a>
          <button class="nav-link logout-btn" @click="doLogout">退出</button>
        </nav>
      </div>
    </header>

    <!-- Body -->
    <div class="admin-body">
      <aside class="admin-sidebar">
        <router-link to="/admin" class="sidebar-item">&#x1F4CA; 运营看板</router-link>
        <router-link to="/admin/merchants" class="sidebar-item">&#x1F3EA; 商家审核</router-link>
        <router-link to="/admin/products/audit" class="sidebar-item">&#x1F4E6; 商品审核</router-link>
        <router-link to="/admin/categories" class="sidebar-item">&#x1F4C2; 类目管理</router-link>
        <router-link to="/admin/banners" class="sidebar-item">&#x1F5BC; Banner 广告</router-link>
        <router-link to="/admin/risk" class="sidebar-item">&#x1F6E1; 风控管理</router-link>
        <router-link to="/admin/reports" class="sidebar-item">&#x1F4C8; 数据报表</router-link>
        <router-link to="/admin/notify-templates" class="sidebar-item">&#x1F4E7; 通知模板</router-link>
      </aside>
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { clearAuth } from '@supermarket/utils'

const router = useRouter()
function doLogout() { clearAuth(); router.push('/login') }
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: var(--color-bg, #faf8f5);
  font-family: var(--font-body, 'Inter', sans-serif);
}
.admin-header {
  background: #3b1d6e;
  padding: 0 var(--space-lg, 24px);
  position: sticky; top: 0; z-index: 100;
}
.header-inner {
  max-width: 1400px; margin: 0 auto;
  display: flex; align-items: center;
  height: 56px; justify-content: space-between;
}
.admin-logo {
  display: flex; align-items: center; gap: 10px;
  color: #fff; text-decoration: none; font-size: 16px; font-weight: 600;
}
.admin-logo em {
  font-style: normal; font-weight: 400;
  color: rgba(255,255,255,0.55); font-size: 12px; margin-left: 4px;
}
.logo-mark {
  width: 32px; height: 32px;
  background: #7c3aed;
  color: #fff; border-radius: var(--radius-sm, 4px);
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-display); font-weight: 700; font-size: 18px;
}
.header-nav { display: flex; align-items: center; gap: var(--space-lg, 24px); }
.nav-link {
  color: rgba(255,255,255,0.65); font-size: 13px;
  text-decoration: none; transition: color 0.2s;
  background: none; border: none; cursor: pointer;
}
.nav-link:hover { color: #fff; }
.logout-btn { color: rgba(255,255,255,0.4); padding: 0; }

.admin-body { display: flex; min-height: calc(100vh - 56px); }
.admin-sidebar {
  width: 200px; background: var(--color-surface, #fff);
  border-right: 1px solid var(--color-border-light, #f0ece6);
  padding: var(--space-md, 16px) 0; flex-shrink: 0;
}
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
  background: #f5f0ff; color: #7c3aed;
  border-left-color: #7c3aed; font-weight: 500;
}
.admin-content {
  flex: 1; padding: var(--space-lg, 24px);
  max-width: calc(100vw - 200px);
}
</style>
