<template>
  <div class="center-page page-enter">
    <!-- User Info Card -->
    <div class="user-card">
      <div class="user-avatar">
        <span class="avatar-text">{{ avatarChar }}</span>
      </div>
      <div class="user-meta">
        <h3 class="user-nickname">{{ userStore.userInfo?.nickname || '用户' }}</h3>
        <p class="user-phone" v-if="userStore.userInfo?.phone">
          <el-icon><Phone /></el-icon> {{ maskedPhone }}
        </p>
      </div>
      <div class="user-extra">
        <el-button size="small" plain @click="$router.push('/user/center')">编辑资料</el-button>
      </div>
    </div>

    <!-- Member Level Card -->
    <div class="member-card" v-if="memberInfo">
      <div class="member-level">
        <span class="level-badge" :class="'level-' + memberLevel">
          {{ memberInfo.levelName || `Lv.${memberInfo.level}` }}
        </span>
        <span class="member-title">
          {{ memberInfo.levelName ? '会员' : `${memberInfo.level} 级会员` }}
        </span>
      </div>
      <div class="member-stats">
        <div class="stat-item">
          <span class="stat-value">{{ memberInfo.points ?? 0 }}</span>
          <span class="stat-label">积分</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ memberInfo.growth ?? memberInfo.growthValue ?? 0 }}</span>
          <span class="stat-label">成长值</span>
        </div>
      </div>
      <div class="growth-bar" v-if="nextLevelGrowth">
        <div class="growth-fill" :style="{ width: growthPercent + '%' }"></div>
        <span class="growth-text">距下一级还差 {{ growthRemaining }} 成长值</span>
      </div>
    </div>
    <div v-else-if="memberLoading" class="member-card skeleton">
      <el-skeleton animated :rows="2" />
    </div>

    <!-- Quick Links -->
    <div class="quick-links">
      <h3 class="section-title">我的服务</h3>
      <div class="links-grid">
        <router-link to="/user/orders" class="link-item">
          <div class="link-icon orders">
            <el-icon :size="22"><Document /></el-icon>
          </div>
          <span class="link-label">我的订单</span>
        </router-link>
        <router-link to="/user/addresses" class="link-item">
          <div class="link-icon address">
            <el-icon :size="22"><Location /></el-icon>
          </div>
          <span class="link-label">收货地址</span>
        </router-link>
        <router-link to="/user/coupons" class="link-item">
          <div class="link-icon coupon">
            <el-icon :size="22"><Ticket /></el-icon>
          </div>
          <span class="link-label">优惠券</span>
        </router-link>
        <router-link to="/user/reviews" class="link-item">
          <div class="link-icon review">
            <el-icon :size="22"><Star /></el-icon>
          </div>
          <span class="link-label">我的评价</span>
        </router-link>
        <router-link to="/user/notifications" class="link-item">
          <div class="link-icon notify">
            <el-icon :size="22"><Bell /></el-icon>
          </div>
          <span class="link-label">消息通知</span>
        </router-link>
        <router-link to="/cart" class="link-item">
          <div class="link-icon cart">
            <el-icon :size="22"><ShoppingCart /></el-icon>
          </div>
          <span class="link-label">购物车</span>
        </router-link>
      </div>
    </div>

    <!-- Activity / Extra -->
    <div class="extra-links">
      <h3 class="section-title">更多</h3>
      <div class="extra-grid">
        <router-link to="/seckill" class="extra-item">
          <span>限时秒杀</span>
          <el-icon><ArrowRight /></el-icon>
        </router-link>
        <router-link to="/" class="extra-item">
          <span>回到首页</span>
          <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@supermarket/stores'
import { getMemberInfo } from '@supermarket/api'
import {
  Phone, Document, Location, Ticket, Star, Bell, ShoppingCart, ArrowRight,
} from '@element-plus/icons-vue'

const userStore = useUserStore()

const memberInfo = ref<any>(null)
const memberLoading = ref(false)

const avatarChar = computed(() => {
  const name = userStore.userInfo?.nickname || '用户'
  return name.charAt(0).toUpperCase()
})

