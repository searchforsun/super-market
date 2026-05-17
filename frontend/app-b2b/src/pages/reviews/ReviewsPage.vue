<template>
  <div class="review-page page-enter">
    <h2 class="page-title">评价管理</h2>

    <div class="filters">
      <div class="filter-item">
        <span class="filter-label">评分：</span>
        <el-radio-group v-model="ratingCategory" @change="onRatingChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="good">好评</el-radio-button>
          <el-radio-button value="medium">中评</el-radio-button>
          <el-radio-button value="bad">差评</el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-item">
        <el-input
          v-model="keyword"
          placeholder="搜索商品名称"
          clearable
          style="width:260px"
          @clear="onSearchClear"
          @keyup.enter="onSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" style="margin-left:8px" @click="onSearch">搜索</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" stripe border style="width:100%">
      <el-table-column label="商品信息" min-width="200">
        <template #default="{ row }">
          <div class="cell-product">
            <el-image
              v-if="getProduct(row.spuId)?.mainImage"
              :src="getProduct(row.spuId)!.mainImage"
              style="width:48px;height:48px;border-radius:4px;flex-shrink:0"
              fit="cover"
            />
            <div class="cell-product-placeholder" v-else />
            <span class="cell-product-name">{{ getProduct(row.spuId)?.name || '商品#' + row.spuId }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="用户" width="100">
        <template #default="{ row }">
          <span>用户{{ row.userId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="评分" width="100">
        <template #default="{ row }">
          <RatingStars :rating="row.rating" :size="16" />
        </template>
      </el-table-column>
      <el-table-column label="评价内容" min-width="240">
        <template #default="{ row }">
          <div class="cell-content">
            <p class="cell-content-text">{{ row.content }}</p>
            <div v-if="row.replyContent" class="cell-reply">
              <span class="cell-reply-label">商家回复：</span>
              <p class="cell-reply-text">{{ row.replyContent }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="图片" width="130">
        <template #default="{ row }">
          <div v-if="row.images" class="cell-images">
            <el-image
              v-for="(img, idx) in row.images.split(',')"
              :key="idx"
              :src="img"
              style="width:40px;height:40px;border-radius:4px;cursor:pointer"
              fit="cover"
              :preview-src-list="row.images.split(',')"
              preview-teleported
            />
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="170">
        <template #default="{ row }">
          {{ row.createdAt }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success" size="small" effect="plain">已发布</el-tag>
          <el-tag v-else type="warning" size="small" effect="plain">待审核</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openReply(row)">
            {{ row.replyContent ? '查看回复' : '回复' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadData"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog v-model="replyVisible" title="回复评价" width="580px" :close-on-click-modal="false" destroy-on-close>
      <div v-if="replyTarget" class="reply-dialog">
        <div class="reply-origin">
          <div class="reply-origin-header">
            <RatingStars :rating="replyTarget.rating" :size="14" />
            <span class="reply-origin-user">用户{{ replyTarget.userId }}</span>
            <span class="reply-origin-time">{{ replyTarget.createdAt }}</span>
          </div>
          <p class="reply-origin-text">{{ replyTarget.content }}</p>
        </div>
        <div v-if="replyTarget.replyContent" class="reply-existing">
          <div class="reply-existing-header">
            <el-icon><ChatDotSquare /></el-icon>
            <span>已有回复</span>
          </div>
          <p class="reply-existing-text">{{ replyTarget.replyContent }}</p>
        </div>
        <div class="reply-editor">
          <label class="reply-editor-label">{{ replyTarget.replyContent ? '修改回复' : '输入回复' }}</label>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="5"
            placeholder="请输入回复内容，最多500字"
            maxlength="500"
            show-word-limit
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, ChatDotSquare } from '@element-plus/icons-vue'
import { RatingStars } from '@supermarket/ui'
import {
  getReviewPage,
  replyReview,
  getProductDetail,
  searchProduct,
} from '@supermarket/api'

interface ReviewItem {
  id: number
  userId: number
  spuId: number
  skuId: number
  orderNo: string
  rating: number
  content: string
  images: string
  isAnonymous: number
  replyContent: string
  replyAt: string
  status: number
  createdAt: string
  updatedAt: string
}

interface SpuItem {
  id: number
  name: string
  mainImage: string
}

const loading = ref(false)
const list = ref<ReviewItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const ratingCategory = ref('')
const keyword = ref('')
const searchSpuId = ref<number | undefined>(undefined)
const productMap = ref<Record<number, SpuItem>>({})

const replyVisible = ref(false)
const replyTarget = ref<ReviewItem | null>(null)
const replyContent = ref('')
const submitting = ref(false)

function getProduct(spuId: number): SpuItem | undefined {
  return productMap.value[spuId]
}

function loadProducts(spuIds: number[]) {
  const missing = spuIds.filter((id) => !productMap.value[id])
  if (missing.length === 0) return
  Promise.all(
    missing.map((id) =>
      getProductDetail(id)
        .then((spu: any) => {
          productMap.value[id] = { id: spu.id, name: spu.name, mainImage: spu.mainImage }
        })
        .catch(() => {})
    )
  )
}

function loadData() {
  loading.value = true
  const params: Record<string, any> = {
    page: currentPage.value,
    size: pageSize.value,
  }
  if (ratingCategory.value === 'good') {
    params.minRating = 4
  } else if (ratingCategory.value === 'medium') {
    params.minRating = 3
    params.maxRating = 3
  } else if (ratingCategory.value === 'bad') {
    params.maxRating = 2
  }
  if (searchSpuId.value) {
    params.spuId = searchSpuId.value
  }
  getReviewPage(params)
    .then((res: any) => {
      list.value = res.records || []
      total.value = res.total || 0
      const spuIds = [...new Set(list.value.map((r: ReviewItem) => r.spuId))]
      loadProducts(spuIds)
    })
    .catch(() => {
      list.value = []
      total.value = 0
    })
    .finally(() => {
      loading.value = false
    })
}

function onRatingChange() {
  currentPage.value = 1
  loadData()
}

function onSizeChange() {
  currentPage.value = 1
  loadData()
}

function onSearch() {
  const val = keyword.value?.trim()
  if (!val) {
    searchSpuId.value = undefined
    currentPage.value = 1
    loadData()
    return
  }
  searchProduct({ keyword: val, page: 1, size: 10 })
    .then((res: any) => {
      const records = res.records || []
      if (records.length > 0) {
        searchSpuId.value = records[0].id
        records.forEach((spu: SpuItem) => {
          productMap.value[spu.id] = spu
        })
      } else {
        searchSpuId.value = -1 as unknown as number
        ElMessage.info('未找到匹配的商品')
      }
      currentPage.value = 1
      loadData()
    })
    .catch(() => {})
}

function onSearchClear() {
  searchSpuId.value = undefined
  currentPage.value = 1
  loadData()
}

function openReply(row: ReviewItem) {
  replyTarget.value = row
  replyContent.value = row.replyContent || ''
  replyVisible.value = true
}

function submitReply() {
  if (!replyTarget.value) return
  const trimmed = replyContent.value.trim()
  if (!trimmed) {
    ElMessage.warning('请输入回复内容')
    return
  }
  submitting.value = true
  replyReview(replyTarget.value.id, trimmed)
    .then(() => {
      ElMessage.success('回复成功')
      replyVisible.value = false
      const found = list.value.find((r) => r.id === replyTarget.value!.id)
      if (found) {
        found.replyContent = trimmed
      }
    })
    .catch(() => {})
    .finally(() => {
      submitting.value = false
    })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.review-page {
  background: var(--color-surface, #fff);
  padding: 24px;
  border-radius: 8px;
}
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary, #303133);
}
.filters {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.filter-item {
  display: flex;
  align-items: center;
}
.filter-label {
  font-size: 14px;
  color: #606266;
  margin-right: 8px;
  white-space: nowrap;
}
.cell-product {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cell-product-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  background: var(--color-surface-hover, #f5f7fa);
  flex-shrink: 0;
}
.cell-product-name {
  font-size: 13px;
  color: var(--color-text-primary, #303133);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.cell-content-text {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-primary, #303133);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}
.cell-reply {
  margin-top: 8px;
  padding: 8px 10px;
  background: #f0f9eb;
  border-radius: 4px;
  border-left: 3px solid #67c23a;
}
.cell-reply-label {
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}
.cell-reply-text {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}
.cell-images {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
.reply-dialog {
  padding: 0;
}
.reply-origin {
  padding: 16px;
  background: var(--color-surface-hover, #f5f7fa);
  border-radius: 6px;
  margin-bottom: 16px;
}
.reply-origin-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.reply-origin-user {
  font-size: 13px;
  color: var(--color-text-muted, #909399);
}
.reply-origin-time {
  font-size: 12px;
  color: var(--color-text-muted, #c0c4cc);
  margin-left: auto;
}
.reply-origin-text {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-primary, #303133);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
.reply-existing {
  padding: 16px;
  background: #f0f9eb;
  border-radius: 6px;
  margin-bottom: 16px;
  border-left: 3px solid #67c23a;
}
.reply-existing-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #67c23a;
  font-weight: 500;
  margin-bottom: 6px;
}
.reply-existing-text {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
.reply-editor-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary, #303133);
  margin-bottom: 8px;
}
</style>
