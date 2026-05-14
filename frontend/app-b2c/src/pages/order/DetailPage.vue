<template>
  <div class="detail-page" v-if="order">
    <h2>订单详情</h2>
    <OrderStatusTag :status="order.orderStatus" />
    <p class="order-no">订单号: {{ order.orderNo }}</p>
    <p class="order-amount">实付金额: <PriceDisplay :price="order.actualAmount" /></p>
    <p>创建时间: {{ order.createdAt }}</p>
    <p v-if="order.paidAt">支付时间: {{ order.paidAt }}</p>
    <div style="margin-top:16px">
      <el-button v-if="order.orderStatus===1" type="warning" @click="payOrder">去支付</el-button>
      <el-button v-if="order.orderStatus===1" @click="cancelOrder">取消订单</el-button>
      <el-button v-if="order.orderStatus===3" type="success" @click="confirmReceive">确认收货</el-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'; import { useRoute } from 'vue-router'; import { useUserStore } from '@supermarket/stores'
import { getOrderDetail, cancelOrder as cancelApi, confirmReceive as confirmApi } from '@supermarket/api'
import { createPayment } from '@supermarket/api'
import { OrderStatusTag, PriceDisplay } from '@supermarket/ui'; import { ElMessage } from 'element-plus'
const route = useRoute(); const userStore = useUserStore(); const order = ref<any>(null)
onMounted(async () => { order.value = await getOrderDetail(route.params.no as string) })
async function payOrder() {
  await createPayment(order.value.orderNo, userStore.userId, order.value.actualAmount, 1)
  ElMessage.success('模拟支付成功'); order.value.orderStatus = 2
}
async function cancelOrder() { await cancelApi(order.value.orderNo, '用户取消'); order.value.orderStatus = 5 }
async function confirmReceive() { await confirmApi(order.value.orderNo); order.value.orderStatus = 4 }
</script>
<style scoped>
.detail-page { background:#fff;padding:24px;border-radius:4px;max-width:600px;margin:0 auto }
.order-no{color:#999;margin:8px 0}.order-amount{font-size:18px;margin:8px 0}
</style>