const maskedPhone = computed(() => {
  const phone = userStore.userInfo?.phone || ''
  if (phone.length === 11) {
    return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
  }
  return phone
})

const memberLevel = computed(() => {
  return memberInfo.value?.level ?? 1
})

const nextLevelGrowth = computed(() => {
  return memberInfo.value?.nextLevelGrowth ?? null
})

const growthPercent = computed(() => {
  if (!memberInfo.value) return 0
  const current = memberInfo.value.growth ?? memberInfo.value.growthValue ?? 0
  const next = nextLevelGrowth.value
  if (!next) return 0
  // Assume current level growth starts from previous threshold
  const prev = memberInfo.value.prevLevelGrowth ?? 0
  const range = next - prev
  if (range <= 0) return 100
  return Math.min(100, Math.round(((current - prev) / range) * 100))
})

const growthRemaining = computed(() => {
  if (!nextLevelGrowth.value) return 0
  const current = memberInfo.value?.growth ?? memberInfo.value?.growthValue ?? 0
  return Math.max(0, nextLevelGrowth.value - current)
})

async function fetchMemberInfo() {
  if (!userStore.userId) return
  memberLoading.value = true
  try {
    memberInfo.value = await getMemberInfo(userStore.userId)
  } catch {
    memberInfo.value = null
  } finally {
    memberLoading.value = false
  }
}

onMounted(() => {
  fetchMemberInfo()
})
</script>

<style scoped>
.center-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 0 32px;
}

/* User Card */
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: linear-gradient(135deg, #e1251b, #ff4d4f);
  border-radius: 12px;
  color: #fff;
  margin-bottom: 16px;
}

.user-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-text {
  font-size: 26px;
  font-weight: 600;
  color: #fff;
}

.user-meta {
  flex: 1;
  min-width: 0;
}

.user-nickname {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}

.user-phone {
  font-size: 13px;
  margin: 0;
  opacity: 0.85;
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-extra {
  flex-shrink: 0;
}

/* Member Card */
.member-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
}

.member-card.skeleton {
  padding: 24px;
}

.member-level {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.level-badge {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  background: #f0f0f0;
  color: #666;
}

.level-badge.level-1 { background: #f0f0f0; color: #666; }
.level-badge.level-2 { background: #e6f7ff; color: #1890ff; }
.level-badge.level-3 { background: #fff7e6; color: #fa8c16; }
.level-badge.level-4 { background: #f6ffed; color: #52c41a; }
.level-badge.level-5 { background: #fff0f0; color: #e1251b; }

.member-title {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.member-stats {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 12px;
}

.stat-item {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: #f0f0f0;
}

.growth-bar {
  height: 20px;
  background: #f5f5f5;
  border-radius: 10px;
  overflow: hidden;
  position: relative;
}

.growth-fill {
  height: 100%;
  background: linear-gradient(90deg, #ffa940, #ff4d4f);
  border-radius: 10px;
  transition: width 0.6s ease;
}

.growth-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  color: #666;
  white-space: nowrap;
}

/* Quick Links */
.quick-links {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px;
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.link-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  background: #fafafa;
  border-radius: 10px;
  text-decoration: none;
  color: #333;
  transition: all 0.2s;
}

.link-item:hover {
  background: #fff0f0;
  color: #e1251b;
  transform: translateY(-2px);
}

.link-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.link-icon.orders { background: #e6f7ff; color: #1890ff; }
.link-icon.address { background: #f6ffed; color: #52c41a; }
.link-icon.coupon { background: #fff7e6; color: #fa8c16; }
.link-icon.review { background: #fff0f0; color: #f5222d; }
.link-icon.notify { background: #f9f0ff; color: #722ed1; }
.link-icon.cart { background: #e6fffb; color: #13c2c2; }

.link-label {
  font-size: 12px;
  color: #666;
}

/* Extra Links */
.extra-links {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
}

.extra-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.extra-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  text-decoration: none;
  color: #333;
  font-size: 14px;
  transition: color 0.2s;
}

.extra-item:last-child {
  border-bottom: none;
}

.extra-item:hover {
  color: #e1251b;
}
</style>
