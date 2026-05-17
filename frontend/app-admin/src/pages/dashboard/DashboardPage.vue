<template>
  <div class="dashboard-page page-enter">
    <h2 class="page-title">运营看板</h2>

    <!-- Stat Cards -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="item in statCards" :key="item.label">
        <StatCard v-bind="item" />
      </el-col>
    </el-row>

    <!-- Charts Row 1: 销售趋势 + 分类占比 -->
    <el-row :gutter="16" class="charts-row">
      <el-col :span="15">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="section-title">近7日销售趋势</span></template>
          <div ref="trendChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="section-title">分类销售占比</span></template>
          <div ref="categoryChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row 2: 订单状态 + 用户增长 -->
    <el-row :gutter="16" class="charts-row">
      <el-col :span="9">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="section-title">订单状态分布</span></template>
          <div ref="statusChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="15">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="section-title">近7日用户增长</span></template>
          <div ref="userChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Recent Orders -->
    <el-card shadow="never" class="orders-card" v-loading="ordersLoading">
      <template #header>
        <span class="section-title">最近订单</span>
      </template>
      <el-table :data="recentOrders" stripe size="small" empty-text="暂无订单" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="200" />
        <el-table-column prop="userName" label="用户" width="100" />
        <el-table-column label="金额" width="130">
          <template #default="{ row }">{{ formatPrice(row.actualAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
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

    <!-- Pending Audit -->
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
import { formatPrice, formatDate, useECharts, echarts } from '@supermarket/utils'
import {
  getDashboardStats,
  getDailyTrend,
  getCategorySales,
  getStatusDistribution,
  getUserTrend,
  getAdminOrders,
  getPendingAudit,
} from '@supermarket/api'
import type { DailyTrend, CategorySales, StatusDistItem, UserTrendItem, RecentOrder } from '@supermarket/api'

// -- Stats --
const stats = reactive({ todayGmv: 0, todayOrderCount: 0, newUsers: 0, pendingMerchants: 0 })
const statCards = computed(() => [
  { label: '今日 GMV', value: `¥ ${stats.todayGmv.toLocaleString()}` },
  { label: '今日订单数', value: stats.todayOrderCount.toLocaleString() },
  { label: '新增用户', value: stats.newUsers.toLocaleString() },
  { label: '待审核商家', value: stats.pendingMerchants.toLocaleString() },
])

// -- Order status mapping --
const orderStatusMap: Record<string, string> = {
  PENDING_PAYMENT: '待支付', PENDING_SHIPMENT: '待发货', SHIPPED: '已发货',
  DELIVERED: '已送达', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDING: '退款中',
}
const orderStatusTag: Record<string, string> = {
  PENDING_PAYMENT: 'warning', PENDING_SHIPMENT: '', SHIPPED: 'primary',
  DELIVERED: 'success', COMPLETED: 'success', CANCELLED: 'danger', REFUNDING: 'danger',
}
function statusLabel(s: string) { return orderStatusMap[s] || s }
function statusTagType(s: string) { return orderStatusTag[s] || 'info' }

// -- Chart refs --
const trendChartRef = ref<HTMLElement | null>(null)
const categoryChartRef = ref<HTMLElement | null>(null)
const statusChartRef = ref<HTMLElement | null>(null)
const userChartRef = ref<HTMLElement | null>(null)

const trendData = ref<DailyTrend[]>([])
const categorySales = ref<CategorySales[]>([])
const statusDist = ref<StatusDistItem[]>([])
const userTrend = ref<UserTrendItem[]>([])

// -- Trend chart --
const { refresh: refreshTrend } = useECharts(trendChartRef, () => {
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
      { name: '订单数', type: 'line', yAxisIndex: 1, data: trendData.value.map(d => d.orderCount), itemStyle: { color: c.warning }, lineStyle: { width: 2 }, symbol: 'circle', symbolSize: 8 },
    ],
  }
})

// -- Category pie --
const { refresh: refreshCategory } = useECharts(categoryChartRef, () => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  series: [{
    type: 'pie', radius: ['50%', '75%'], center: ['50%', '55%'], avoidLabelOverlap: false,
    label: { show: false },
    emphasis: { label: { show: true, fontWeight: 'bold' } },
    data: categorySales.value.map(c => ({ name: c.categoryName, value: c.amount })),
  }],
}))

