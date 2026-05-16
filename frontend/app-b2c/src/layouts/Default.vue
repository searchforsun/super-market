<template>
  <div class="site-shell">
    <!-- Top Announcement Bar -->
    <div class="announcement-bar">
      <span>新用户首单享 9 折优惠 · 满 99 元包邮 · 7 天无忧退换</span>
    </div>

    <!-- Header -->
    <header class="site-header">
      <div class="header-inner container">
        <!-- Logo -->
        <router-link to="/" class="logo">
          <span class="logo-mark">S</span>
          <span class="logo-text">Super Market</span>
        </router-link>

        <!-- Search -->
        <div class="header-search">
          <SearchBar @search="onSearch" />
        </div>

        <!-- Actions -->
        <nav class="header-actions">
          <template v-if="userStore.isLoggedIn">
            <router-link to="/user/center" class="action-link">
              <span class="action-icon">&#xf007;</span>
              <span class="action-label">{{ userStore.userInfo?.nickname || '我的' }}</span>
            </router-link>
            <router-link to="/user/orders" class="action-link">
              <span class="action-label">订单</span>
            </router-link>
            <router-link to="/user/notifications" class="action-link has-badge">
              <span class="action-label">消息</span>
              <span v-if="unreadCount" class="badge-dot">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </router-link>
            <router-link to="/cart" class="action-link cart-link">
              <span class="cart-icon-wrap">
                <span class="action-label">购物车</span>
                <span v-if="cartStore.count" class="cart-badge">{{ cartStore.count }}</span>
              </span>
            </router-link>
            <button class="action-link logout-btn" @click="doLogout">退出</button>
          </template>
          <template v-else>
            <router-link to="/login" class="action-link">登录</router-link>
            <router-link to="/register" class="btn-register">注册</router-link>
          </template>
        </nav>
      </div>
    </header>

    <!-- Category Navigation -->
    <nav class="category-nav">
      <div class="container nav-inner">
        <router-link to="/" class="nav-item" :class="{ active: isHome }">首页</router-link>
        <router-link to="/seckill" class="nav-item nav-seckill">&#9889; 限时秒杀</router-link>
        <router-link to="/search?categoryId=4" class="nav-item">手机</router-link>
        <router-link to="/search?categoryId=7" class="nav-item">电脑</router-link>
        <router-link to="/search?categoryId=11" class="nav-item">零食</router-link>
        <router-link to="/search?categoryId=12" class="nav-item">粮油</router-link>
      </div>
    </nav>

    <!-- Page Content -->
    <main class="site-main">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="site-footer">
      <div class="container footer-inner">
        <div class="footer-brand">
          <span class="logo-mark small">S</span>
          <p class="footer-tagline">品质生活，从这里开始</p>
        </div>
        <div class="footer-links">
          <dl>
            <dt>购物指南</dt>
            <dd><a href="#">购物流程</a></dd>
            <dd><a href="#">会员介绍</a></dd>
            <dd><a href="#">常见问题</a></dd>
          </dl>
          <dl>
            <dt>配送方式</dt>
            <dd><a href="#">配送范围</a></dd>
            <dd><a href="#">配送时效</a></dd>
            <dd><a href="#">验货签收</a></dd>
          </dl>
          <dl>
            <dt>售后服务</dt>
            <dd><a href="#">退换政策</a></dd>
            <dd><a href="#">退款说明</a></dd>
            <dd><a href="#">联系客服</a></dd>
          </dl>
          <dl>
            <dt>关于我们</dt>
            <dd><a href="#">公司介绍</a></dd>
            <dd><a href="#">加入我们</a></dd>
            <dd><a href="#">联系我们</a></dd>
          </dl>
        </div>
      </div>
      <div class="footer-bottom">
        <span>&copy; 2026 Super Market. All rights reserved.</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore, useCartStore } from '@supermarket/stores'
import { SearchBar } from '@supermarket/ui'
import { getUnreadCount } from '@supermarket/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const unreadCount = ref(0)

const isHome = computed(() => route.name === 'home')

async function fetchUnread() {
  if (!userStore.userId) return
  try { unreadCount.value = await getUnreadCount(userStore.userId) } catch {}
}

function onSearch(keyword: string) {
  if (keyword) router.push({ path: '/search', query: { q: keyword } })
}

