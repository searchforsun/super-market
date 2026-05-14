<template>
  <div class="orders-page">
    <h2>我的订单</h2>
    <div v-for="o in orders" :key="o.orderNo" class="order-card" @click="$router.push(`/order/${o.orderNo}`)">
      <OrderStatusTag :status="o.orderStatus" />
      <span class="order-no">{{ o.orderNo }}</span>
      <PriceDisplay :price="o.actualAmount" />
      <span class="order-date">{{ o.createdAt }}</span>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'; import { useUserStore } from '@supermarket/stores'
import { getOrderList } from '@supermarket/api'; import { OrderStatusTag, PriceDisplay } from '@supermarket/ui'
const userStore = useUserStore(); const orders = ref<any[]>([])
onMounted(async () => { const res: any = await getOrderList(userStore.userId, { page: 1, size: 20 }); orders.value = res.records || [] })
</script>
<style scoped>
.orders-page{background:#fff;padding:20px;border-radius:4px}
.order-card{display:flex;align-items:center;gap:16px;padding:12px;border-bottom:1px solid #eee;cursor:pointer}
.order-card:hover{background:#fafafa}.order-no{font-size:12px;color:#999;flex:1}.order-date{font-size:11px;color:#999}
</style>
