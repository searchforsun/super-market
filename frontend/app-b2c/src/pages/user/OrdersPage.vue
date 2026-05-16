<template>
  <div class="orders-page page-enter">
    <h2 class="page-title">我的订单</h2>

    <!-- Status Filter Tabs -->
    <div class="status-tabs">
      <span
        v-for="tab in tabs"
        :key="tab.value"
        :class="{ active: currentStatus === tab.value }"
        @click="changeStatus(tab.value)"
      >{{ tab.label }}</span>
    </div>

    <!-- Order List -->
    <div v-loading="loading">
      <template v-if="orders.length">
        <div
          v-for="order in orders"
          :key="order.orderNo"
          class="order-card"
          @click="$router.push(`/order/${order.orderNo}`)"
        >
          <div class="card-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <OrderStatusTag :status="order.orderStatus" />
          </div>

          <!-- Order Items Preview -->
          <div class="card-items" v-if="order.items?.length">
            <div v-for="item in order.items.slice(0, 4)" :key="item.id ?? item.skuId" class="mini-item">
              <el-image
                :src="item.skuImage || item.image"
                fit="cover"
                class="mini-image"
                lazy
              >
                <template #error>
                  <div class="mini-image-placeholder"></div>
                </template>
              </el-image>
            </div>
            <div v-if="order.items.length > 4" class="more-items">+{{ order.items.length - 4 }}</div>
          </div>

          <!-- Fallback: show first item info if no items array -->
          <div v-else-if="order.firstItemImage" class="card-items">
            <el-image :src="order.firstItemImage" fit="cover" class="mini-image" lazy>
              <template #error><div class="mini-image-placeholder"></div></template>
            </el-image>
          </div>

          <div class="card-footer">
            <span class="order-date">{{ order.createdAt }}</span>
            <span class="order-total">
              共{{ order.itemCount ?? order.items?.length ?? 1 }}件 合计：
              <PriceDisplay :price="order.actualAmount" />
            </span>
          </div>
        </div>

        <!-- Pagination -->
        <div class="pagination" v-if="total > size">
          <el-pagination
            background
            layout="prev, pager, next, total"
            :total="total"
            :page-size="size"
            :current-page="currentPage"
            @current-change="changePage"
          />
        </div>
      </template>

      <!-- Empty State -->
      <div v-else-if="!loading" class="empty-state">
        <el-empty description="暂无订单" :image-size="120" />
        <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
      </div>
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
const currentPage = ref(1)
const size = ref(10)
const total = ref(0)
const currentStatus = ref<number | undefined>(undefined)

const tabs = [
  { label: '全部', value: undefined as number | undefined },
  ...Object.entries(ORDER_STATUS).map(([key, label]) => ({
    label,
    value: Number(key),
  })),
]

async function changeStatus(status: number | undefined) {
  currentStatus.value = status
  currentPage.value = 1
  await fetchOrders()
}

async function changePage(page: number) {
  currentPage.value = page
  await fetchOrders()
  // Scroll to top
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function fetchOrders() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: currentPage.value,
      size: size.value,
    }
    if (currentStatus.value !== undefined) {
      params.status = currentStatus.value
    }
    const res: any = await getOrderList(userStore.userId, params)
    if (res?.records) {
      orders.value = res.records
      total.value = res.total ?? 0
    } else if (Array.isArray(res)) {
      orders.value = res
      total.value = res.length
    } else {
      orders.value = []
      total.value = 0
    }
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(fetchOrders)
</script>

<style scoped>
.orders-page {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 16px;
  color: #333;
}

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.status-tabs span {
  font-size: 13px;
  color: #666;
  cursor: pointer;
  padding: 6px 16px;
  border-radius: 20px;
  background: #f5f5f5;
  transition: all 0.2s;
}

.status-tabs span:hover {
  color: #e1251b;
  background: #fff0f0;
}

.status-tabs span.active {
  color: #fff;
  background: #e1251b;
  font-weight: 600;
}

.order-card {
  margin-bottom: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.order-card:hover {
  border-color: #e1251b;
  box-shadow: 0 2px 12px rgba(225, 37, 27, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
}

.order-no {
  font-size: 12px;
  color: #999;
}

.card-items {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
}

.mini-image {
  width: 64px;
  height: 64px;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
}

.mini-image-placeholder {
  width: 64px;
  height: 64px;
  background: #f5f5f5;
}

.more-items {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 13px;
  color: #999;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-top: 1px solid #f5f5f5;
}

.order-date {
  font-size: 12px;
  color: #999;
}

.order-total {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 16px;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
}
</style>
