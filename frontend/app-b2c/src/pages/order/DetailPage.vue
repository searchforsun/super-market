<template>
  <div class="order-detail-page page-enter">
    <el-skeleton :loading="loading" animated :count="1">
      <template #template>
        <div style="padding:24px">
          <el-skeleton-item variant="text" style="width:60%;height:24px;margin-bottom:16px" />
          <el-skeleton-item variant="text" style="width:40%;height:20px;margin-bottom:12px" />
          <el-skeleton-item variant="rect" style="height:120px;margin-bottom:16px" />
          <el-skeleton-item variant="text" style="width:80%;height:20px" />
        </div>
      </template>

      <template v-if="order">
        <!-- Status Header -->
        <div class="status-header">
          <OrderStatusTag :status="order.orderStatus" />
          <el-countdown
            v-if="order.orderStatus === 1 && paymentDeadline"
            :value="paymentDeadline"
            format="HH:mm:ss"
            :title="'支付剩余时间'"
            value-style="color:#e1251b;font-size:16px;font-weight:600"
            style="margin-left:16px"
          />
        </div>

        <!-- Order Info -->
        <div class="info-section">
          <div class="info-row">
            <span class="info-label">订单编号</span>
            <span class="info-value">{{ order.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">下单时间</span>
            <span class="info-value">{{ order.createdAt }}</span>
          </div>
          <div v-if="order.paidAt" class="info-row">
            <span class="info-label">支付时间</span>
            <span class="info-value">{{ order.paidAt }}</span>
          </div>
          <div v-if="order.shippedAt" class="info-row">
            <span class="info-label">发货时间</span>
            <span class="info-value">{{ order.shippedAt }}</span>
          </div>
          <div v-if="order.deliveredAt" class="info-row">
            <span class="info-label">收货时间</span>
            <span class="info-value">{{ order.deliveredAt }}</span>
          </div>
        </div>

        <!-- Line Items -->
        <div class="items-section">
          <h3 class="section-title">商品信息</h3>
          <div v-if="items.length" class="items-list">
            <div v-for="item in items" :key="item.id ?? item.skuId" class="item-row">
              <el-image
                :src="item.skuImage || item.image"
                fit="cover"
                class="item-image"
                :preview-src-list="[item.skuImage || item.image]"
              >
                <template #error>
                  <div class="image-error">暂无图片</div>
                </template>
              </el-image>
              <div class="item-info">
                <p class="item-name">{{ item.skuName || item.spuName }}</p>
                <p class="item-spec" v-if="item.skuSpec">{{ item.skuSpec }}</p>
              </div>
              <div class="item-price-qty">
                <PriceDisplay :price="item.price || item.skuPrice" />
                <span class="item-qty">x{{ item.quantity || item.buyCount }}</span>
              </div>
              <div class="item-subtotal">
                <PriceDisplay :price="(item.price || item.skuPrice || 0) * (item.quantity || item.buyCount || 1)" />
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无商品信息" :image-size="80" />
        </div>

        <!-- Amount Summary -->
        <div class="amount-section">
          <h3 class="section-title">金额明细</h3>
          <div class="amount-row">
            <span class="amount-label">商品总额</span>
            <PriceDisplay :price="order.totalAmount" />
          </div>
          <div class="amount-row" v-if="order.discountAmount">
            <span class="amount-label">优惠减免</span>
            <span class="discount-value">-<PriceDisplay :price="order.discountAmount" /></span>
          </div>
          <div class="amount-row" v-if="order.freightAmount">
            <span class="amount-label">运费</span>
            <PriceDisplay :price="order.freightAmount" />
          </div>
          <div class="amount-row amount-total">
            <span class="amount-label">实付金额</span>
            <span class="total-value"><PriceDisplay :price="order.actualAmount" /></span>
          </div>
        </div>

        <!-- Address -->
        <div class="address-section" v-if="addressSnapshot">
          <h3 class="section-title">收货信息</h3>
          <div class="address-detail">
            <p><strong>{{ addressSnapshot.receiverName }}</strong> {{ addressSnapshot.receiverPhone }}</p>
            <p>{{ addressSnapshot.province }}{{ addressSnapshot.city }}{{ addressSnapshot.district }} {{ addressSnapshot.detail }}</p>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="action-bar" v-if="showActions">
          <el-button
            v-if="order.orderStatus === 1"
            type="danger"
            size="large"
            @click="handlePay"
            :loading="actionLoading"
          >立即支付</el-button>
          <el-button
            v-if="order.orderStatus === 1"
            size="large"
            @click="handleCancel"
            :loading="actionLoading"
          >取消订单</el-button>
          <el-button
            v-if="order.orderStatus === 3"
            type="success"
            size="large"
            @click="handleConfirmReceive"
            :loading="actionLoading"
          >确认收货</el-button>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="订单不存在" :image-size="120" />
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@supermarket/stores'
import { getOrderDetail, getOrderItems, cancelOrder, confirmReceive } from '@supermarket/api'
import { createPayment } from '@supermarket/api'
import { OrderStatusTag, PriceDisplay } from '@supermarket/ui'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const order = ref<any>(null)
const items = ref<any[]>([])
const loading = ref(true)
const actionLoading = ref(false)

const addressSnapshot = computed(() => {
  if (!order.value?.addressSnapshot) return null
  try {
    return typeof order.value.addressSnapshot === 'string'
      ? JSON.parse(order.value.addressSnapshot)
      : order.value.addressSnapshot
  } catch {
    return null
  }
})

const showActions = computed(() => {
  return order.value && [1, 3].includes(order.value.orderStatus)
})

// Payment deadline: 30 minutes from order creation
const paymentDeadline = computed(() => {
  if (!order.value?.createdAt) return null
  const created = new Date(order.value.createdAt).getTime()
  if (isNaN(created)) return null
  return created + 30 * 60 * 1000
})

async function fetchOrder() {
  loading.value = true
  try {
    const orderNo = route.params.no as string
    order.value = await getOrderDetail(orderNo)
    try {
      const res: any = await getOrderItems(orderNo)
      items.value = Array.isArray(res) ? res : (res?.records ?? res?.data ?? [])
    } catch {
      items.value = []
    }
  } catch {
    order.value = null
    items.value = []
  } finally {
    loading.value = false
  }
}

async function handlePay() {
  actionLoading.value = true
  try {
    await createPayment(order.value.orderNo, userStore.userId, order.value.actualAmount, 1)
    ElMessage.success('支付成功')
    await fetchOrder()
  } catch {
    // 错误已由拦截器处理
  } finally {
    actionLoading.value = false
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确认取消该订单吗？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '再想想',
      type: 'warning',
    })
    actionLoading.value = true
    await cancelOrder(order.value.orderNo, '用户取消')
    ElMessage.success('订单已取消')
    await fetchOrder()
  } catch {
    if (actionLoading.value) {
      actionLoading.value = false
    }
  }
}

