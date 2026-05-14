<template>
  <div class="search-page">
    <SearchBar @search="onSearch" />
    <div class="search-results" v-if="results">
      <p class="result-count">共 {{ results.total }} 件商品</p>
      <div class="product-grid">
        <ProductCard v-for="p in results.records" :key="p.spuId" :image="p.mainImage" :title="p.name"
          :price="p.minPrice" :originalPrice="p.maxPrice" :sales="p.salesCount" :rating="p.avgRating"
          @click="$router.push(`/product/${p.spuId}`)" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchProducts } from '@supermarket/api'
import { SearchBar, ProductCard } from '@supermarket/ui'

const route = useRoute()
const router = useRouter()
const results = ref<any>(null)

async function onSearch(keyword: string) {
  router.push({ query: { ...route.query, q: keyword } })
  doSearch()
}

async function doSearch() {
  try { results.value = await searchProducts(route.query) } catch {}
}

onMounted(doSearch)
</script>

<style scoped>
.search-page { background: #fff; padding: 20px; border-radius: 4px; }
.result-count { font-size: 13px; color: #999; margin: 12px 0; }
.product-grid { display: flex; flex-wrap: wrap; gap: 12px; }
</style>
