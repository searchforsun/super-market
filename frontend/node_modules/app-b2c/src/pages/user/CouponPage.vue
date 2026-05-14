<template>
  <div class="coupon-page"><h2>我的优惠券</h2>
    <div v-for="c in coupons" :key="c.id" class="coupon-card">
      <strong>{{ c.couponCode }}</strong> · 有效期至 {{ c.expireTime }}
      <OrderStatusTag :status="c.status" />
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'; import { useUserStore } from '@supermarket/stores'
import { getUserCoupons } from '@supermarket/api'; import { OrderStatusTag } from '@supermarket/ui'
const userStore = useUserStore(); const coupons = ref<any[]>([])
onMounted(async () => { const res: any = await getUserCoupons(userStore.userId); coupons.value = res.records || res || [] })
</script>
<style scoped>
.coupon-page{background:#fff;padding:20px;border-radius:4px}
.coupon-card{padding:12px;border:1px solid #eee;border-radius:4px;margin:4px 0;display:flex;justify-content:space-between;align-items:center}
</style>
