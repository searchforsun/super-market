<template>
  <div class="home">
    <div class="home-main">
      <!-- 左侧分类 -->
      <div class="category-side">
        <div v-for="cat in categories" :key="cat.id" class="cat-item"
             @click="$router.push(`/search?categoryId=${cat.id}`)">{{ cat.name }}</div>
      </div>
      <!-- 中部 Banner -->
      <div class="banner-area">
        <el-carousel height="400px">
          <el-carousel-item v-for="b in banners" :key="b.id">
            <a :href="b.linkUrl"><img :src="b.imageUrl" style="width:100%;height:100%;object-fit:cover" /></a>
          </el-carousel-item>
        </el-carousel>
      </div>
      <!-- 右侧秒杀 -->
      <div class="seckill-side">
        <div class="side-title">⚡ 限时秒杀</div>
        <div v-for="p in seckillProducts" :key="p.id" class="sk-item" @click="$router.push(`/product/${p.spuId}`)">
          <PriceDisplay :price="p.seckillPrice" />
          <span class="sk-stock">仅剩 {{ p.seckillStock }}</span>
        </div>
      </div>
    </div>
    <!-- 推荐商品 -->
    <div class="section">
      <h3>为你推荐</h3>
      <div class="product-grid">
        <ProductCard v-for="p in products" :key="p.spuId" :image="p.mainImage" :title="p.name"
          :price="p.minPrice" :originalPrice="p.maxPrice" :sales="p.salesCount"
          @click="$router.push(`/product/${p.spuId}`)" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCategoryTree } from '@supermarket/api'
import { getProductList } from '@supermarket/api'
import { getActiveBanners } from '@supermarket/api'
import { getSeckillSessions, getSeckillProducts } from '@supermarket/api'
import { ProductCard, PriceDisplay } from '@supermarket/ui'

const categories = ref<any[]>([])
const banners = ref<any[]>([])
const products = ref<any[]>([])
const seckillProducts = ref<any[]>([])

onMounted(async () => {
  try { categories.value = await getCategoryTree() } catch {}
  try { banners.value = await getActiveBanners() } catch {}
  try {
    const res: any = await getProductList({ page: 1, size: 8, sort: 'sales_desc' })
    products.value = res.records || []
  } catch {}
  try {
    const sessions: any[] = await getSeckillSessions()
    if (sessions.length) {
      seckillProducts.value = await getSeckillProducts(sessions[0].id) || []
    }
  } catch {}
})
</script>

<style scoped>
.home-main { display: flex; gap: 12px; margin-bottom: 24px; }
.category-side { width: 180px; background: #fff; padding: 8px 0; border: 1px solid #eee; }
.cat-item { padding: 6px 16px; font-size: 13px; cursor: pointer; }
.cat-item:hover { color: #e1251b; background: #fff0f0; }
.banner-area { flex: 1; }
.seckill-side { width: 180px; background: #fff; border: 1px solid #eee; padding: 8px; }
.side-title { font-weight: 600; font-size: 14px; color: #e1251b; margin-bottom: 8px; }
.sk-item { padding: 6px 0; border-bottom: 1px dashed #eee; cursor: pointer; }
.sk-stock { font-size: 11px; color: #999; }
.section { margin-top: 24px; }
.section h3 { font-size: 18px; margin-bottom: 12px; }
.product-grid { display: flex; flex-wrap: wrap; gap: 12px; }
</style>
