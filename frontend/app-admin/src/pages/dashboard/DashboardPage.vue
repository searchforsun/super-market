<template>
  <div class="dashboard-page page-enter">
    <h2 class="page-title">运营看板</h2>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="item in statCards" :key="item.label">
        <StatCard v-bind="item" />
      </el-col>
    </el-row>

    <el-row :gutter="16" class="charts-row">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span class="section-title">近7日销售趋势</span>
          </template>
          <el-table :data="trendData" v-loading="trendLoading" stripe size="small" empty-text="暂无数据">
            <el-table-column label="日期" width="120">
              <template #default="{ row }">{{ formatDate(row.date, 'MM-DD') }}</template>
            </el-table-column>
            <el-table-column label="GMV">
              <template #default="{ row }">{{ formatPrice(row.gmv) }}</template>
            </el-table-column>
            <el-table-column prop="orderCount" label="订单数" width="100" align="center" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span class="section-title">分类销售占比</span>
          </template>
          <el-table :data="categorySales" v-loading="categoryLoading" stripe size="small" empty-text="暂无数据">
            <el-table-column prop="categoryName" label="分类" min-width="100" />
            <el-table-column label="销售额" width="100">
              <template #default="{ row }">{{ formatPrice(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="占比" width="140">
              <template #default="{ row }">
                <div class="pct-cell">
                  <el-progress :percentage="Math.round(row.percentage)" :stroke-width="12" />
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="orders-card" v-loading="ordersLoading">
      <template #header>
        <span class="section-title">最近订单</span>
      </template>
      <el-table :data="recentOrders" stripe empty-text="暂无订单">
        <el-table-column prop="orderNo" label="订单号" width="220" />
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">{{ formatPrice(row.actualAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="plain">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-row :gutter="16" class="audit-row">
      <el-col :span="12">
        <el-card shadow="never">
          <div class="audit-stat">
            <div class="audit-stat-label">待审核商家</div>
            <div class="audit-stat-value">{{ pendingAudit.pendingMerchants }}</div>
            <div class="audit-stat-unit">家</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <div class="audit-stat">
            <div class="audit-stat-label">待审核商品</div>
            <div class="audit-stat-value">{{ pendingAudit.pendingProducts }}</div>
            <div class="audit-stat-unit">个</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { StatCard } from '@supermarket/ui'
import { formatPrice, formatDate } from '@supermarket/utils'
import {
  getDashboardStats,
  getDailyTrend,
  getCategorySales,
  getAdminOrders,
  getPendingAudit,
} from '@supermarket/api'
import type { DailyTrend, CategorySales, RecentOrder } from '@supermarket/api'

const stats = reactive({
  todayGmv: 0,
  todayOrderCount: 0,
  newUsers: 0,
  pendingMerchants: 0,
})

const statCards = computed(() => [
  { label: '今日 GMV', value: `¥ ${stats.todayGmv.toLocaleString()}` },
  { label: '今日订单数', value: stats.todayOrderCount.toLocaleString() },
  { label: '新增用户', value: stats.newUsers.toLocaleString() },
  { label: '待审核商家', value: stats.pendingMerchants.toLocaleString() },
])

const trendLoading = ref(false)
const trendData = ref<DailyTrend[]>([])

const categoryLoading = ref(false)
const categorySales = ref<CategorySales[]>([])

const ordersLoading = ref(false)
const recentOrders = ref<RecentOrder[]>([])

const pendingAudit = reactive({
  pendingMerchants: 0,
  pendingProducts: 0,
})

const orderStatusMap: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PENDING_SHIPMENT: '待发货',
  SHIPPED: '已发货',
  DELIVERED: '已送达',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
}

const orderStatusTag: Record<string, string> = {
  PENDING_PAYMENT: 'warning',
  PENDING_SHIPMENT: '',
  SHIPPED: 'primary',
  DELIVERED: 'success',
  COMPLETED: 'success',
  CANCELLED: 'danger',
  REFUNDING: 'danger',
}

function statusLabel(s: string): string {
  return orderStatusMap[s] || s
}

function statusTagType(s: string): string {
  return orderStatusTag[s] || 'info'
}

async function fetchStats() {
  try {
    const data = await getDashboardStats()
    stats.todayGmv = data.todayGmv
    stats.todayOrderCount = data.todayOrderCount
    stats.newUsers = data.newUsers
    stats.pendingMerchants = data.pendingMerchants
  } catch {
    stats.todayGmv = 0
  }
}

async function fetchTrend() {
  trendLoading.value = true
  try {
    trendData.value = await getDailyTrend(7)
  } catch {
    trendData.value = []
  } finally {
    trendLoading.value = false
  }
}

async function fetchCategorySales() {
  categoryLoading.value = true
  try {
    categorySales.value = await getCategorySales()
  } catch {
    categorySales.value = []
  } finally {
    categoryLoading.value = false
  }
}

async function fetchOrders() {
  ordersLoading.value = true
  try {
    const result = await getAdminOrders({ page: 1, size: 10 })
    recentOrders.value = result.records || []
  } catch {
    recentOrders.value = []
  } finally {
    ordersLoading.value = false
  }
}

async function fetchPendingAudit() {
  try {
    const data = await getPendingAudit()
    pendingAudit.pendingMerchants = data.pendingMerchants
    pendingAudit.pendingProducts = data.pendingProducts
  } catch {
    pendingAudit.pendingProducts = 0
  }
}

onMounted(async () => {
  await Promise.all([
    fetchStats(),
    fetchTrend(),
    fetchCategorySales(),
    fetchOrders(),
    fetchPendingAudit(),
  ])
})
</script>

<style scoped>
.dashboard-page {
  padding: 20px;
}

.page-title {
  margin: 0 0 20px;
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
}

.stat-row {
  margin-bottom: 16px;
}

.charts-row {
  margin-bottom: 16px;
}

.orders-card {
  margin-bottom: 16px;
}

.audit-row {
  margin-bottom: 0;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.pct-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.audit-stat {
  text-align: center;
  padding: 20px 0;
}

.audit-stat-label {
  font-size: 14px;
  color: #888;
  margin-bottom: 8px;
}

.audit-stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #409eff;
  line-height: 1.2;
}

.audit-stat-unit {
  font-size: 13px;
  color: #aaa;
  margin-top: 4px;
}
</style>
