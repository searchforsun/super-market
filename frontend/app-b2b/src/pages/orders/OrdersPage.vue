<template>
  <div class="orders-page page-enter">
    <h2>订单处理</h2>

    <div class="filter-bar">
      <el-select
        v-model="queryParams.status"
        placeholder="订单状态"
        clearable
        style="width:140px"
        @change="handleSearch"
      >
        <el-option label="全部" :value="undefined" />
        <el-option
          v-for="(label, key) in ORDER_STATUS"
          :key="key"
          :label="label"
          :value="Number(key)"
        />
      </el-select>
      <el-input
        v-model="queryParams.orderNo"
        placeholder="搜索订单号"
        clearable
        style="width:220px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <el-table
      :data="orders"
      v-loading="loading"
      stripe
      border
      style="width:100%"
      empty-text="暂无订单数据"
    >
      <el-table-column label="订单号" prop="orderNo" min-width="180" show-overflow-tooltip />
      <el-table-column label="用户ID" prop="userId" width="80" />
      <el-table-column label="商品信息" min-width="180">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="$router.push(`/merchant/orders/${row.orderNo}`)">
            查看商品明细
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="140">
        <template #default="{ row }">
          <PriceDisplay :price="row.actualAmount" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <OrderStatusTag :status="row.orderStatus" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.orderStatus === 2"
            type="primary"
            size="small"
            @click="openShipDialog(row)"
          >
            发货
          </el-button>
          <el-button
            type="default"
            size="small"
            @click="$router.push(`/merchant/orders/${row.orderNo}`)"
          >
            查看详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="fetchOrders"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="shipDialogVisible" title="发货" width="420px" :close-on-click-modal="false">
      <el-form :model="shipForm" label-width="90px">
        <el-form-item label="物流公司" required>
          <el-input v-model="shipForm.company" placeholder="请输入物流公司名称" />
        </el-form-item>
        <el-form-item label="物流单号" required>
          <el-input v-model="shipForm.trackingNo" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipLoading" @click="confirmShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { OrderStatusTag, PriceDisplay } from '@supermarket/ui'
import { getMerchantOrderList, shipOrder } from '@supermarket/api'
import { ORDER_STATUS, USER_ID_KEY } from '@supermarket/utils'

const loading = ref(false)
const orders = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const queryParams = reactive<{ status: number | undefined; orderNo: string }>({
  status: undefined,
  orderNo: '',
})

const shipDialogVisible = ref(false)
const shipLoading = ref(false)
const currentShipOrderNo = ref('')
const shipForm = reactive({ company: '', trackingNo: '' })

function formatTime(val: string | undefined | null): string {
  if (!val) return ''
  return val.slice(0, 16).replace('T', ' ')
}

async function fetchOrders() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: pageSize.value,
    }
    if (queryParams.status) params.status = queryParams.status
    if (queryParams.orderNo) params.orderNo = queryParams.orderNo

    const userId = Number(localStorage.getItem(USER_ID_KEY)) || 0
    const res: any = await getMerchantOrderList(userId, params)
    orders.value = res.records || []
    total.value = res.total || 0
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchOrders()
}

function handleSizeChange(newSize: number) {
  pageSize.value = newSize
  page.value = 1
  fetchOrders()
}

function openShipDialog(row: any) {
  currentShipOrderNo.value = row.orderNo
  shipForm.company = ''
  shipForm.trackingNo = ''
  shipDialogVisible.value = true
}

async function confirmShip() {
  if (!shipForm.company || !shipForm.trackingNo) {
    ElMessage.warning('请填写完整的物流信息')
    return
  }
  shipLoading.value = true
  try {
    await shipOrder(currentShipOrderNo.value)
    ElMessage.success('发货成功')
    shipDialogVisible.value = false
    fetchOrders()
  } catch {
  } finally {
    shipLoading.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.orders-page {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
}

.orders-page h2 {
  margin: 0 0 20px;
  font-size: 18px;
  color: #333;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
