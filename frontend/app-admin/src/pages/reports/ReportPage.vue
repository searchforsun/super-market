<template>
  <div class="report-page page-enter">
    <h2 class="page-title">数据报表</h2>

    <div class="filter-bar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        class="date-range-picker"
      />
      <el-button type="primary" @click="handleQuery" :loading="loading">查询</el-button>
    </div>

    <el-tabs v-model="activeTab" class="report-tabs">
      <el-tab-pane label="GMV报表" name="gmv">
        <div class="stat-cards" v-if="gmvData">
          <StatCard label="总GMV" :value="'¥ ' + formatNumber(gmvData.totalGmv)" />
          <StatCard label="总订单数" :value="formatNumber(gmvData.totalOrders)" />
          <StatCard label="客单价" :value="'¥ ' + gmvData.avgOrderValue.toFixed(2)" />
          <StatCard
            label="环比增长"
            :value="(gmvData.gmvGrowth >= 0 ? '+' : '') + gmvData.gmvGrowth.toFixed(2) + '%'"
            :trend="gmvData.gmvGrowth >= 0 ? 'up' : 'down'"
            :trendValue="Math.abs(gmvData.gmvGrowth).toFixed(2) + '%'"
          />
        </div>
        <el-table
          :data="gmvData?.daily ?? []"
          stripe
          border
          v-loading="loading"
          style="margin-top: 16px"
          empty-text="暂无数据"
        >
          <el-table-column label="日期" prop="date" width="120" />
          <el-table-column label="GMV" min-width="140">
            <template #default="{ row }">
              <PriceDisplay :price="row.gmv" />
            </template>
          </el-table-column>
          <el-table-column label="订单数" prop="orders" width="100" align="center" />
          <el-table-column label="客单价" min-width="140">
            <template #default="{ row }">
              <PriceDisplay :price="row.avgPrice" />
            </template>
          </el-table-column>
          <el-table-column label="UV" prop="uv" width="100" align="center" />
          <el-table-column label="转化率" width="110" align="center">
            <template #default="{ row }">
              {{ (row.conversionRate * 100).toFixed(2) + '%' }}
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="订单报表" name="orders">
        <div class="stat-cards" v-if="orderData">
          <StatCard label="总订单" :value="formatNumber(orderData.totalOrders)" />
          <StatCard label="已完成" :value="formatNumber(orderData.completedOrders)" />
          <StatCard label="已取消" :value="formatNumber(orderData.cancelledOrders)" />
          <StatCard
            label="退款率"
            :value="orderData.refundRate.toFixed(2) + '%'"
            :trend="orderData.refundRate > 5 ? 'down' : 'up'"
            :trendValue="orderData.refundRate.toFixed(2) + '%'"
          />
        </div>
        <el-table
          :data="orderData?.daily ?? []"
          stripe
          border
          v-loading="loading"
          style="margin-top: 16px"
          empty-text="暂无数据"
        >
          <el-table-column label="日期" prop="date" width="120" />
          <el-table-column label="订单数" prop="orders" width="100" align="center" />
          <el-table-column label="完成数" prop="completed" width="100" align="center" />
          <el-table-column label="取消数" prop="cancelled" width="100" align="center" />
          <el-table-column label="退款数" prop="refunds" width="100" align="center" />
          <el-table-column label="完成率" min-width="220" align="center">
            <template #default="{ row }">
              <el-progress
                :percentage="Math.round(row.completionRate * 100)"
                :stroke-width="16"
                :text-inside="true"
                :status="row.completionRate >= 0.9 ? 'success' : row.completionRate >= 0.7 ? 'warning' : 'exception'"
              />
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="用户报表" name="users">
        <div class="stat-cards" v-if="userData">
          <StatCard label="新注册" :value="formatNumber(userData.newRegistrations)" />
          <StatCard label="总用户" :value="formatNumber(userData.totalUsers)" />
          <StatCard label="活跃用户" :value="formatNumber(userData.activeUsers)" />
          <StatCard label="日活" :value="formatNumber(userData.dailyActive)" />
        </div>
        <el-table
          :data="userData?.daily ?? []"
          stripe
          border
          v-loading="loading"
          style="margin-top: 16px"
          empty-text="暂无数据"
        >
          <el-table-column label="日期" prop="date" width="120" />
          <el-table-column label="新注册" prop="newRegistrations" width="110" align="center" />
          <el-table-column label="登录数" prop="logins" min-width="110" align="center" />
          <el-table-column label="下单用户" prop="orderingUsers" min-width="110" align="center" />
          <el-table-column label="新客下单" prop="newCustomerOrders" min-width="110" align="center" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { StatCard, PriceDisplay } from '@supermarket/ui'
import {
  getGmvReport,
  getOrderReport,
  getUserReport,
  type GmvReportData,
  type OrderReportData,
  type UserReportData,
} from '@supermarket/api'

const activeTab = ref('gmv')
const loading = ref(false)

const gmvData = ref<GmvReportData | null>(null)
const orderData = ref<OrderReportData | null>(null)
const userData = ref<UserReportData | null>(null)

function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatNumber(n: number): string {
  return n.toLocaleString('zh-CN')
}

const today = new Date()
const past = new Date(today)
past.setDate(past.getDate() - 7)

const dateRange = ref<[string, string]>([formatDate(past), formatDate(today)])

async function handleQuery() {
  const [startDate, endDate] = dateRange.value
  if (!startDate || !endDate) return
  loading.value = true
  try {
    const [gmv, orders, users] = await Promise.all([
      getGmvReport(startDate, endDate),
      getOrderReport(startDate, endDate),
      getUserReport(startDate, endDate),
    ])
    gmvData.value = gmv
    orderData.value = orders
    userData.value = users
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.report-page {
  background: var(--color-surface, #fff);
  padding: 24px;
  border-radius: 8px;
}

.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary, #303133);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.report-tabs {
  margin-top: 8px;
}

.date-range-picker {
  width: 260px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 4px;
}

@media (max-width: 1200px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