// -- Status distribution pie --
const { refresh: refreshStatus } = useECharts(statusChartRef, () => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0, textStyle: { color: echartsColors().text, fontSize: 11 } },
  series: [{
    type: 'pie', radius: ['45%', '70%'], center: ['50%', '50%'],
    label: { show: false },
    data: statusDist.value.map(s => ({ name: s.name, value: s.value })),
    itemStyle: { borderRadius: 4, borderColor: echartsColors().surface, borderWidth: 3 },
  }],
}))

// -- User growth bar --
const { refresh: refreshUser } = useECharts(userChartRef, () => {
  const c = echartsColors()
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '8%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: userTrend.value.map(d => d.date?.slice(5) || ''), axisLabel: { color: c.text }, axisLine: { lineStyle: { color: c.border } } },
    yAxis: { type: 'value', name: '新增用户', nameTextStyle: { color: c.text }, axisLabel: { color: c.text }, splitLine: { lineStyle: { color: c.border } } },
    series: [{
      type: 'bar', data: userTrend.value.map(d => d.count),
      itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: c.accent }, { offset: 1, color: c.surface },
      ]), borderRadius: [4, 4, 0, 0] },
      barWidth: '40%',
    }],
  }
})

// -- Orders --
const ordersLoading = ref(false)
const recentOrders = ref<RecentOrder[]>([])
const pendingAudit = reactive({ pendingMerchants: 0, pendingProducts: 0 })

function echartsColors() {
  const el = document.documentElement
  const css = (n: string) => getComputedStyle(el).getPropertyValue(n).trim()
  return {
    text: css('--color-text-secondary') || '#475569',
    accent: css('--color-accent') || '#2563eb',
    surface: css('--color-surface') || '#f8fafc',
    border: css('--color-border') || '#e2e8f0',
    warning: css('--color-warning') || '#d97706',
  }
}

async function fetchAll() {
  await Promise.all([
    (async () => {
      try { Object.assign(stats, await getDashboardStats()) } catch { /* keep defaults */ }
    })(),
    (async () => {
      try { trendData.value = await getDailyTrend(7); refreshTrend() } catch { trendData.value = [] }
    })(),
    (async () => {
      try { categorySales.value = await getCategorySales(); refreshCategory() } catch { categorySales.value = [] }
    })(),
    (async () => {
      try { statusDist.value = await getStatusDistribution(); refreshStatus() } catch { statusDist.value = [] }
    })(),
    (async () => {
      try { userTrend.value = await getUserTrend(7); refreshUser() } catch { userTrend.value = [] }
    })(),
    (async () => {
      ordersLoading.value = true
      try { const r = await getAdminOrders({ page: 1, size: 10 }); recentOrders.value = r.records || [] }
      catch { recentOrders.value = [] }
      finally { ordersLoading.value = false }
    })(),
    (async () => {
      try { Object.assign(pendingAudit, await getPendingAudit()) } catch { /* keep defaults */ }
    })(),
  ])
}

onMounted(fetchAll)
</script>

<style scoped>
.dashboard-page { padding: 20px; }
.page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: var(--color-text-primary, #303133); }
.stat-row { margin-bottom: 16px; }
.charts-row { margin-bottom: 16px; }
.orders-card { margin-bottom: 16px; }
.audit-row { margin-bottom: 0; }
.section-title { font-size: 15px; font-weight: 600; color: var(--color-text-primary, #333); }
.chart-card { height: 100%; }
.chart-box { width: 100%; height: 320px; }

.audit-stat { text-align: center; padding: 20px 0; }
.audit-stat-label { font-size: 14px; color: var(--color-text-muted, #888); margin-bottom: 8px; }
.audit-stat-value { font-size: 36px; font-weight: 700; color: var(--color-accent, #409eff); line-height: 1.2; }
.audit-stat-unit { font-size: 13px; color: var(--color-text-muted, #aaa); margin-top: 4px; }
</style>
