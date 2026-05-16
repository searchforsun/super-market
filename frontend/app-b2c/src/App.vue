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
