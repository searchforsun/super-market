<template>
  <div class="center-page page-enter">
    <h2>个人中心</h2>
    <div v-if="userStore.userInfo" class="user-info-card">
      <div class="user-avatar">{{ (userStore.userInfo.nickname || '用户')[0] }}</div>
      <div class="user-detail">
        <p class="nickname"><strong>{{ userStore.userInfo.nickname }}</strong></p>
        <p>手机号：{{ userStore.userInfo.phone }}</p>
        <p v-if="memberInfo">会员等级：{{ memberInfo.level }} 级 | 积分：{{ memberInfo.points }}</p>
        <p v-else class="loading-text">加载会员信息...</p>
      </div>
    </div>
    <div class="center-links">
      <router-link to="/user/orders"><span class="link-icon">📋</span>我的订单</router-link>
      <router-link to="/user/addresses"><span class="link-icon">📍</span>收货地址</router-link>
      <router-link to="/user/coupons"><span class="link-icon">🎫</span>我的优惠券</router-link>
      <router-link to="/user/reviews"><span class="link-icon">⭐</span>我的评价</router-link>
      <router-link to="/user/notifications"><span class="link-icon">🔔</span>消息通知</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@supermarket/stores'
import { getMemberInfo } from '@supermarket/api'

const userStore = useUserStore()
const memberInfo = ref<any>(null)

onMounted(async () => {
  if (!userStore.userId) return
  try { memberInfo.value = await getMemberInfo(userStore.userId) } catch { /* ignore */ }
})
</script>

<style scoped>
.center-page { background: #fff; padding: 24px; border-radius: 8px; }
h2 { margin: 0 0 20px; font-size: 18px; }
.user-info-card { display: flex; align-items: center; gap: 16px; padding: 16px; background: #fafafa; border-radius: 8px; margin-bottom: 24px; }
.user-avatar { width: 56px; height: 56px; border-radius: 50%; background: #e1251b; color: #fff; font-size: 24px; display: flex; align-items: center; justify-content: center; }
.user-detail p { margin: 4px 0; font-size: 14px; color: #666; }
.user-detail .nickname { color: #333; font-size: 16px; }
.loading-text { color: #999 !important; }
.center-links { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.center-links a { padding: 16px 12px; background: #f8f8f8; text-align: center; border-radius: 8px; color: #333; text-decoration: none; transition: all 0.2s; display: flex; align-items: center; justify-content: center; gap: 6px; font-size: 14px; }
.center-links a:hover { background: #fff0f0; color: #e1251b; transform: translateY(-1px); box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.link-icon { font-size: 18px; }
</style>
