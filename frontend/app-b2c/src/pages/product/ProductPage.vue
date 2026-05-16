<template>
  <div class="product-detail page-enter" v-if="product">
    <div class="detail-top">
      <div class="detail-img"><img :src="product.mainImage" /></div>
      <div class="detail-info">
        <h2>{{ product.name }}</h2>
        <p class="subtitle">{{ product.subtitle }}</p>
        <PriceDisplay :price="currentSku?.price || product.minPrice" :originalPrice="currentSku?.marketPrice" />
        <div class="detail-sku" v-if="skus.length">
          <SkuSelector :specs="specGroups" @select="onSpecSelect" />
        </div>
        <div class="detail-actions">
          <el-input-number v-model="qty" :min="1" :max="99" size="small" />
          <el-button type="danger" @click="addToCart">加入购物车</el-button>
          <el-button type="warning" @click="buyNow">立即购买</el-button>
        </div>
      </div>
    </div>
    <!-- 评价 -->
    <div class="detail-section">
      <h3>商品评价 ({{ ratingSummary?.avgRating }}分)</h3>
      <div v-for="r in reviews" :key="r.id" class="review-item">
        <RatingStars :rating="r.rating" />
        <p>{{ r.content }}</p>
        <span class="review-user">{{ r.isAnonymous ? '匿名用户' : `用户${r.userId}` }} · {{ r.createdAt }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore, useCartStore } from '@supermarket/stores'
import { getProductDetail, getProductSkus } from '@supermarket/api'
import { getReviewsBySpu, getRatingSummary } from '@supermarket/api'
import { addToCart as addCartApi } from '@supermarket/api'
import { PriceDisplay, SkuSelector, RatingStars } from '@supermarket/ui'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const product = ref<any>(null)
const skus = ref<any[]>([])
const currentSku = ref<any>(null)
const qty = ref(1)
const reviews = ref<any[]>([])
const ratingSummary = ref<any>(null)

const specGroups = computed(() => {
  const groups: Record<string, Set<string>> = {}
  skus.value.forEach((s: any) => {
    if (s.specName) {
      s.specName.split(';').forEach((pair: string) => {
        const [k, v] = pair.split(':')
        if (k && v) {
          if (!groups[k]) groups[k] = new Set()
          groups[k].add(v)
        }
      })
    }
  })
  return Object.entries(groups).map(([name, values]) => ({ name, values: [...values] }))
})

function onSpecSelect(name: string, value: string) {
  const found = skus.value.find((s: any) => s.specName?.includes(`${name}:${value}`))
  if (found) currentSku.value = found
}

async function addToCart() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  await addCartApi(userStore.userId, { skuId: currentSku.value?.id || skus.value[0]?.id, quantity: qty.value })
  ElMessage.success('已加入购物车')
  cartStore.fetchCount()
}

function buyNow() {
  addToCart()
  router.push('/cart')
}

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    product.value = await getProductDetail(id)
    skus.value = await getProductSkus(id)
    if (skus.value.length) currentSku.value = skus.value[0]
  } catch { /* product load failed */ }
  try {
    const res: any = await getReviewsBySpu(id, { page: 1, size: 5 })
    reviews.value = res.records || []
    ratingSummary.value = await getRatingSummary(id)
  } catch {}
})
</script>

<style scoped>
.detail-top { display: flex; gap: 24px; background: #fff; padding: 20px; border-radius: 4px; }
.detail-img { width: 400px; }
.detail-img img { width: 100%; }
.detail-info { flex: 1; }
.detail-info h2 { font-size: 20px; margin-bottom: 4px; }
.subtitle { color: #e1251b; font-size: 13px; margin-bottom: 12px; }
.detail-sku { margin: 16px 0; }
.detail-actions { display: flex; gap: 10px; align-items: center; margin-top: 20px; }
.detail-section { background: #fff; padding: 20px; margin-top: 16px; border-radius: 4px; }
.detail-section h3 { margin-bottom: 12px; }
.review-item { padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.review-user { font-size: 11px; color: #999; }
</style>
