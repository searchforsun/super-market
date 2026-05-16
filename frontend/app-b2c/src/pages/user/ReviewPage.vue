<template>
  <div class="review-page page-enter">
    <div class="page-header">
      <h2 class="page-title">我的评价</h2>
      <el-button type="primary" @click="openWriteDialog" v-if="!dialogVisible">
        <el-icon><Edit /></el-icon> 写评价
      </el-button>
    </div>

    <!-- Review List -->
    <div v-loading="loading">
      <div v-if="reviews.length" class="review-list">
        <div
          v-for="r in reviews"
          :key="r.id"
          class="review-card"
        >
          <div class="review-header">
            <div class="review-product" v-if="r.spuName || r.productName" @click="$router.push(`/product/${r.spuId || r.productId}`)">
              <el-image
                v-if="r.skuImage || r.productImage"
                :src="r.skuImage || r.productImage"
                fit="cover"
                class="review-product-img"
                lazy
              >
                <template #error><div class="review-product-img-fb"></div></template>
              </el-image>
              <span class="review-product-name">{{ r.spuName || r.productName }}</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
            <span class="review-date">{{ r.createdAt }}</span>
          </div>
          <div class="review-rating">
            <RatingStars :rating="r.rating" />
          </div>
          <p class="review-content" v-if="r.content">{{ r.content }}</p>
          <div class="review-images" v-if="r.images && r.images.length">
            <el-image
              v-for="(img, idx) in getImageArray(r.images)"
              :key="idx"
              :src="img"
              fit="cover"
              class="review-image"
              :preview-src-list="getImageArray(r.images)"
              :initial-index="idx"
              lazy
            />
          </div>
          <div class="review-actions">
            <el-button size="small" text type="primary" @click="openEditDialog(r)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
          </div>
        </div>

        <!-- Pagination -->
        <div class="pagination" v-if="total > pageSize">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="changePage"
          />
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!loading" class="empty-state">
        <el-empty description="暂无评价" :image-size="100">
          <el-button type="primary" @click="$router.push('/user/orders')">去评价商品</el-button>
        </el-empty>
      </div>
    </div>

    <!-- Write / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingReview ? '编辑评价' : '写评价'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="评分">
          <div class="star-rating-input">
            <span
              v-for="i in 5"
              :key="i"
              class="star-input-item"
              :class="{ active: i <= form.rating }"
              @click="form.rating = i"
            >
              <el-icon :size="28">
                <StarFilled v-if="i <= form.rating" />
                <Star v-else />
              </el-icon>
            </span>
          </div>
        </el-form-item>

        <el-form-item label="评价内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="分享您的使用体验..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="晒图（可选）">
          <FileUploader
            :max-count="6"
            :current-files="form.images"
            bucket="review"
            :uploader-id="userStore.userId"
            @update:files="form.images = $event"
          />
        </el-form-item>

        <el-form-item v-if="!editingReview" label="评价商品">
          <el-select
            v-model="form.spuId"
            placeholder="选择已购买的商品"
            filterable
            style="width: 100%"
            :loading="purchasedLoading"
          >
            <el-option
              v-for="item in purchasedProducts"
              :key="item.spuId || item.productId"
              :label="item.spuName || item.productName"
              :value="item.spuId || item.productId"
            />
          </el-select>
          <span class="form-hint" v-if="purchasedProducts.length === 0 && !purchasedLoading">
            暂无待评价商品
          </span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingReview ? '保存' : '提交评价' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@supermarket/stores'
import { getReviewsByUser, createReview } from '@supermarket/api'
import { RatingStars, FileUploader } from '@supermarket/ui'
import { ElMessage } from 'element-plus'
import { Edit, ArrowRight, StarFilled, Star } from '@element-plus/icons-vue'

const userStore = useUserStore()

