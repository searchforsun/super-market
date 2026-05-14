<template>
  <div class="settlement-page">
    <el-card shadow="never" class="header-card">
      <div class="page-header">
        <h2>结算查询</h2>
      </div>
    </el-card>

    <el-card shadow="never" class="stats-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <StatCard label="本期订单数" :value="summary.orderCount" />
        </el-col>
        <el-col :span="6">
          <StatCard label="总金额" :value="'¥' + summary.totalAmount.toFixed(2)" />
        </el-col>
        <el-col :span="6">
          <StatCard label="已结算金额" :value="'¥' + summary.settledAmount.toFixed(2)" />
        </el-col>
        <el-col :span="6">
          <StatCard label="待结算金额" :value="'¥' + summary.pendingAmount.toFixed(2)" />
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="8">
          <el-date-picker
            v-model="dateRange"
            type="monthrange"
            range-separator="至"
            start-placeholder="结算开始月"
            end-placeholder="结算结束月"
            value-format="YYYY-MM"
            style="width: 100%"
            @change="handleDateChange"
          />
        </el-col>
        <el-col :span="4">
          <el-select v-model="statusFilter" placeholder="结算状态" clearable style="width: 100%" @change="handleFilterChange">
            <el-option label="全部" value="" />
            <el-option label="已结算" value="settled" />
            <el-option label="待结算" value="pending" />
            <el-option label="处理中" value="processing" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="fetchSettlements">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="settlements"
        stripe
        style="width: 100%"
        v-loading="loading"
        :row-key="(row: SettlementRecord) => row.id"
      >
        <el-table-column prop="settlementNo" label="结算单号" width="200" />
        <el-table-column label="结算周期" width="200">
          <template #default="{ row }: { row: SettlementRecord }">
            {{ row.periodStart }} ~ {{ row.periodEnd }}
          </template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="100" align="center" />
        <el-table-column label="订单金额" width="130" align="right">
          <template #default="{ row }: { row: SettlementRecord }">
            <PriceDisplay :price="row.orderAmount" />
          </template>
        </el-table-column>
        <el-table-column prop="refundAmount" label="退款金额" width="110" align="right">
          <template #default="{ row }: { row: SettlementRecord }">
            ¥{{ row.refundAmount.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="fee" label="手续费" width="100" align="right">
          <template #default="{ row }: { row: SettlementRecord }">
            ¥{{ row.fee.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="实结金额" width="140" align="right">
          <template #default="{ row }: { row: SettlementRecord }">
            <PriceDisplay :price="row.netAmount" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }: { row: SettlementRecord }">
            <el-tag v-if="row.status === 'settled'" type="success">已结算</el-tag>
            <el-tag v-else-if="row.status === 'pending'" type="warning">待结算</el-tag>
            <el-tag v-else type="info">处理中</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }: { row: SettlementRecord }">
            <el-button link type="primary" size="small" @click="showDetail(row)">查看明细</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="结算明细" width="900" :close-on-click-modal="false">
      <template v-if="currentSettlement">
        <div class="detail-summary">
          <el-descriptions :column="4" border size="small">
            <el-descriptions-item label="结算单号">{{ currentSettlement.settlementNo }}</el-descriptions-item>
            <el-descriptions-item label="结算周期">{{ currentSettlement.periodStart }} ~ {{ currentSettlement.periodEnd }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag v-if="currentSettlement.status === 'settled'" type="success" size="small">已结算</el-tag>
              <el-tag v-else-if="currentSettlement.status === 'pending'" type="warning" size="small">待结算</el-tag>
              <el-tag v-else type="info" size="small">处理中</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="订单数">{{ currentSettlement.orderCount }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <el-table :data="detailOrders" stripe style="width: 100%" v-loading="detailLoading" class="detail-table">
          <el-table-column prop="orderNo" label="订单号" width="180" />
          <el-table-column label="订单金额" width="120" align="right">
            <template #default="{ row }: { row: OrderItem }">
              <PriceDisplay :price="row.amount" />
            </template>
          </el-table-column>
          <el-table-column prop="refundAmount" label="退款金额" width="110" align="right">
            <template #default="{ row }: { row: OrderItem }">
              ¥{{ (row.refundAmount || 0).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column prop="fee" label="手续费" width="100" align="right">
            <template #default="{ row }: { row: OrderItem }">
              ¥{{ (row.fee || 0).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="实结金额" width="130" align="right">
            <template #default="{ row }: { row: OrderItem }">
              <PriceDisplay :price="row.netAmount || row.amount" />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="下单时间" width="170" />
        </el-table>
        <div class="detail-total">
          <span>小计：</span>
          <PriceDisplay :price="detailTotal" />
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getOrderList } from '@supermarket/api'
import { PriceDisplay, StatCard } from '@supermarket/ui'

interface SettlementRecord {
  id: string
  settlementNo: string
  periodStart: string
  periodEnd: string
  orderCount: number
  orderAmount: number
  refundAmount: number
  fee: number
  netAmount: number
  status: 'settled' | 'pending' | 'processing'
}

interface OrderItem {
  orderNo: string
  amount: number
  refundAmount?: number
  fee?: number
  netAmount?: number
  createTime: string
  status: string
}

interface SummaryData {
  orderCount: number
  totalAmount: number
  settledAmount: number
  pendingAmount: number
}

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const settlements = ref<SettlementRecord[]>([])
const detailOrders = ref<OrderItem[]>([])
const currentSettlement = ref<SettlementRecord | null>(null)
const dateRange = ref<[string, string]>(['', ''])
const statusFilter = ref('')

const summary = computed<SummaryData>(() => {
  const list = settlements.value
  const orderCount = list.reduce((s, r) => s + r.orderCount, 0)
  const totalAmount = list.reduce((s, r) => s + r.orderAmount, 0)
  const settledAmount = list.filter(r => r.status === 'settled').reduce((s, r) => s + r.netAmount, 0)
  const pendingAmount = list.filter(r => r.status !== 'settled').reduce((s, r) => s + r.netAmount, 0)
  return { orderCount, totalAmount, settledAmount, pendingAmount }
})

const detailTotal = computed(() => {
  return detailOrders.value.reduce((s, r) => s + (r.netAmount || r.amount), 0)
})

function getMonthRange(offset: number) {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + offset
  const ym = new Date(y, m, 1)
  const yStr = ym.getFullYear()
  const mStr = String(ym.getMonth() + 1).padStart(2, '0')
  const lastDay = new Date(yStr, ym.getMonth() + 1, 0).getDate()
  return {
    start: `${yStr}-${mStr}-01`,
    end: `${yStr}-${mStr}-${lastDay}`,
    label: `${yStr}-${mStr}`,
  }
}

function generateSettlementNo(yearMonth: string, index: number) {
  return `STL${yearMonth.replace('-', '')}${String(index).padStart(4, '0')}`
}

function computeSettlements(orders: any[]) {
  const groups: Record<string, any[]> = {}
  for (const order of orders) {
    if (!order.createTime) continue
    const ym = order.createTime.substring(0, 7)
    if (!groups[ym]) groups[ym] = []
    groups[ym].push(order)
  }

  const statuses: Array<SettlementRecord['status']> = ['settled', 'pending', 'processing']
  let idx = 0
  const result: SettlementRecord[] = []
  const sortedKeys = Object.keys(groups).sort()

  for (const ym of sortedKeys) {
    const group = groups[ym]
    const period = getMonthRange(0)
    const isCurrentPeriod = ym === period.label
    const status: SettlementRecord['status'] = isCurrentPeriod ? 'pending' : statuses[idx % statuses.length]

    let orderAmount = 0
    let refundAmount = 0
    let fee = 0
    for (const o of group) {
      const amt = Number(o.totalAmount || o.amount || o.totalPrice || 0)
      orderAmount += amt
      refundAmount += Number(o.refundAmount || 0)
      fee += amt * 0.006
    }
    const netAmount = orderAmount - refundAmount - fee

    idx++
    result.push({
      id: `stl-${ym}`,
      settlementNo: generateSettlementNo(ym, idx),
      periodStart: `${ym}-01`,
      periodEnd: `${ym}-${new Date(Number(ym.substring(0, 4)), Number(ym.substring(5, 7)), 0).getDate()}`,
      orderCount: group.length,
      orderAmount: Math.round(orderAmount * 100) / 100,
      refundAmount: Math.round(refundAmount * 100) / 100,
      fee: Math.round(fee * 100) / 100,
      netAmount: Math.round(netAmount * 100) / 100,
      status,
    })
  }

  return result
}

async function fetchSettlements() {
  loading.value = true
  try {
    const userId = Number(localStorage.getItem('user_id') || 0)
    const params: Record<string, any> = {
      page: currentPage.value,
      size: 200,
    }
    if (dateRange.value[0]) {
      params.startMonth = dateRange.value[0]
    }
    if (dateRange.value[1]) {
      params.endMonth = dateRange.value[1]
    }

    const res = await getOrderList(userId, params)
    const orders = Array.isArray(res) ? res : (res?.records || res?.list || [])
    let list = computeSettlements(orders)

    if (statusFilter.value) {
      list = list.filter(r => r.status === statusFilter.value)
    }

    total.value = list.length
    const start = (currentPage.value - 1) * pageSize.value
    settlements.value = list.slice(start, start + pageSize.value)
  } catch {
    settlements.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function showDetail(record: SettlementRecord) {
  currentSettlement.value = record
  detailVisible.value = true
  detailLoading.value = true
  detailOrders.value = []

  try {
    const userId = Number(localStorage.getItem('user_id') || 0)
    const res = await getOrderList(userId, {
      page: 1,
      size: 200,
    })
    const orders = Array.isArray(res) ? res : (res?.records || res?.list || [])
    const ym = record.periodStart.substring(0, 7)
    const matched = orders.filter((o: any) => {
      if (!o.createTime) return false
      return o.createTime.substring(0, 7) === ym
    })

    detailOrders.value = matched.map((o: any) => {
      const amount = Number(o.totalAmount || o.amount || o.totalPrice || 0)
      const refund = Number(o.refundAmount || 0)
      const fee = amount * 0.006
      return {
        orderNo: o.orderNo || o.orderId || '',
        amount,
        refundAmount: refund,
        fee: Math.round(fee * 100) / 100,
        netAmount: Math.round((amount - refund - fee) * 100) / 100,
        createTime: o.createTime || '',
        status: o.status || '',
      }
    })
  } catch {
    detailOrders.value = []
  } finally {
    detailLoading.value = false
  }
}

function handleDateChange() {
  currentPage.value = 1
  fetchSettlements()
}

function handleFilterChange() {
  currentPage.value = 1
  fetchSettlements()
}

function resetFilter() {
  dateRange.value = ['', '']
  statusFilter.value = ''
  currentPage.value = 1
  fetchSettlements()
}

function handleSizeChange(val: number) {
  pageSize.value = val
  currentPage.value = 1
  fetchSettlements()
}

function handleCurrentChange(val: number) {
  currentPage.value = val
  fetchSettlements()
}

onMounted(() => {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  const prev = new Date(y, m - 2, 1)
  const prevStr = `${prev.getFullYear()}-${String(prev.getMonth() + 1).padStart(2, '0')}`
  const curStr = `${y}-${String(m + 1).padStart(2, '0')}`
  dateRange.value = [prevStr, curStr]
  fetchSettlements()
})
</script>

<style scoped>
.settlement-page {
  padding: 0;
}

.header-card {
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.stats-card {
  margin-bottom: 16px;
}

.filter-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding: 8px 0;
}

.detail-summary {
  margin-bottom: 20px;
}

.detail-table {
  margin-bottom: 16px;
}

.detail-total {
  text-align: right;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  padding: 8px 0;
}
</style>
