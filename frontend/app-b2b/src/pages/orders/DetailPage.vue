<template>
  <div class="b2b-order-detail">
    <div class="page-header">
      <el-button :icon="ArrowLeft" text @click="goBack">返回</el-button>
      <h2>订单详情</h2>
    </div>

    <div v-loading="loading" class="detail-body">
      <template v-if="order">
        <el-card shadow="never" class="status-card">
          <div class="status-row">
            <span class="order-no">订单编号: {{ order.orderNo }}</span>
            <OrderStatusTag :status="order.orderStatus" />
          </div>
          <div class="time-row">创建时间: {{ formatDate(order.createdAt) }}</div>
        </el-card>

        <el-card shadow="never" class="section-card">
          <template #header>商品信息</template>
          <el-table :data="items" stripe border>
            <el-table-column label="商品" min-width="320">
              <template #default="{ row }">
                <div class="product-cell">
                  <el-image :src="row.skuImage" class="product-img" fit="contain">
                    <template #error>
                      <div class="img-placeholder">暂无图片</div>
                    </template>
                  </el-image>
                  <div class="product-meta">
                    <div class="product-name">{{ row.skuName }}</div>
                    <div class="product-spec">{{ row.skuSpec || '-' }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="140" align="right">
              <template #default="{ row }">
                <PriceDisplay :price="row.skuPrice" />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="80" align="center" prop="quantity" />
            <el-table-column label="小计" width="140" align="right">
              <template #default="{ row }">
                <PriceDisplay :price="row.totalPrice" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="section-card">
          <template #header>订单信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="收货地址" label-width="120px">
              {{ order.addressSnapshot || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="支付方式" label-width="120px">
              {{ payMethodText }}
            </el-descriptions-item>
            <el-descriptions-item label="商品金额" label-width="120px">
              <PriceDisplay :price="order.totalAmount" />
            </el-descriptions-item>
            <el-descriptions-item label="优惠金额" label-width="120px">
              <PriceDisplay :price="order.discountAmount" />
            </el-descriptions-item>
            <el-descriptions-item label="运费" label-width="120px">
              <PriceDisplay :price="order.freightAmount" />
            </el-descriptions-item>
            <el-descriptions-item label="实付金额" label-width="120px">
              <PriceDisplay :price="order.actualAmount" />
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="section-card">
          <template #header>订单进度</template>
          <el-timeline>
            <el-timeline-item
              v-for="(evt, i) in timelineEvents"
              :key="i"
              :timestamp="evt.time"
              :type="evt.type"
              placement="top"
            >
              {{ evt.label }}
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <div v-if="actionButtons.length" class="action-bar">
          <el-button
            v-for="(btn, i) in actionButtons"
            :key="i"
            :type="btn.type"
            size="large"
            @click="btn.handler"
          >
            {{ btn.label }}
          </el-button>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="未找到订单信息" />
    </div>

    <el-dialog v-model="shipDialogVisible" title="确认发货" width="480px" :close-on-click-modal="false">
      <el-form :model="shipForm" label-width="100px">
        <el-form-item label="物流公司">
          <el-input v-model="shipForm.company" placeholder="请输入物流公司名称" />
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipping" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getOrderDetail, getOrderItems, shipOrder, confirmReceive } from '@supermarket/api'
import { OrderStatusTag, PriceDisplay } from '@supermarket/ui'
import { PAY_METHODS, formatDate } from '@supermarket/utils'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const order = ref<Record<string, any> | null>(null)
const items = ref<any[]>([])
const shipDialogVisible = ref(false)
const shipping = ref(false)
const shipForm = ref({ company: '', trackingNo: '' })

const payMethodText = computed(() => {
  if (!order.value?.payMethod) return '-'
  return PAY_METHODS[order.value.payMethod] || '未知'
})

const timelineEvents = computed(() => {
  const events: { time: string; label: string; type: string }[] = []
  if (!order.value) return events
  if (order.value.createdAt) {
    events.push({ time: formatDate(order.value.createdAt), label: '创建订单', type: 'primary' })
  }
  if (order.value.paidAt) {
    events.push({ time: formatDate(order.value.paidAt), label: '支付完成', type: 'success' })
  }
  if (order.value.shippedAt) {
    events.push({ time: formatDate(order.value.shippedAt), label: '已发货', type: 'primary' })
  }
  if (order.value.receivedAt) {
    events.push({ time: formatDate(order.value.receivedAt), label: '已签收', type: 'success' })
  }
  return events
})

const actionButtons = computed(() => {
  const buttons: { type: string; label: string; handler: () => void }[] = []
  if (order.value?.orderStatus === 2) {
    buttons.push({ type: 'primary', label: '发货', handler: () => { shipDialogVisible.value = true } })
  }
  if (order.value?.orderStatus === 3) {
    buttons.push({ type: 'success', label: '标记已签收', handler: handleConfirmReceive })
  }
  return buttons
})

function goBack() {
  router.back()
}

async function fetchDetail() {
  const orderNo = route.params.no as string
  if (!orderNo) return
  loading.value = true
  try {
    order.value = await getOrderDetail(orderNo)
    items.value = await getOrderItems(orderNo)
  } catch {
    order.value = null
    items.value = []
  } finally {
    loading.value = false
  }
}

async function handleShip() {
  const orderNo = route.params.no as string
  shipping.value = true
  try {
    await shipOrder(orderNo)
    ElMessage.success('发货成功')
    shipDialogVisible.value = false
    shipForm.value = { company: '', trackingNo: '' }
    await fetchDetail()
  } finally {
    shipping.value = false
  }
}

async function handleConfirmReceive() {
  const orderNo = route.params.no as string
  try {
    await confirmReceive(orderNo)
    ElMessage.success('已标记签收')
    await fetchDetail()
  } catch {
    // handled by interceptor
  }
}

onMounted(fetchDetail)
</script>

<style scoped>
.b2b-order-detail {
  max-width: 960px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-card {
  border-radius: 6px;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.order-no {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.time-row {
  color: #909399;
  font-size: 13px;
}

.section-card {
  border-radius: 6px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-img {
  width: 60px;
  height: 60px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  flex-shrink: 0;
  background: #fafafa;
}

.img-placeholder {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #c0c4cc;
}

.product-meta {
  min-width: 0;
}

.product-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  line-height: 1.4;
}

.product-spec {
  font-size: 12px;
  color: #909399;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 8px 0;
}
</style>