const reviews = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// Dialog
const dialogVisible = ref(false)
const editingReview = ref<any>(null)
const submitting = ref(false)
const form = ref({
  spuId: null as number | null,
  rating: 5,
  content: '',
  images: [] as string[],
})

// Purchased products (for new review)
const purchasedProducts = ref<any[]>([])
const purchasedLoading = ref(false)

function getImageArray(images: any): string[] {
  if (!images) return []
  if (Array.isArray(images)) return images
  if (typeof images === 'string') {
    try {
      const parsed = JSON.parse(images)
      return Array.isArray(parsed) ? parsed : [images]
    } catch {
      return [images]
    }
  }
  return []
}

async function fetchReviews() {
  loading.value = true
  try {
    const res: any = await getReviewsByUser(userStore.userId, currentPage.value, pageSize.value)
    if (res?.records) {
      reviews.value = res.records
      total.value = res.total ?? 0
    } else if (Array.isArray(res)) {
      reviews.value = res
      total.value = res.length
    } else {
      reviews.value = []
      total.value = 0
    }
  } catch {
    reviews.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function changePage(page: number) {
  currentPage.value = page
  await fetchReviews()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function resetForm() {
  form.value = {
    spuId: null,
    rating: 5,
    content: '',
    images: [],
  }
  editingReview.value = null
}

function openWriteDialog() {
  resetForm()
  // Load purchased products
  purchasedLoading.value = true
  // TODO: In full implementation, load user's purchased products from orders
  // For now, we allow any spuId to be entered
  purchasedLoading.value = false
  dialogVisible.value = true
}

function openEditDialog(review: any) {
  editingReview.value = review
  form.value = {
    spuId: review.spuId || review.productId || null,
    rating: review.rating || 5,
    content: review.content || '',
    images: getImageArray(review.images),
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }
  submitting.value = true
  try {
    if (editingReview.value) {
      // Update existing review - use create with review id for update
      await createReview({
        id: editingReview.value.id,
        userId: userStore.userId,
        spuId: form.value.spuId ?? editingReview.value.spuId,
        rating: form.value.rating,
        content: form.value.content,
        images: form.value.images,
      })
      ElMessage.success('评价已更新')
    } else {
      if (!form.value.spuId) {
        ElMessage.warning('请选择评价商品')
        return
      }
      await createReview({
        userId: userStore.userId,
        spuId: form.value.spuId,
        rating: form.value.rating,
        content: form.value.content,
        images: form.value.images,
      })
      ElMessage.success('评价已提交')
    }
    dialogVisible.value = false
    await fetchReviews()
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(fetchReviews)
</script>

<style scoped>
.review-page {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: #333;
}

/* Review List */
.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-card {
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  transition: all 0.2s;
}

.review-card:hover {
  border-color: #ddd;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.review-product {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #333;
  font-size: 13px;
  transition: color 0.2s;
}

.review-product:hover {
  color: #e1251b;
}

.review-product-img {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
}

.review-product-img-fb {
  width: 36px;
  height: 36px;
  background: #f5f5f5;
  border-radius: 4px;
}

.review-product-name {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-date {
  font-size: 12px;
  color: #ccc;
}

.review-rating {
  margin-bottom: 8px;
}

.review-content {
  font-size: 13px;
  color: #333;
  line-height: 1.6;
  margin: 0 0 8px;
}

.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.review-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
}

.review-actions {
  display: flex;
  justify-content: flex-end;
}

/* Star Rating Input */
.star-rating-input {
  display: flex;
  gap: 4px;
}

.star-input-item {
  cursor: pointer;
  color: #ddd;
  transition: color 0.2s, transform 0.2s;
}

.star-input-item:hover {
  transform: scale(1.15);
}

.star-input-item.active {
  color: #ff9800;
}

.form-hint {
  font-size: 12px;
  color: #bbb;
  display: block;
  margin-top: 4px;
}

/* Empty */
.empty-state {
  padding: 60px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 16px;
}
</style>
