import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAvailableCoupons, getUserCoupons, claimCoupon, getAvailableList } from '@supermarket/api'

export const useCouponStore = defineStore('coupon', () => {
  const availableCoupons = ref<any[]>([])
  const myCoupons = ref<any[]>([])
  const loading = ref(false)

  async function fetchAvailable(page = 1, size = 20) {
    loading.value = true
    try {
      const res: any = await getAvailableCoupons(page, size)
      availableCoupons.value = res?.records ?? res ?? []
    } finally {
      loading.value = false
    }
  }

  async function fetchMyCoupons(userId: number, status?: number) {
    loading.value = true
    try {
      const res: any = await getUserCoupons(userId, status)
      myCoupons.value = res?.data ?? res ?? []
    } finally {
      loading.value = false
    }
  }

  async function claim(userId: number, templateId: number) {
    await claimCoupon(userId, templateId)
    await fetchMyCoupons(userId)
  }

  async function fetchAvailableList(userId: number) {
    const res: any = await getAvailableList(userId)
    return res?.data ?? res ?? []
  }

  return { availableCoupons, myCoupons, loading, fetchAvailable, fetchMyCoupons, claim, fetchAvailableList }
})
