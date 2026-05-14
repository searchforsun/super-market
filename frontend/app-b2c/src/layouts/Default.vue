<template>
  <div class="layout">
    <!-- 顶部工具栏 -->
    <div class="top-bar">
      <div class="top-inner">
        <span v-if="!userStore.isLoggedIn">
          <router-link to="/login">你好，请登录</router-link>
          <router-link to="/login" style="margin-left:12px">免费注册</router-link>
        </span>
        <span v-else>
          你好，<strong>{{ userStore.userInfo?.nickname || '用户' }}</strong>
          <a @click="userStore.logout()" style="margin-left:12px;cursor:pointer">退出</a>
        </span>
        <div class="top-links">
          <router-link to="/user/orders">我的订单</router-link>
          <router-link to="/user/center" style="margin-left:16px">个人中心</router-link>
          <router-link to="/user/notifications" style="margin-left:16px">
            消息
            <span v-if="unreadCount" class="badge">{{ unreadCount }}</span>
          </router-link>
        </div>
      </div>
    </div>

    <!-- 头部搜索栏 -->
    <div class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">Super Market</router-link>
        <SearchBar placeholder="搜索商品、品牌" @search="onSearch" />
        <router-link to="/cart" class="cart-btn">🛒 购物车 {{ cartStore.count || '' }}</router-link>
      </div>
    </div>

    <!-- 分类导航 -->
    <div class="nav">
      <div class="nav-inner">
        <span class="nav-category">全部商品分类</span>
        <router-link to="/search?categoryId=1">手机通讯</router-link>
        <router-link to="/search?categoryId=2">电脑办公</router-link>
        <router-link to="/search?categoryId=3">服装内衣</router-link>
        <router-link to="/search?categoryId=4">运动户外</router-link>
        <router-link to="/search?categoryId=5">食品生鲜</router-link>
        <router-link to="/seckill" style="color:#f30213">⚡ 限时秒杀</router-link>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content">
      <router-view />
    </div>

    <!-- 底部 -->
    <div class="footer">
      <div class="footer-inner">
        <div class="footer-links">
          <a>关于我们</a><a>联系我们</a><a>帮助中心</a><a>售后服务</a><a>隐私政策</a>
        </div>
        <p>© 2026 Super Market. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore, useCartStore } from '@supermarket/stores'
import { SearchBar } from '@supermarket/ui'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const unreadCount = ref(0)

function onSearch(keyword: string) {
  if (keyword) router.push({ path: '/search', query: { q: keyword } })
}
</script>

<style scoped>
.layout { min-height: 100vh; display: flex; flex-direction: column; }
.top-bar { background: #2d2d2d; color: #999; font-size: 12px; padding: 4px 0; }
.top-inner { max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; padding: 0 16px; }
.top-bar a { color: #ccc; text-decoration: none; }
.top-bar a:hover { color: #fff; }
.top-links { display: flex; }
.badge { background: #e1251b; color: #fff; border-radius: 10px; padding: 0 5px; font-size: 10px; }
.header { background: #e1251b; padding: 12px 0; }
.header-inner { max-width: 1200px; margin: 0 auto; display: flex; align-items: center; gap: 20px; padding: 0 16px; }
.logo { font-size: 22px; font-weight: 700; color: #fff; text-decoration: none; white-space: nowrap; }
.cart-btn { color: #fff; font-size: 14px; text-decoration: none; white-space: nowrap; margin-left: auto; }
.nav { background: #e1251b; padding: 4px 0 8px; }
.nav-inner { max-width: 1200px; margin: 0 auto; display: flex; gap: 16px; padding: 0 16px; }
.nav-inner a { color: #fcc; font-size: 13px; text-decoration: none; }
.nav-inner a:hover { color: #fff; }
.nav-category { color: #fff; font-weight: 600; font-size: 13px; margin-right: 8px; }
.content { flex: 1; max-width: 1200px; margin: 0 auto; padding: 16px; width: 100%; }
.footer { background: #f5f5f5; padding: 24px 0; margin-top: 40px; text-align: center; color: #999; font-size: 12px; }
.footer-links { display: flex; gap: 20px; justify-content: center; margin-bottom: 8px; }
.footer-links a { color: #666; cursor: pointer; }
</style>
