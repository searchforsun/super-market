<template>
  <div class="seckill-page page-enter">
    <div class="seckill-header">
      <h2 class="seckill-title">限时秒杀</h2>
      <p class="seckill-subtitle">超值好货，限时抢购</p>
    </div>

    <!-- Loading State -->
    <el-skeleton v-if="sessionLoading" animated :rows="3" style="padding: 20px" />

    <!-- Session Tabs -->
    <div v-else-if="sessions.length" class="session-area">
      <div class="session-tabs">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-tab"
          :class="{ active: currentSessionId === session.id }"
          @click="switchSession(session)"
        >
          <span class="session-name">{{ session.name }}</span>
          <span class="session-time">{{ formatSessionTime(session) }}</span>
          <span class="session-status" v-if="getSessionStatus(session) === 'ongoing'">抢购中</span>
          <span class="session-status upcoming" v-else-if="getSessionStatus(session) === 'upcoming'">即将开始</span>
        </div>
      </div>

      <!-- Current Session Info -->
      <div v-if="currentSession" class="current-session">
        <div class="session-countdown" v-if="sessionStatus === 'ongoing' && currentCountdownTarget">
          <span class="countdown-label">距结束</span>
          <el-countdown
            :value="currentCountdownTarget"
            format="HH:mm:ss"
            value-style="color:#e1251b;font-size:20px;font-weight:700"
          />
        </div>
        <div class="session-countdown upcoming-info" v-else-if="sessionStatus === 'upcoming' && currentCountdownTarget">
          <span class="countdown-label">距开始</span>
          <el-countdown
            :value="currentCountdownTarget"
            format="HH:mm:ss"
            value-style="color:#ff9800;font-size:20px;font-weight:700"
          />
        </div>
      </div>

      <!-- Product Grid -->
      <div v-loading="productLoading">
        <div v-if="products.length" class="product-grid">
          <div
            v-for="p in products"
            :key="p.id ?? p.seckillProductId"
            class="sk-product-card"
            @click="$router.push(`/product/${p.productId || p.spuId}`)"
          >
            <!-- Product Image -->
            <div class="sk-image-wrapper">
              <el-image
                :src="p.skuImage || p.image"
                fit="cover"
                class="sk-image"
                lazy
              >
                <template #error>
                  <div class="sk-image-fallback">暂无图片</div>
                </template>
              </el-image>
              <!-- Sold out overlay -->
              <div v-if="p.stock <= 0" class="sold-out-mask">
                <span>已抢光</span>
              </div>
            </div>

            <!-- Product Info -->
            <div class="sk-info">
              <p class="sk-name">{{ p.spuName || p.productName }}</p>
              <div class="sk-price-row">
                <span class="sk-seckill-price">
                  <PriceDisplay :price="p.seckillPrice" />
                </span>
                <span class="sk-original-price" v-if="p.originalPrice">
                  <PriceDisplay :price="p.originalPrice" />
                </span>
              </div>

              <!-- Stock Progress -->
              <div class="sk-stock-bar" v-if="p.totalStock || p.stockCount">
                <el-progress
                  :percentage="getStockPercent(p)"
                  :stroke-width="6"
                  :show-text="false"
                  :color="getStockPercent(p) > 50 ? '#e1251b' : '#ff9800'"
                />
                <span class="stock-text">已抢{{ p.soldCount ?? p.sold ?? 0 }}件</span>
              </div>

              <!-- Buy Button -->
              <el-button
                type="danger"
                size="small"
                class="sk-buy-btn"
                :disabled="p.stock <= 0 || sessionStatus !== 'ongoing'"
                :loading="buyingId === (p.id ?? p.seckillProductId)"
                @click.stop="handleBuy(p)"
              >
                {{ p.stock <= 0 ? '已抢光' : sessionStatus === 'upcoming' ? '即将开始' : '立即抢购' }}
              </el-button>
            </div>
          </div>
        </div>

        <!-- Empty -->
        <el-empty v-else description="暂无秒杀商品" :image-size="100" />
      </div>
    </div>

    <!-- No Sessions -->
    <el-empty v-else description="暂无秒杀场次" :image-size="120">
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@supermarket/stores'
import {
  getSeckillSessions,
  getSeckillProducts,
  executeSeckill,
} from '@supermarket/api'
import { PriceDisplay } from '@supermarket/ui'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const sessions = ref<any[]>([])
const currentSessionId = ref<number | null>(null)
const products = ref<any[]>([])
const sessionLoading = ref(true)
const productLoading = ref(false)
const buyingId = ref<number | null>(null)

const currentSession = computed(() => {
  return sessions.value.find(s => s.id === currentSessionId.value) || null
})

const sessionStatus = computed(() => {
  if (!currentSession.value) return 'ended'
  const now = Date.now()
  const start = new Date(currentSession.value.startTime).getTime()
  const end = new Date(currentSession.value.endTime).getTime()
  if (now < start) return 'upcoming'
  if (now >= start && now < end) return 'ongoing'
  return 'ended'
})

