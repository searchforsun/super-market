<template>
  <router-view />
</template>

<script setup lang="ts">
import { useUserStore, useCartStore } from '@supermarket/stores'

const userStore = useUserStore()
const cartStore = useCartStore()

// 初始化：如果已登录则拉取用户信息 + 购物车数量
if (userStore.isLoggedIn) {
  userStore.fetchUserInfo()
  cartStore.fetchCount()
}
</script>
