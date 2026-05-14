<template>
  <div class="audit-page">
    <h2 class="page-title">商品审核</h2>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="待审核" name="0" />
      <el-tab-pane label="已通过" name="1" />
      <el-tab-pane label="已驳回" name="2" />
    </el-tabs>

    <div class="filter-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select
        v-model="categoryId"
        placeholder="选择类目"
        clearable
        @change="handleSearch"
      >
        <el-option
          v-for="cat in flatCategories"
          :key="cat.id"
          :label="cat.name"
          :value="cat.id"
        />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <el-table
      :data="productList"
      v-loading="loading"
      stripe
      border
      style="width: 100%"
      empty-text="暂无数据"
    >
      <el-table-column label="商品图片" width="90">
        <template #default="{ row }">
          <el-image
            :src="row.mainImage"
            fit="cover"
            style="width: 56px; height: 56px; border-radius: 4px"
            :preview-src-list="[row.mainImage]"
            v-if="row.mainImage"
          />
          <div v-else class="img-placeholder">暂无</div>
        </template>
      </el-table-column>
      <el-table-column label="商品名称" min-width="200">
        <template #default="{ row }">
          <span class="product-name">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="商家" width="160">
        <template #default="{ row }">
          {{ shopCache[row.shopId]?.shopName || '加载中...' }}
        </template>
      </el-table-column>
      <el-table-column label="价格" width="130">
        <template #default="{ row }">
          <PriceDisplay v-if="row._price" :price="row._price" />
          <span v-else class="no-price">--</span>
        </template>
      </el-table-column>
      <el-table-column label="类目" width="120">
        <template #default="{ row }">
          {{ categoryMap[row.categoryId] || row.categoryId }}
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="175">
        <template #default="{ row }">
          {{ row.createdAt }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="auditTagType(row.auditStatus)" effect="plain">
            {{ auditStatusText(row.auditStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openDetail(row)">
            {{ row.auditStatus === 0 ? '审核' : '查看' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      title="商品审核"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-body">
        <div v-if="productImages.length" class="image-gallery">
          <el-image
            v-for="(img, idx) in productImages"
            :key="idx"
            :src="img"
            fit="cover"
            style="width: 100px; height: 100px; border-radius: 6px; cursor: pointer; flex-shrink: 0"
            :preview-src-list="productImages"
            :initial-index="idx"
          />
        </div>

        <el-descriptions :column="2" border class="detail-info">
          <el-descriptions-item label="商品名称" :span="2">
            {{ currentProduct?.name }}
          </el-descriptions-item>
          <el-descriptions-item label="副标题" :span="2">
            {{ currentProduct?.subtitle || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="所属商家">
            {{ shopInfo?.shopName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="商品类目">
            {{ categoryMap[currentProduct?.categoryId] || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="商品描述" :span="2">
            <div class="description-text">{{ currentProduct?.description || '-' }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">规格与价格</h4>
        <el-table :data="currentSkus" border size="small" max-height="220" empty-text="暂无规格数据">
          <el-table-column prop="specName" label="规格名称" min-width="140" />
          <el-table-column label="销售价" width="140">
            <template #default="{ row }">
              <PriceDisplay :price="row.price" />
            </template>
          </el-table-column>
          <el-table-column label="市场价" width="140">
            <template #default="{ row }">
              <PriceDisplay v-if="row.marketPrice" :price="row.marketPrice" />
              <span v-else class="no-price">-</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="currentProduct?.auditStatus === 0" class="audit-section">
          <h4 class="section-title">审核操作</h4>
          <el-input
            v-model="rejectReason"
            type="textarea"
            placeholder="驳回原因（可选）"
            :rows="3"
            maxlength="200"
            show-word-limit
          />
          <div class="audit-buttons">
            <el-button type="success" :loading="auditing" size="large" @click="handleApprove">
              审核通过
            </el-button>
            <el-button type="danger" :loading="auditing" size="large" @click="handleReject">
              驳回
            </el-button>
          </div>
        </div>

        <div v-else class="audit-result">
          <el-tag
            :type="currentProduct?.auditStatus === 1 ? 'success' : 'danger'"
            size="large"
            effect="dark"
          >
            {{ auditStatusText(currentProduct?.auditStatus) }}
          </el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { PriceDisplay } from '@supermarket/ui'
import {
  getProductList,
  getProductDetail,
  getProductSkus,
  auditProduct,
  getCategoryTree,
  getShopById,
} from '@supermarket/api'

interface SpuItem {
  id: number
  spuNo: string
  shopId: number
  categoryId: number
  brandId: number
  name: string
  subtitle: string
  mainImage: string
  images: string
  description: string
  auditStatus: number
  shelfStatus: number
  isDeleted: number
  createdAt: string
  updatedAt: string
  _price?: number
}

interface SkuItem {
  id: number
  skuNo: string
  spuId: number
  specName: string
  specCode: string
  price: number
  marketPrice: number
  costPrice: number
  image: string
  weight: number
  status: number
}

interface CategoryItem {
  id: number
  name: string
  children?: CategoryItem[]
}

interface ShopItem {
  id: number
  merchantId: number
  shopName: string
  shopLogo: string
  shopDesc: string
  status: number
}

const activeTab = ref('0')
const productList = ref<SpuItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const categoryId = ref<number | undefined>()
const loading = ref(false)

const categories = ref<CategoryItem[]>([])
const categoryMap = ref<Record<number, string>>({})
const shopCache = ref<Record<number, ShopItem>>({})

const dialogVisible = ref(false)
const detailLoading = ref(false)
const currentProduct = ref<SpuItem | null>(null)
const currentSkus = ref<SkuItem[]>([])
const shopInfo = ref<ShopItem | null>(null)
const rejectReason = ref('')
const auditing = ref(false)

const flatCategories = computed(() => {
  const result: { id: number; name: string }[] = []
  function walk(list: CategoryItem[]) {
    for (const item of list) {
      result.push({ id: item.id, name: item.name })
      if (item.children?.length) {
        walk(item.children)
      }
    }
  }
  walk(categories.value)
  return result
})

const productImages = computed(() => {
  if (!currentProduct.value) return []
  const imgs: string[] = []
  if (currentProduct.value.mainImage) {
    imgs.push(currentProduct.value.mainImage)
  }
  if (currentProduct.value.images) {
    const parts = currentProduct.value.images.split(',').filter(Boolean)
    imgs.push(...parts)
  }
  return imgs
})

function auditStatusText(status: number): string {
  const map: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已驳回' }
  return map[status] || '未知'
}

function auditTagType(status: number): string {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

async function loadCategories() {
  try {
    const data = await getCategoryTree()
    categories.value = (data as CategoryItem[]) || []
    const map: Record<number, string> = {}
    function walk(list: CategoryItem[]) {
      for (const item of list) {
        map[item.id] = item.name
        if (item.children?.length) {
          walk(item.children)
        }
      }
    }
    walk(categories.value)
    categoryMap.value = map
  } catch {
    // categories remain empty
  }
}

async function loadShopInfo(shopId: number) {
  if (shopCache.value[shopId]) return
  try {
    const data = await getShopById(shopId)
    shopCache.value = { ...shopCache.value, [shopId]: data as ShopItem }
  } catch {
    shopCache.value = { ...shopCache.value, [shopId]: { shopName: `商家${shopId}` } as ShopItem }
  }
}

async function loadSkusForProducts(list: SpuItem[]) {
  await Promise.allSettled(
    list.map(product =>
      getProductSkus(product.id).then((skus) => {
        const items = skus as SkuItem[]
        if (items.length) {
          product._price = Math.min(...items.map(s => Number(s.price)))
        }
      })
    )
  )
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: pageSize.value,
      auditStatus: Number(activeTab.value),
    }
    if (keyword.value) params.keyword = keyword.value
    if (categoryId.value) params.categoryId = categoryId.value

    const res = await getProductList(params) as { records: SpuItem[]; total: number }
    const list = res.records || []
    productList.value = list
    total.value = res.total || 0

    const shopIds = [...new Set(list.map(p => p.shopId))]
    await Promise.all([
      ...shopIds.map(id => loadShopInfo(id)),
      loadSkusForProducts(list),
    ])
  } catch {
    productList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  page.value = 1
  fetchData()
}

function handleSearch() {
  page.value = 1
  fetchData()
}

async function openDetail(row: SpuItem) {
  currentProduct.value = row
  dialogVisible.value = true
  rejectReason.value = ''
  detailLoading.value = true
  currentSkus.value = []
  shopInfo.value = null

  try {
    const [detail, skus] = await Promise.all([
      getProductDetail(row.id),
      getProductSkus(row.id),
    ])
    currentProduct.value = detail as SpuItem
    currentSkus.value = (skus as SkuItem[]) || []
    await loadShopInfo(row.shopId)
    shopInfo.value = shopCache.value[row.shopId] || null
  } catch {
    // handled by interceptor
  } finally {
    detailLoading.value = false
  }
}

async function handleApprove() {
  if (!currentProduct.value) return
  auditing.value = true
  try {
    await auditProduct(currentProduct.value.id, 1)
    ElMessage.success('商品审核已通过')
    dialogVisible.value = false
    fetchData()
  } catch {
    // handled by interceptor
  } finally {
    auditing.value = false
  }
}

async function handleReject() {
  if (!currentProduct.value) return
  auditing.value = true
  try {
    await auditProduct(currentProduct.value.id, 2, rejectReason.value || undefined)
    ElMessage.success('商品已被驳回')
    dialogVisible.value = false
    fetchData()
  } catch {
    // handled by interceptor
  } finally {
    auditing.value = false
  }
}

onMounted(() => {
  loadCategories()
  fetchData()
})
</script>

<style scoped>
.audit-page {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #303133;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.filter-bar .el-input {
  width: 280px;
}

.filter-bar .el-select {
  width: 200px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.product-name {
  color: #303133;
  font-weight: 500;
}

.img-placeholder {
  width: 56px;
  height: 56px;
  background: #f5f5f5;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #999;
}

.no-price {
  color: #999;
  font-size: 12px;
}

.detail-body {
  min-height: 200px;
}

.image-gallery {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.detail-info {
  margin-bottom: 20px;
}

.description-text {
  white-space: pre-wrap;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 16px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.audit-section {
  margin-top: 20px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}

.audit-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 12px;
}

.audit-result {
  text-align: center;
  padding: 24px;
}
</style>
