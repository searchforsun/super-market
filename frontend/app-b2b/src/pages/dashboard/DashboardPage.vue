<template>
  <div class="merchant-dashboard page-enter">
    <h2 class="dash-title">首页看板</h2>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <StatCard label="今日订单数" :value="stats.todayOrders" />
      </el-col>
      <el-col :span="6">
        <StatCard label="商品数量" :value="stats.productCount" />
      </el-col>
      <el-col :span="6">
        <StatCard label="待发货订单" :value="stats.pendingShip" />
      </el-col>
      <el-col :span="6">
        <StatCard label="待处理退款" :value="stats.pendingRefund" />
      </el-col>
    </el-row>

    <el-card class="orders-card" shadow="never">
      <template #header>
        <span>最近订单</span>
      </template>
      <el-table :data="recentOrders" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <OrderStatusTag :status="row.orderStatus" />
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="120" />
        <el-table-column prop="createTime" label="下单时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getOrderList, getProductList } from '@supermarket/api'
import { StatCard, OrderStatusTag } from '@supermarket/ui'
import { USER_ID_KEY } from '@supermarket/utils'

const loading = ref(false)
const recentOrders = ref<any[]>([])

const stats = reactive({
  todayOrders: 0,
  productCount: 0,
  pendingShip: 0,
  pendingRefund: 0,
})

function getUserId(): number {
  return Number(localStorage.getItem(USER_ID_KEY)) || 0
}

onMounted(async () => {
  const userId = getUserId()
  if (!userId) return

  loading.value = true

  try {
    const res: any = await getProductList({ page: 1, size: 1 })
    stats.productCount = res.total || 0
  } catch {}

  try {
    const res: any = await getOrderList(userId, { page: 1, size: 10 })
    const list = res.records || res.list || []
    recentOrders.value = list
    stats.todayOrders = list.length
  } catch {}

  try {
    const res: any = await getOrderList(userId, { page: 1, size: 1, status: 2 })
    stats.pendingShip = res.total || 0
  } catch {}

  try {
    const res: any = await getOrderList(userId, { page: 1, size: 1, status: 6 })
    stats.pendingRefund = res.total || 0
  } catch {}

  loading.value = false
})
</script>

<style scoped>
.merchant-dashboard {
  background: #f5f7fa;
}

.dash-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.orders-card :deep(.el-card__header) {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  border-bottom: 1px solid #f0f0f0;
}
</style>
