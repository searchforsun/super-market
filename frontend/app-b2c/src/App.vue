<template>
  <router-view />
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
