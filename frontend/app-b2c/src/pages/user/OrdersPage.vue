<template>
  <div class="orders-page page-enter">
    <h2>我的订单</h2>
    <div class="status-tabs">
      <span v-for="tab in tabs" :key="tab.value" :class="{ active: currentStatus === tab.value }"
        @click="changeStatus(tab.value)">{{ tab.label }}</span>
    </div>
    <div v-loading="loading">
      <template v-if="orders.length">
        <div v-for="o in orders" :key="o.orderNo" class="order-card" @click="$router.push(`/order/${o.orderNo}`)">
          <OrderStatusTag :status="o.orderStatus" />
          <span class="order-no">{{ o.orderNo }}</span>
          <PriceDisplay :price="o.actualAmount" />
          <span class="order-date">{{ o.createdAt }}</span>
        </div>
        <div class="pagination" v-if="total > size">
          <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
            :current-page="page" @current-change="changePage" />
        </div>
      </template>
      <div v-else class="empty-state">暂无订单</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@supermarket/stores'
import { getOrderList } from '@supermarket/api'
import { ORDER_STATUS } from '@supermarket/utils'
import { OrderStatusTag, PriceDisplay } from '@supermarket/ui'

const userStore = useUserStore()
const orders = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const currentStatus = ref<number | undefined>(undefined)

const tabs = [
  { label: '全部', value: undefined },
  ...Object.entries(ORDER_STATUS).map(([key, label]) => ({
    label,
    value: Number(key),
  })),
]

async function changeStatus(status: any) {
  currentStatus.value = status
  page.value = 1
  await fetchOrders()
}

async function changePage(p: number) {
  page.value = p
  await fetchOrders()
}

async function fetchOrders() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (currentStatus.value) params.status = currentStatus.value
    const res: any = await getOrderList(userStore.userId, params)
    orders.value = res.records || []
    total.value = res.total || 0
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchOrders)
</script>

<style scoped>
.orders-page { background: #fff; padding: 20px; border-radius: 4px; }
.status-tabs { display: flex; gap: 8px; margin: 12px 0 16px; }
.status-tabs span { font-size: 13px; color: #666; cursor: pointer; padding: 6px 14px; border-radius: 3px; background: #f4f4f4; }
.status-tabs span.active { color: #e1251b; background: #fff0f0; font-weight: 600; }
.order-card { display: flex; align-items: center; gap: 16px; padding: 12px; border-bottom: 1px solid #eee; cursor: pointer; }
.order-card:hover { background: #fafafa; }
.order-no { font-size: 12px; color: #999; flex: 1; }
.order-date { font-size: 11px; color: #999; }
.empty-state { text-align: center; padding: 60px 0; color: #999; }
.pagination { display: flex; justify-content: center; margin-top: 16px; }
</style>
