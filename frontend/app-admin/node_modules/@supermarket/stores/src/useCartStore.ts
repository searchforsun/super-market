import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCartCount } from '@supermarket/api'
import { useUserStore } from './useUserStore'

export const useCartStore = defineStore('cart', () => {
  const count = ref(0)

  async function fetchCount() {
    const userStore = useUserStore()
    if (!userStore.userId) return
    try {
      count.value = await getCartCount(userStore.userId)
    } catch { /* ignore */ }
  }

  const hasItems = computed(() => count.value > 0)

  return { count, fetchCount, hasItems }
})