function doLogout() {
  userStore.logout()
  router.push('/')
}

onMounted(fetchUnread)
watch(() => userStore.userId, fetchUnread)
</script>

<style scoped>
/* ===== Shell ===== */
.site-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
}
.site-main {
  flex: 1;
}

/* ===== Announcement Bar ===== */
.announcement-bar {
  background: var(--color-text-primary);
  color: var(--color-text-inverse);
  font-size: 12px;
  text-align: center;
  padding: 6px var(--space-md);
  letter-spacing: 0.02em;
  opacity: 0.9;
}

/* ===== Header ===== */
.site-header {
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
  background: rgba(255,255,255,0.92);
}
.header-inner {
  display: flex;
  align-items: center;
  height: var(--header-height);
  gap: var(--space-xl);
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.logo-mark {
  width: 36px; height: 36px;
  background: var(--color-accent);
  color: var(--color-text-inverse);
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 700;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-mark.small { width: 28px; height: 28px; font-size: 16px; border-radius: var(--radius-sm); }
.logo-text {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.header-search { flex: 1; max-width: 480px; }
.header-actions { display: flex; align-items: center; gap: var(--space-lg); flex-shrink: 0; }
.action-link {
  font-size: 13px;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
  transition: color var(--duration-fast) var(--ease-out);
}
.action-link:hover { color: var(--color-text-primary); }
.action-icon { font-size: 15px; }
.action-label { white-space: nowrap; }
.has-badge { position: relative; }
.badge-dot {
  position: absolute; top: -8px; right: -16px;
  background: var(--color-accent); color: var(--color-text-inverse);
  font-size: 10px; min-width: 18px; height: 18px; line-height: 18px;
  border-radius: var(--radius-full); text-align: center; padding: 0 4px;
  font-weight: 600;
}
.cart-icon-wrap { position: relative; display: flex; align-items: center; }
.cart-badge {
  position: absolute; top: -10px; right: -14px;
  background: var(--color-accent); color: var(--color-text-inverse);
  font-size: 10px; min-width: 18px; height: 18px; line-height: 18px;
  border-radius: var(--radius-full); text-align: center; font-weight: 600;
}
.btn-register {
  background: var(--color-accent);
  color: var(--color-text-inverse);
  padding: 8px 20px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  transition: background var(--duration-fast) var(--ease-out);
}
.btn-register:hover { background: var(--color-accent-hover); }
.logout-btn { background: none; border: none; cursor: pointer; }

/* ===== Category Nav ===== */
.category-nav {
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.nav-inner {
  display: flex;
  height: var(--nav-height);
  align-items: center;
  gap: var(--space-xl);
}
.nav-item {
  font-size: 14px;
  color: var(--color-text-secondary);
  position: relative;
  padding: 2px 0;
  transition: color var(--duration-fast) var(--ease-out);
}
.nav-item:hover { color: var(--color-text-primary); }
.nav-item.active { color: var(--color-accent); font-weight: 600; }
.nav-item.active::after {
  content: '';
  position: absolute; bottom: -11px; left: 0; right: 0;
  height: 2px; background: var(--color-accent); border-radius: 1px;
}
.nav-seckill { color: var(--color-accent); font-weight: 500; }

/* ===== Footer ===== */
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
.footer-inner {
  display: flex; gap: var(--space-3xl);
  padding: var(--space-3xl) var(--space-lg);
}
.footer-brand { flex-shrink: 0; }
.footer-tagline { margin-top: var(--space-md); font-size: 14px; color: rgba(255,255,255,0.5); }
.footer-links { display: flex; gap: var(--space-3xl); flex: 1; justify-content: flex-end; }
.footer-links dl { min-width: 110px; }
.footer-links dt { color: rgba(255,255,255,0.9); font-size: 13px; font-weight: 600; margin-bottom: var(--space-sm); }
.footer-links dd { font-size: 12px; margin-bottom: 6px; }
.footer-links dd a { color: rgba(255,255,255,0.5); transition: color var(--duration-fast); }
.footer-links dd a:hover { color: rgba(255,255,255,0.8); }
.footer-bottom {
  border-top: 1px solid rgba(255,255,255,0.08);
  text-align: center; padding: var(--space-lg);
  font-size: 12px; color: rgba(255,255,255,0.35);
}
</style>
