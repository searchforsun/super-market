<template>
  <div class="result-page page-enter">
    <div class="result-card">
      <!-- Success State -->
      <template v-if="isSuccess">
        <div class="result-icon success">
          <el-icon :size="64"><CircleCheckFilled /></el-icon>
        </div>
        <h2 class="result-title">订单提交成功！</h2>
        <p class="result-desc">感谢您的购买，我们会尽快为您发货</p>
        <p class="order-no-display" v-if="orderNo">
          订单编号：<span class="order-no-value">{{ orderNo }}</span>
        </p>
        <div class="result-actions">
          <el-button type="primary" size="large" @click="$router.push(`/order/${orderNo}`)">
            查看订单
          </el-button>
          <el-button size="large" @click="$router.push('/')">
            继续购物
          </el-button>
        </div>
      </template>

      <!-- Failure State -->
      <template v-else>
        <div class="result-icon fail">
          <el-icon :size="64"><CircleCloseFilled /></el-icon>
        </div>
        <h2 class="result-title">订单提交失败</h2>
        <p class="result-desc">{{ errorMsg || '请稍后重试或联系客服' }}</p>
        <div class="result-actions">
          <el-button type="primary" size="large" @click="$router.back()">
            返回重试
          </el-button>
          <el-button size="large" @click="$router.push('/')">
            返回首页
          </el-button>
        </div>
      </template>

      <!-- Recommended Products -->
      <div class="recommend-section" v-if="isSuccess">
        <h3>猜你喜欢</h3>
        <p class="recommend-placeholder">浏览更多商品</p>
        <el-button type="primary" plain @click="$router.push('/')">去购物</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'

const route = useRoute()

const isSuccess = computed(() => {
  return route.query.success !== 'false' && !!route.query.orderNo
})

const orderNo = computed(() => (route.query.orderNo as string) || '')

const errorMsg = computed(() => (route.query.message as string) || '')
</script>

<style scoped>
.result-page {
  display: flex;
  justify-content: center;
  padding: 40px 16px;
}

.result-card {
  background: #fff;
  border-radius: 12px;
  padding: 48px 40px;
  text-align: center;
  max-width: 520px;
  width: 100%;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.04);
}

.result-icon {
  margin-bottom: 20px;
}

.result-icon.success {
  color: #67c23a;
}

.result-icon.fail {
  color: #f56c6c;
}

.result-title {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
}

.result-desc {
  font-size: 14px;
  color: #999;
  margin: 0 0 20px;
}

.order-no-display {
  font-size: 14px;
  color: #666;
  margin: 0 0 28px;
  padding: 10px 16px;
  background: #fafafa;
  border-radius: 6px;
  display: inline-block;
}

.order-no-value {
  font-weight: 600;
  color: #e1251b;
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.recommend-section {
  margin-top: 40px;
  padding-top: 28px;
  border-top: 1px solid #f0f0f0;
}

.recommend-section h3 {
  font-size: 15px;
  color: #666;
  margin: 0 0 8px;
  font-weight: 500;
}

.recommend-placeholder {
  font-size: 13px;
  color: #bbb;
  margin: 0 0 16px;
}
</style>