async function handleConfirmReceive() {
  try {
    await ElMessageBox.confirm('确认已收到商品吗？', '提示', {
      confirmButtonText: '确认收货',
      cancelButtonText: '再等等',
      type: 'success',
    })
    actionLoading.value = true
    await confirmReceive(order.value.orderNo)
    ElMessage.success('已确认收货')
    await fetchOrder()
  } catch {
    if (actionLoading.value) {
      actionLoading.value = false
    }
  }
}

onMounted(fetchOrder)
</script>

<style scoped>
.order-detail-page {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}

.status-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px;
}

.info-section {
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
}
.info-row {
  display: flex;
  padding: 6px 0;
  font-size: 13px;
}
.info-label {
  color: #999;
  width: 80px;
  flex-shrink: 0;
}
.info-value {
  color: #333;
}

.items-section {
  margin-bottom: 20px;
}
.items-list {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}
.item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
}
.item-row:last-child {
  border-bottom: none;
}
.item-image {
  width: 72px;
  height: 72px;
  border-radius: 4px;
  flex-shrink: 0;
}
.image-error {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #ccc;
  font-size: 11px;
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  font-size: 13px;
  color: #333;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-spec {
  font-size: 11px;
  color: #999;
  margin: 4px 0 0;
}
.item-price-qty {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}
.item-qty {
  font-size: 12px;
  color: #999;
}
.item-subtotal {
  flex-shrink: 0;
  font-weight: 600;
  font-size: 14px;
  min-width: 80px;
  text-align: right;
}

.amount-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}
.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
}
.amount-label {
  color: #666;
}
.amount-total {
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px dashed #e0e0e0;
  font-size: 16px;
}
.total-value {
  color: #e1251b;
  font-weight: 700;
}
.discount-value {
  color: #e1251b;
}

.address-section {
  margin-bottom: 20px;
}
.address-detail {
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
  font-size: 13px;
  color: #333;
}
.address-detail p {
  margin: 4px 0;
}

.action-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}
</style>
