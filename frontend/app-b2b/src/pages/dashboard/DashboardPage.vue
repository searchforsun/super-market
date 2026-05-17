<template>
  <div class="merchant-dashboard page-enter">
    <h2 class="dash-title">数据看板</h2>

    <!-- Stat Cards -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6" v-for="item in statCards" :key="item.label">
        <StatCard v-bind="item" />
      </el-col>
    </el-row>

    <!-- Charts Row -->
    <el-row :gutter="16" class="charts-row">
      <el-col :span="15">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="section-title">近7日销售趋势</span></template>
          <div ref="trendChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="section-title">订单状态分布</span></template>
          <div ref="statusChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Recent Orders -->
    <el-card shadow="never" class="orders-card" v-loading="loading">
      <template #header><span class="section-title">最近订单</span></template>
      <el-table :data="recentOrders" stripe size="small" empty-text="暂无订单" style="width:100%">
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <OrderStatusTag :status="row.orderStatus" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140">
          <template #default="{ row }">{{ formatPrice(row.actualAmount || row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="下单时间" min-width="160">
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { StatCard, OrderStatusTag } from '@supermarket/ui'
import { formatPrice, useECharts, USER_ID_KEY } from '@supermarket/utils'
import {
  getOrderList,
  getProductList,
  getMerchantStats,
  getMerchantTrend,
  getMerchantStatusDistribution,
  getShopIdByUserId,
} from '@supermarket/api'
import type { DailyTrend, StatusDistItem } from '@supermarket/api'

const loading = ref(false)
const recentOrders = ref<any[]>([])

const stats = reactive({ todayOrders: 0, todayGmv: 0, productCount: 0, pendingShip: 0, pendingRefund: 0 })
const statCards = computed(() => [
  { label: '今日订单数', value: stats.todayOrders.toLocaleString() },
  { label: '今日 GMV', value: `¥ ${stats.todayGmv.toLocaleString()}` },
  { label: '商品数量', value: stats.productCount.toLocaleString() },
  { label: '待处理退款', value: stats.pendingRefund.toLocaleString() },
])

const trendChartRef = ref<HTMLElement | null>(null)
const statusChartRef = ref<HTMLElement | null>(null)
const trendData = ref<DailyTrend[]>([])
const statusDist = ref<StatusDistItem[]>([])

function getUserId(): number { return Number(localStorage.getItem(USER_ID_KEY)) || 0 }

function echartsColors() {
  const el = document.documentElement
  const css = (n: string) => getComputedStyle(el).getPropertyValue(n).trim()
  return {
    text: css('--color-text-secondary') || '#5c5a55',
    accent: css('--color-accent') || '#d97706',
    surface: css('--color-surface') || '#ffffff',
    border: css('--color-border') || '#e0ded6',
    success: css('--color-success') || '#059669',
    warning: css('--color-warning') || '#d97706',
    danger: css('--color-danger') || '#dc2626',
  }
}

// Sales trend chart
useECharts(trendChartRef, () => {
  const c = echartsColors()
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['GMV', '订单数'], bottom: 0, textStyle: { color: c.text } },
    grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: trendData.value.map(d => d.date?.slice(5) || ''), axisLabel: { color: c.text }, axisLine: { lineStyle: { color: c.border } } },
    yAxis: [
      { type: 'value', name: 'GMV (¥)', nameTextStyle: { color: c.text }, axisLabel: { color: c.text, formatter: (v: number) => v >= 1000 ? (v / 1000).toFixed(1) + 'k' : v.toString() }, splitLine: { lineStyle: { color: c.border } } },
      { type: 'value', name: '订单数', nameTextStyle: { color: c.text }, axisLabel: { color: c.text }, splitLine: { show: false } },
    ],
    series: [
      { name: 'GMV', type: 'bar', data: trendData.value.map(d => d.gmv), itemStyle: { color: c.accent, borderRadius: [4, 4, 0, 0] }, barWidth: '40%' },
      { name: '订单数', type: 'line', yAxisIndex: 1, data: trendData.value.map(d => d.orderCount), itemStyle: { color: c.success }, lineStyle: { width: 2 }, symbol: 'circle', symbolSize: 8 },
    ],
  }
})

// Status distribution pie
useECharts(statusChartRef, () => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0, textStyle: { color: echartsColors().text, fontSize: 11 } },
  series: [{
    type: 'pie', radius: ['45%', '70%'], center: ['50%', '50%'],
    label: { show: false },
    data: statusDist.value.map(s => ({ name: s.name, value: s.value })),
    itemStyle: { borderRadius: 4, borderColor: echartsColors().surface, borderWidth: 3 },
  }],
}))

async function fetchAll() {
  const userId = getUserId()
  if (!userId) return
  loading.value = true

  // Get shop ID
  let shopId = 0
  try { shopId = await getShopIdByUserId(userId) } catch { /* no shop */ }

  const promises: Promise<void>[] = [
    (async () => {
      try { const r: any = await getProductList({ page: 1, size: 1 }); stats.productCount = r.total || 0 } catch { /* keep default */ }
    })(),
    (async () => {
      try { const r: any = await getOrderList(userId, { page: 1, size: 10 }); recentOrders.value = (r.records || r.list || []).slice(0, 10) } catch { recentOrders.value = [] }
    })(),
    (async () => {
      try { const r: any = await getOrderList(userId, { page: 1, size: 1, status: 6 }); stats.pendingRefund = r.total || 0 } catch { /* keep default */ }
    })(),
  ]

  if (shopId) {
    promises.push(
      (async () => {
        try { const r = await getMerchantStats(shopId); stats.todayOrders = r.todayOrders; stats.todayGmv = r.todayGmv } catch { /* keep default */ }
      })(),
      (async () => {
        try { trendData.value = await getMerchantTrend(shopId, 7) } catch { trendData.value = [] }
      })(),
      (async () => {
        try { statusDist.value = await getMerchantStatusDistribution(shopId) } catch { statusDist.value = [] }
      })(),
    )
  }

  await Promise.all(promises)
  loading.value = false
}

onMounted(fetchAll)
</script>

<style scoped>
.merchant-dashboard {
  padding: 20px;
  background: var(--color-bg, #f5f5f0);
  background-image:
    linear-gradient(rgba(26,26,24,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(26,26,24,0.03) 1px, transparent 1px);
  background-size: 24px 24px;
}

.dash-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary, #1a1a18);
  margin-bottom: 20px;
}

.stats-row { margin-bottom: 16px; }
.charts-row { margin-bottom: 16px; }
.orders-card { margin-bottom: 0; }
.section-title { font-size: 15px; font-weight: 600; color: var(--color-text-primary, #1a1a18); }
.chart-card { height: 100%; }
.chart-box { width: 100%; height: 320px; }
</style>