const currentCountdownTarget = computed(() => {
  if (!currentSession.value) return null
  if (sessionStatus.value === 'ongoing') {
    return new Date(currentSession.value.endTime).getTime()
  }
  if (sessionStatus.value === 'upcoming') {
    return new Date(currentSession.value.startTime).getTime()
  }
  return null
})

function getSessionStatus(session: any): string {
  const now = Date.now()
  const start = new Date(session.startTime).getTime()
  const end = new Date(session.endTime).getTime()
  if (now < start) return 'upcoming'
  if (now >= start && now < end) return 'ongoing'
  return 'ended'
}

function formatSessionTime(session: any): string {
  const start = new Date(session.startTime)
  const end = new Date(session.endTime)
  const fmt = (d: Date) =>
    `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  return `${fmt(start)}-${fmt(end)}`
}

function getStockPercent(product: any): number {
  const total = product.totalStock || product.stockCount || 1
  const sold = product.soldCount || product.sold || 0
  return Math.min(100, Math.round((sold / total) * 100))
}

async function fetchSessions() {
  sessionLoading.value = true
  try {
    sessions.value = (await getSeckillSessions()) as unknown as any[]
    // Auto-select the first ongoing or upcoming session
    const now = Date.now()
    let best = sessions.value.find(
      s => now >= new Date(s.startTime).getTime() && now < new Date(s.endTime).getTime(),
    )
    if (!best) {
      best = sessions.value.find(s => new Date(s.startTime).getTime() > now)
    }
    if (!best && sessions.value.length) {
      best = sessions.value[0]
    }
    if (best) {
      await switchSession(best)
    }
  } catch {
    sessions.value = []
  } finally {
    sessionLoading.value = false
  }
}

async function switchSession(session: any) {
  currentSessionId.value = session.id
  productLoading.value = true
  try {
    products.value = (await getSeckillProducts(session.id)) as unknown as any[]
  } catch {
    products.value = []
  } finally {
    productLoading.value = false
  }
}

async function handleBuy(product: any) {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }

  const productId = product.id ?? product.seckillProductId
  buyingId.value = productId

  try {
    const res: any = await executeSeckill(userStore.userId, productId, 1)
    if (res?.success || res?.code === 200) {
      ElMessage.success('抢购成功！请前往订单页面查看')
      // Update local stock
      product.stock = Math.max(0, (product.stock || 0) - 1)
      const sold = (product.soldCount ?? product.sold ?? 0) + 1
      product.soldCount = sold
      product.sold = sold
    } else {
      ElMessage.warning(res?.message || '抢购失败，请重试')
    }
  } catch {
    // Handled by interceptor
  } finally {
    buyingId.value = null
  }
}

onMounted(fetchSessions)
</script>

<style scoped>
.seckill-page {
  max-width: 1000px;
  margin: 0 auto;
}

.seckill-header {
  text-align: center;
  padding: 32px 0 20px;
}

.seckill-title {
  font-size: 28px;
  font-weight: 700;
  color: #e1251b;
  margin: 0;
}

.seckill-subtitle {
  font-size: 14px;
  color: #999;
  margin: 6px 0 0;
}

/* Session Tabs */
.session-area {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
}

.session-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.session-tab {
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid #f0f0f0;
  transition: all 0.2s;
  text-align: center;
}

.session-tab:hover {
  border-color: #ff9999;
}

.session-tab.active {
  border-color: #e1251b;
  background: #fff0f0;
}

.session-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.session-time {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.session-status {
  display: inline-block;
  font-size: 11px;
  color: #e1251b;
  background: #fff0f0;
  padding: 1px 6px;
  border-radius: 3px;
  margin-top: 4px;
}

.session-status.upcoming {
  color: #ff9800;
  background: #fff8e1;
}

/* Current Session Countdown */
.current-session {
  margin-bottom: 16px;
}

.session-countdown {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  background: #fff5f5;
  border-radius: 8px;
}

.session-countdown.upcoming-info {
  background: #fff8e1;
}

.countdown-label {
  font-size: 13px;
  color: #666;
}

/* Product Grid */
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

@media (max-width: 900px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 640px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.sk-product-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.sk-product-card:hover {
  border-color: #e1251b;
  box-shadow: 0 4px 16px rgba(225, 37, 27, 0.1);
  transform: translateY(-2px);
}

.sk-image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
  background: #fafafa;
}

.sk-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.sk-image-fallback {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 12px;
}

.sold-out-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.sk-info {
  padding: 10px 12px;
}

.sk-name {
  font-size: 13px;
  color: #333;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sk-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 6px 0;
}

.sk-seckill-price {
  font-size: 16px;
  font-weight: 700;
  color: #e1251b;
}

.sk-original-price {
  font-size: 12px;
  color: #bbb;
  text-decoration: line-through;
}

.sk-stock-bar {
  margin: 6px 0;
}

.stock-text {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  display: block;
}

.sk-buy-btn {
  width: 100%;
  margin-top: 8px;
}
</style>
