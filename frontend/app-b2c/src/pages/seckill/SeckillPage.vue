<template>
  <div class="seckill-page page-enter">
    <h2>⚡ 限时秒杀</h2>
    <div v-if="currentSession">
      <Countdown :endTime="currentSession.endTime" />
      <div class="product-grid" style="margin-top:16px">
        <div v-for="p in products" :key="p.id" class="sk-product">
          <img :src="p.skuImage || ''" width="150" />
          <p>{{ p.spuName }}</p>
          <PriceDisplay :price="p.seckillPrice" />
          <el-button type="danger" size="small" @click="doSeckill(p)">立即抢购</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@supermarket/stores'
import { getSeckillSessions, getSeckillProducts, executeSeckill } from '@supermarket/api'
import { PriceDisplay, Countdown } from '@supermarket/ui'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const currentSession = ref<any>(null)
const products = ref<any[]>([])

onMounted(async () => {
  const sessions: any[] = await getSeckillSessions()
  if (sessions.length) {
    currentSession.value = sessions[0]
    products.value = await getSeckillProducts(sessions[0].id)
  }
})

async function doSeckill(p: any) {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    const res: any = await executeSeckill(userStore.userId, p.id, 1)
    if (res.success) ElMessage.success('抢购成功！订单处理中')
    else ElMessage.warning(res.message || '抢购失败')
  } catch {
    // 错误已由拦截器处理
  }
}
</script>

<style scoped>
.seckill-page { background: #fff; padding: 20px; border-radius: 4px; }
.product-grid { display: flex; flex-wrap: wrap; gap: 16px; }
.sk-product { text-align: center; padding: 12px; border: 1px solid #eee; border-radius: 4px; width: 180px; }
</style>
