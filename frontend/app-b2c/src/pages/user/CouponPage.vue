<template>
  <div class="coupon-page page-enter">
    <h2 class="page-title">优惠券</h2>

    <el-tabs v-model="activeTab" class="coupon-tabs">
      <!-- Tab 1: Available Coupon Center -->
      <el-tab-pane label="领券中心" name="available">
        <div v-loading="availableLoading">
          <div v-if="availableCoupons.length" class="coupon-grid">
            <div
              v-for="coupon in availableCoupons"
              :key="coupon.id ?? coupon.templateId"
              class="coupon-card available"
            >
              <div class="coupon-face">
                <div class="coupon-value">
                  <template v-if="coupon.discountType === 2 || coupon.type === 2">
                    <span class="coupon-number">{{ coupon.discountValue || coupon.discount }}</span>
                    <span class="coupon-unit">折</span>
                  </template>
                  <template v-else>
                    <span class="coupon-currency">¥</span>
                    <span class="coupon-number">{{ coupon.discountValue || coupon.amount || coupon.value }}</span>
                  </template>
                </div>
                <div class="coupon-condition" v-if="coupon.minAmount">
                  满{{ coupon.minAmount }}元可用
                </div>
                <div class="coupon-condition" v-else>无门槛</div>
              </div>
              <div class="coupon-body">
                <p class="coupon-name">{{ coupon.templateName || coupon.name || coupon.couponCode }}</p>
                <p class="coupon-expire">
                  有效期至 {{ coupon.expireTime || coupon.validEndTime || coupon.endTime }}
                </p>
                <el-button
                  type="primary"
                  size="small"
                  :loading="claimingId === (coupon.id ?? coupon.templateId)"
                  @click.stop="handleClaim(coupon)"
                >立即领取</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无可领优惠券" :image-size="100" />
        </div>
      </el-tab-pane>

      <!-- Tab 2: My Coupons -->
      <el-tab-pane label="我的优惠券" name="my">
        <!-- Status Filter -->
        <div class="my-filter">
          <el-radio-group v-model="myStatusFilter" size="small" @change="fetchMyCoupons">
            <el-radio-button :value="undefined" v-if="false">全部</el-radio-button>
            <el-radio-button :value="1">可用</el-radio-button>
            <el-radio-button :value="2">已用</el-radio-button>
            <el-radio-button :value="3">已过期</el-radio-button>
          </el-radio-group>
        </div>

        <div v-loading="myLoading">
          <div v-if="myCoupons.length" class="coupon-list">
            <div
              v-for="c in myCoupons"
              :key="c.id"
              class="coupon-item"
              :class="{ used: c.status === 2, expired: c.status === 3 }"
            >
              <div class="coupon-item-left">
                <div class="coupon-item-value">
                  <template v-if="c.discountType === 2 || c.type === 2">
                    <span class="item-number">{{ c.discountValue || c.discount }}</span>
                    <span class="item-unit">折</span>
                  </template>
                  <template v-else>
                    <span class="item-currency">¥</span>
                    <span class="item-number">{{ c.discountValue || c.amount || c.value }}</span>
                  </template>
                </div>
                <span class="item-condition" v-if="c.minAmount">满{{ c.minAmount }}元可用</span>
                <span class="item-condition" v-else>无门槛</span>
              </div>
              <div class="coupon-item-right">
                <p class="item-name">{{ c.templateName || c.name || c.couponCode }}</p>
                <p class="item-expire">有效期至 {{ c.expireTime || c.validEndTime }}</p>
              </div>
              <div class="coupon-item-status">
                <el-tag :type="couponStatusType(c.status)" size="small" effect="plain">
                  {{ couponStatusText(c.status) }}
                </el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无优惠券" :image-size="100">
            <el-button type="primary" @click="activeTab = 'available'">去领券</el-button>
          </el-empty>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@supermarket/stores'
import {
  getCouponTemplates,
  getAvailableCoupons,
  getUserCoupons,
  claimCoupon,
} from '@supermarket/api'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const activeTab = ref('available')

// Available coupons
const availableCoupons = ref<any[]>([])
const availableLoading = ref(false)
const claimingId = ref<number | null>(null)

// My coupons
const myCoupons = ref<any[]>([])
const myLoading = ref(false)
const myStatusFilter = ref<number | undefined>(undefined)

