<template>
  <div class="search-page page-enter">
    <SearchBar @search="onSearch" />
    <div v-loading="loading" class="search-content">
      <div class="search-toolbar" v-if="results">
        <span class="result-count">共 {{ results.total }} 件商品</span>
        <div class="sort-options">
          <span v-for="s in sortOptions" :key="s.value" :class="{ active: currentSort === s.value }"
            @click="changeSort(s.value)">{{ s.label }}</span>
        </div>
        <div class="price-range">
          <el-input-number v-model="minPrice" :min="0" size="small" placeholder="最低价" controls-position="right" />
          <span class="sep">-</span>
          <el-input-number v-model="maxPrice" :min="0" size="small" placeholder="最高价" controls-position="right" />
          <el-button size="small" @click="doSearch">确定</el-button>
        </div>
      </div>
      <div v-if="results && results.records.length > 0" class="product-grid">
        <ProductCard v-for="p in results.records" :key="p.spuId" :image="p.mainImage" :title="p.name"
          :price="p.minPrice" :originalPrice="p.maxPrice" :sales="p.salesCount" :rating="p.avgRating"
          @click="$router.push(`/product/${p.spuId}`)" />
      </div>
      <div v-else-if="results && results.records.length === 0" class="empty-state">
        <p class="empty-icon">🔍</p>
        <p>没有找到相关商品</p>
        <p class="empty-hint">试试其他关键词或筛选条件</p>
      </div>
      <div v-if="results && results.total > 0" class="pagination">
        <el-pagination background layout="prev, pager, next" :total="results.total" :page-size="results.size"
          :current-page="results.page" @current-change="changePage" />
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
const loading = ref(false)
const currentSort = ref('newest')
const minPrice = ref<number | undefined>()
const maxPrice = ref<number | undefined>()

const sortOptions = [
  { label: '综合', value: 'newest' },
  { label: '销量', value: 'sales' },
  { label: '价格↑', value: 'price_asc' },
  { label: '价格↓', value: 'price_desc' },
]

async function onSearch(keyword: string) {
  router.push({ query: { ...route.query, q: keyword, page: '1' } })
  doSearch()
}

function changeSort(sort: string) {
  currentSort.value = sort
  router.push({ query: { ...route.query, sort, page: '1' } })
  doSearch()
}

function changePage(page: number) {
  router.push({ query: { ...route.query, page: String(page) } })
  doSearch()
}

async function doSearch() {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    const q = route.query
    if (q.q) params.keyword = q.q
    if (q.categoryId) params.categoryId = Number(q.categoryId)
    if (q.page) params.page = Number(q.page)
    if (q.size) params.size = Number(q.size)
    if (currentSort.value) params.sort = currentSort.value
    if (minPrice.value != null) params.minPrice = minPrice.value
    if (maxPrice.value != null) params.maxPrice = maxPrice.value
    results.value = await searchProducts(params)
  } catch {
    results.value = { total: 0, size: 20, page: 1, records: [] }
  } finally {
    loading.value = false
  }
}

onMounted(doSearch)
</script>

<style scoped>
.search-page { background: #fff; padding: 20px; border-radius: 4px; }
.search-content { min-height: 300px; }
.search-toolbar { display: flex; align-items: center; gap: 20px; margin: 12px 0 16px; flex-wrap: wrap; }
.result-count { font-size: 13px; color: #999; }
.sort-options { display: flex; gap: 8px; }
.sort-options span { font-size: 13px; color: #666; cursor: pointer; padding: 4px 10px; border-radius: 3px; }
.sort-options span.active { color: #e1251b; background: #fff0f0; font-weight: 600; }
.price-range { display: flex; align-items: center; gap: 6px; }
.price-range .sep { color: #999; font-size: 13px; }
.product-grid { display: flex; flex-wrap: wrap; gap: 12px; }
.empty-state { text-align: center; padding: 60px 0; color: #999; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-hint { font-size: 13px; }
.pagination { display: flex; justify-content: center; margin-top: 24px; }
</style>
