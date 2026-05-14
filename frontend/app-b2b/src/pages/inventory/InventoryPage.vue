<template>
  <div class="inventory-page">
    <div class="page-header">
      <h2>库存管理</h2>
    </div>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称或 SKU 编码"
        clearable
        style="width: 320px"
        @input="onKeywordInput"
        @clear="fetchData"
      />
    </div>

    <el-table
      :data="records"
      v-loading="loading"
      border
      stripe
      style="width: 100%"
      :row-class-name="tableRowClassName"
    >
      <el-table-column label="商品图片" width="96" align="center">
        <template #default="{ row }">
          <el-image
            :src="row.spuImage"
            style="width: 60px; height: 60px"
            fit="cover"
            :preview-src-list="[row.spuImage]"
            preview-teleported
          >
            <template #error>
              <div class="image-placeholder">暂无图片</div>
            </template>
          </el-image>
        </template>
      </el-table-column>

      <el-table-column prop="skuNo" label="SKU 编码" width="140" />

      <el-table-column prop="spuName" label="商品名称" min-width="180" show-overflow-tooltip />

      <el-table-column prop="specName" label="规格" width="120" />

      <el-table-column label="当前库存" width="120" align="center">
        <template #default="{ row }">
          <span :class="{ 'stock-low': row.availableStock <= row.safetyStock }">
            {{ row.availableStock }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="安全库存" width="90" align="center">
        <template #default="{ row }">
          {{ row.safetyStock }}
        </template>
      </el-table-column>

      <el-table-column label="价格" width="140">
        <template #default="{ row }">
          <PriceDisplay :price="row.price" :original-price="row.marketPrice" />
        </template>
      </el-table-column>

      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.availableStock > row.safetyStock" type="success" effect="plain" size="small">
            正常
          </el-tag>
          <el-tag v-else-if="row.availableStock > 0" type="warning" effect="plain" size="small">
            库存不足
          </el-tag>
          <el-tag v-else type="danger" effect="dark" size="small">
            缺货
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="锁定库存" width="90" align="center">
        <template #default="{ row }">
          {{ row.lockedStock }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openAdjustDialog(row)">
            调整库存
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="fetchData"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog v-model="adjustDialogVisible" title="调整库存" width="480px" :close-on-click-modal="false">
      <el-form v-if="currentSku" label-width="90px" label-position="left">
        <div class="sku-info-card">
          <el-image :src="currentSku.spuImage" style="width: 64px; height: 64px" fit="cover" />
          <div class="sku-info-text">
            <p class="sku-name">{{ currentSku.spuName }}</p>
            <p class="sku-spec">规格：{{ currentSku.specName }}</p>
            <p class="sku-code">SKU：{{ currentSku.skuNo }}</p>
          </div>
        </div>
        <el-form-item label="当前库存">
          <span class="current-stock">{{ currentSku.availableStock }}</span>
        </el-form-item>
        <el-form-item label="调整数量">
          <div class="adjust-input-wrapper">
            <el-input-number
              v-model="adjustQuantity"
              :min="-currentSku.availableStock"
              :max="99999"
              :precision="0"
              controls-position="right"
              style="width: 180px"
            />
            <span class="adjust-hint">正数增加库存，负数减少库存</span>
          </div>
        </el-form-item>
        <el-form-item label="调整后库存">
          <span :class="['after-stock', { 'stock-low': currentSku.availableStock + adjustQuantity <= currentSku.safetyStock }]">
            {{ currentSku.availableStock + adjustQuantity }}
          </span>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input
            v-model="adjustReason"
            type="textarea"
            :rows="3"
            placeholder="请输入调整原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" :disabled="!adjustReason.trim()" @click="confirmAdjust">
          确认调整
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { PriceDisplay } from '@supermarket/ui'
import { getInventoryList, updateStock } from '@supermarket/api'
import type { InventoryRecord } from '@supermarket/api'

const records = ref<InventoryRecord[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const keyword = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

const adjustDialogVisible = ref(false)
const currentSku = ref<InventoryRecord | null>(null)
const adjustQuantity = ref(0)
const adjustReason = ref('')
const adjusting = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const result = await getInventoryList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value,
    })
    records.value = result.records
    total.value = result.total
  } catch {
    ElMessage.error('获取库存列表失败')
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onKeywordInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    fetchData()
  }, 400)
}

function onSizeChange() {
  page.value = 1
  fetchData()
}

function tableRowClassName({ row }: { row: InventoryRecord }) {
  if (row.availableStock <= 0) return 'row-out-of-stock'
  if (row.availableStock <= row.safetyStock) return 'row-low-stock'
  return ''
}

function openAdjustDialog(row: InventoryRecord) {
  currentSku.value = row
  adjustQuantity.value = 0
  adjustReason.value = ''
  adjustDialogVisible.value = true
}

async function confirmAdjust() {
  if (!currentSku.value || adjustQuantity.value === 0) {
    ElMessage.warning('调整数量不能为 0')
    return
  }
  if (!adjustReason.value.trim()) {
    ElMessage.warning('请输入调整原因')
    return
  }

  adjusting.value = true
  try {
    const success = await updateStock(currentSku.value.skuId, adjustQuantity.value, adjustReason.value.trim())
    if (success) {
      ElMessage.success('库存调整成功')
      adjustDialogVisible.value = false
      await fetchData()
    } else {
      ElMessage.error('库存调整失败，请重试')
    }
  } catch {
    ElMessage.error('库存调整失败，请重试')
  } finally {
    adjusting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.inventory-page {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.stock-low {
  color: #e6a23c;
  font-weight: 700;
}

.image-placeholder {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 12px;
}

.sku-info-card {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 20px;
}

.sku-info-text {
  flex: 1;
  min-width: 0;
}

.sku-info-text .sku-name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.sku-info-text .sku-spec,
.sku-info-text .sku-code {
  margin: 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.8;
}

.current-stock {
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
}

.after-stock {
  font-size: 16px;
  font-weight: 700;
  color: #409eff;
}

.after-stock.stock-low {
  color: #e6a23c;
}

.adjust-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.adjust-hint {
  font-size: 12px;
  color: #909399;
}
</style>

<style>
.el-table .row-low-stock {
  background-color: #fdf6ec;
}

.el-table .row-out-of-stock {
  background-color: #fef0f0;
}

.el-table .row-low-stock:hover > td,
.el-table .row-out-of-stock:hover > td {
  background-color: transparent;
}
</style>
