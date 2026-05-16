<template>
  <div class="coupon-page"><h2>我的优惠券</h2>
    <div v-if="coupons.length === 0" class="empty">暂无优惠券</div>
    <div v-for="c in coupons" :key="c.id" class="coupon-card" :class="{ expired: c.status === 3 }">
      <div class="coupon-info">
        <strong>{{ c.couponCode || c.name }}</strong>
        <span class="coupon-expire">有效期至 {{ c.expireTime || c.validEndTime }}</span>
      </div>
      <el-tag :type="couponStatusType(c.status)" size="small" effect="plain">
        {{ couponStatusText(c.status) }}
      </el-tag>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'; import { useUserStore } from '@supermarket/stores'
import { getUserCoupons } from '@supermarket/api'
const userStore = useUserStore(); const coupons = ref<any[]>([])
function couponStatusText(s: number): string {
  const map: Record<number, string> = { 1: '未使用', 2: '已使用', 3: '已过期' }
  return map[s] || '未知'
}
function couponStatusType(s: number): string {
  const map: Record<number, string> = { 1: 'success', 2: 'info', 3: 'warning' }
  return map[s] || 'info'
}
onMounted(async () => { const res: any = await getUserCoupons(userStore.userId); coupons.value = res.records || res || [] })
</script>
<style scoped>
.coupon-page{background:#fff;padding:20px;border-radius:4px}
.empty{text-align:center;padding:40px 0;color:#999;font-size:14px}
.coupon-card{padding:12px 16px;border:1px solid #eee;border-radius:4px;margin:8px 0;display:flex;justify-content:space-between;align-items:center}
.coupon-card.expired{opacity:.6}
.coupon-info{display:flex;flex-direction:column;gap:4px}
.coupon-expire{font-size:12px;color:#999}
</style>
