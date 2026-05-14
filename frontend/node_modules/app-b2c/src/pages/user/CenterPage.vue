<template>
  <div class="center-page">
    <h2>个人中心</h2>
    <div v-if="userStore.userInfo" class="user-info">
      <p><strong>{{ userStore.userInfo.nickname }}</strong></p>
      <p>手机号: {{ userStore.userInfo.phone }}</p>
      <p>会员等级: {{ userStore.userInfo.memberLevel || '普通用户' }}</p>
    </div>
    <div class="center-links">
      <router-link to="/user/orders">我的订单</router-link>
      <router-link to="/user/addresses">收货地址</router-link>
      <router-link to="/user/coupons">我的优惠券</router-link>
      <router-link to="/user/reviews">我的评价</router-link>
    </div>
  </div>
</template>
<script setup lang="ts">import { useUserStore } from '@supermarket/stores'; const userStore = useUserStore()</script>
<style scoped>
.center-page{background:#fff;padding:20px;border-radius:4px}
.user-info p{margin:4px 0;font-size:14px}
.center-links{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:16px}
.center-links a{padding:12px;background:#f8f8f8;text-align:center;border-radius:4px;color:#333;text-decoration:none}
.center-links a:hover{background:#fff0f0;color:#e1251b}
</style>