function couponStatusText(s: number): string {
  const map: Record<number, string> = { 1: '可用', 2: '已用', 3: '已过期' }
  return map[s] || '未知'
}

function couponStatusType(s: number): 'primary' | 'success' | 'info' | 'warning' | 'danger' | undefined {
  const map: Record<number, string> = { 1: 'primary', 2: 'info', 3: 'warning' }
  return (map[s] as any) || 'info'
}

async function fetchAvailableCoupons() {
  availableLoading.value = true
  try {
    // Try fetching templates first, fall back to available coupons
    try {
      const res: any = await getCouponTemplates(1, 50)
      availableCoupons.value = res?.records ?? res?.data ?? []
    } catch {
      const res: any = await getAvailableCoupons(1, 50)
      availableCoupons.value = res?.records ?? res ?? []
    }
    // If empty, try the other endpoint
    if (!availableCoupons.value.length) {
      try {
        const res: any = await getAvailableCoupons(1, 50)
        availableCoupons.value = res?.records ?? res ?? []
      } catch { /* ignore */ }
    }
  } catch {
    availableCoupons.value = []
  } finally {
    availableLoading.value = false
  }
}

async function fetchMyCoupons() {
  myLoading.value = true
  try {
    const res: any = await getUserCoupons(userStore.userId, myStatusFilter.value)
    myCoupons.value = res?.records ?? res?.data ?? res ?? []
    if (!Array.isArray(myCoupons.value)) {
      myCoupons.value = []
    }
  } catch {
    myCoupons.value = []
  } finally {
    myLoading.value = false
  }
}

async function handleClaim(coupon: any) {
  const templateId = coupon.id ?? coupon.templateId
  claimingId.value = templateId
  try {
    await claimCoupon(userStore.userId, templateId)
    ElMessage.success('领取成功')
    // Refresh my coupons
    await fetchMyCoupons()
  } catch {
    // Error handled by interceptor
  } finally {
    claimingId.value = null
  }
}

onMounted(() => {
  fetchAvailableCoupons()
  fetchMyCoupons()
})
</script>

<style scoped>
.coupon-page {
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

.coupon-tabs {
  margin-top: 4px;
}

/* Available Tab - Grid */
.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
  margin-top: 8px;
}

.coupon-card {
  display: flex;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s;
}

.coupon-card.available:hover {
  border-color: #e1251b;
  box-shadow: 0 2px 12px rgba(225, 37, 27, 0.1);
}

.coupon-face {
  width: 100px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e1251b, #ff4d4f);
  color: #fff;
  padding: 16px 8px;
}

.coupon-value {
  display: flex;
  align-items: baseline;
}

.coupon-currency {
  font-size: 14px;
  margin-right: 2px;
}

.coupon-number {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.coupon-unit {
  font-size: 14px;
  margin-left: 2px;
}

.coupon-condition {
  font-size: 11px;
  margin-top: 6px;
  opacity: 0.9;
}

.coupon-body {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: #fff;
}

.coupon-name {
  font-size: 13px;
  color: #333;
  margin: 0;
  font-weight: 500;
}

.coupon-expire {
  font-size: 11px;
  color: #bbb;
  margin: 4px 0 8px;
}

/* My Tab - List */
.my-filter {
  margin-bottom: 16px;
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.coupon-item {
  display: flex;
  align-items: center;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s;
}

.coupon-item.used {
  opacity: 0.7;
}

.coupon-item.expired {
  opacity: 0.5;
}

.coupon-item-left {
  width: 100px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff6b6b, #e1251b);
  color: #fff;
  padding: 12px 8px;
}

.used .coupon-item-left,
.expired .coupon-item-left {
  background: linear-gradient(135deg, #bbb, #999);
}

.item-currency {
  font-size: 13px;
  margin-right: 2px;
}

.item-number {
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}

.item-unit {
  font-size: 13px;
  margin-left: 2px;
}

.item-condition {
  font-size: 10px;
  margin-top: 4px;
  opacity: 0.9;
}

.coupon-item-right {
  flex: 1;
  padding: 12px 16px;
  min-width: 0;
}

.item-name {
  font-size: 13px;
  color: #333;
  margin: 0;
  font-weight: 500;
}

.item-expire {
  font-size: 11px;
  color: #999;
  margin: 4px 0 0;
}

.coupon-item-status {
  padding-right: 16px;
  flex-shrink: 0;
}
</style>
