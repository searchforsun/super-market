<template>
  <div class="seckill-container page-enter">
    <h2>秒杀活动管理</h2>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="秒杀场次" name="sessions">
        <div class="toolbar">
          <el-button type="primary" @click="openCreateSession">新建场次</el-button>
        </div>

        <el-table :data="sessions" v-loading="sessionLoading" border stripe empty-text="暂无秒杀场次">
          <el-table-column prop="name" label="场次名称" min-width="180" />
          <el-table-column label="开始时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.startTime) }}
            </template>
          </el-table-column>
          <el-table-column label="结束时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="sessionStatus(row).type" effect="plain" size="default">
                {{ sessionStatus(row).text }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click="openEditSession(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="handleDeleteSession(row)">删除</el-button>
              <el-button size="small" type="success" plain @click="handleManageProducts(row)">管理商品</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog
          v-model="sessionDialogVisible"
          :title="sessionDialogMode === 'create' ? '新建场次' : '编辑场次'"
          width="520px"
          :close-on-click-modal="false"
          destroy-on-close
        >
          <el-form ref="sessionFormRef" :model="sessionForm" :rules="sessionRules" label-width="100px">
            <el-form-item label="场次名称" prop="name">
              <el-input v-model="sessionForm.name" placeholder="请输入场次名称" maxlength="50" />
            </el-form-item>
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="sessionForm.startTime"
                type="datetime"
                placeholder="选择开始时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="sessionForm.endTime"
                type="datetime"
                placeholder="选择结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="sessionDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="sessionSubmitLoading" @click="submitSession">确认</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="秒杀商品" name="products">
        <div class="toolbar">
          <div class="session-picker">
            <span class="picker-label">选择场次：</span>
            <el-select v-model="selectedSessionId" placeholder="请选择秒杀场次" style="width: 300px" @change="onSessionChange">
              <el-option v-for="s in sessions" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </div>
          <el-button type="primary" :disabled="!selectedSessionId" @click="openAddProduct">添加商品</el-button>
        </div>

        <el-table :data="products" v-loading="productLoading" border stripe empty-text="暂无秒杀商品">
          <el-table-column label="商品图片" width="80" align="center">
            <template #default="{ row }">
              <el-image
                :src="row.productImage || row.mainImage || row.image || ''"
                style="width: 50px; height: 50px"
                fit="cover"
              >
                <template #error>
                  <div class="img-placeholder">暂无</div>
                </template>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="名称" min-width="180" />
          <el-table-column label="原价" width="100" align="right">
            <template #default="{ row }">
              ¥{{ (row.originalPrice || row.price || 0).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="秒杀价" width="110" align="right">
            <template #default="{ row }">
              <span class="seckill-price">¥{{ (row.seckillPrice || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="stock" label="秒杀库存" width="100" align="center" />
          <el-table-column prop="sold" label="已售" width="80" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.stock > 0 ? 'success' : 'danger'" effect="plain" size="small">
                {{ row.stock > 0 ? '上架中' : '已售罄' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template #default="{ row }">
              <el-button size="small" type="danger" plain @click="handleRemoveProduct(row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog
          v-model="addProductDialogVisible"
          title="添加秒杀商品"
          width="520px"
          :close-on-click-modal="false"
          destroy-on-close
        >
          <el-form ref="addProductFormRef" :model="addProductForm" :rules="addProductRules" label-width="100px">
            <el-form-item label="选择商品" prop="productId">
              <el-select
                v-model="addProductForm.productId"
                filterable
                remote
                :remote-method="searchProducts"
                :loading="searchLoading"
                placeholder="搜索商品名称"
                style="width: 100%"
              >
                <el-option v-for="item in searchResults" :key="item.id" :label="item.name" :value="item.id">
                  <div class="product-option">
                    <span>{{ item.name }}</span>
                    <span class="option-price">¥{{ (item.price || 0).toFixed(2) }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="秒杀价" prop="seckillPrice">
              <el-input-number
                v-model="addProductForm.seckillPrice"
                :min="0.01"
                :precision="2"
                :step="0.1"
                :controls="true"
                style="width: 100%"
                placeholder="请输入秒杀价"
              />
            </el-form-item>
            <el-form-item label="秒杀库存" prop="stock">
              <el-input-number
                v-model="addProductForm.stock"
                :min="1"
                :step="1"
                :precision="0"
                :controls="true"
                style="width: 100%"
                placeholder="请输入秒杀库存"
              />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="addProductDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="addProductSubmitLoading" @click="submitAddProduct">确认</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSeckillSessions,
  getSeckillProducts,
  createSeckillSession,
  updateSeckillSession,
  deleteSeckillSession,
  addSeckillProduct,
  removeSeckillProduct,
  getProductList,
} from '@supermarket/api'
import { formatDate } from '@supermarket/utils'

interface Session {
  id: number
  name: string
  startTime: string
  endTime: string
}

interface SeckillProduct {
  id: number
  sessionId: number
  productId: number
  productName: string
  productImage?: string
  mainImage?: string
  image?: string
  originalPrice?: number
  price?: number
  seckillPrice: number
  stock: number
  sold: number
}

interface ProductSearchItem {
  id: number
  name: string
  image?: string
  price: number
}

const activeTab = ref('sessions')
const sessionLoading = ref(false)
const sessions = ref<Session[]>([])
const productLoading = ref(false)
const products = ref<SeckillProduct[]>([])
const selectedSessionId = ref<number | null>(null)

const sessionDialogVisible = ref(false)
const sessionDialogMode = ref<'create' | 'edit'>('create')
const sessionSubmitLoading = ref(false)
const sessionFormRef = ref<FormInstance>()
const sessionForm = reactive({
  id: 0,
  name: '',
  startTime: '',
  endTime: '',
})
const sessionRules = {
  name: [{ required: true, message: '请输入场次名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

const addProductDialogVisible = ref(false)
const addProductSubmitLoading = ref(false)
const addProductFormRef = ref<FormInstance>()
const searchLoading = ref(false)
const searchResults = ref<ProductSearchItem[]>([])
const addProductForm = reactive({
  productId: null as number | null,
  seckillPrice: null as number | null,
  stock: null as number | null,
})
const addProductRules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  seckillPrice: [{ required: true, message: '请输入秒杀价', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入秒杀库存', trigger: 'blur' }],
}

function sessionStatus(row: Session) {
  const now = Date.now()
  const start = new Date(row.startTime).getTime()
  const end = new Date(row.endTime).getTime()
  if (now < start) return { text: '即将开始', type: 'info' as const }
  if (now > end) return { text: '已结束', type: 'danger' as const }
  return { text: '进行中', type: 'success' as const }
}

async function loadSessions() {
  sessionLoading.value = true
  try {
    const data = await getSeckillSessions()
    if (Array.isArray(data)) {
      sessions.value = data
    } else if (data && data.records) {
      sessions.value = data.records
    } else if (data && data.list) {
      sessions.value = data.list
    } else {
      sessions.value = []
    }
  } catch {
    sessions.value = []
  } finally {
    sessionLoading.value = false
  }
}

async function loadProducts() {
  if (!selectedSessionId.value) {
    products.value = []
    return
  }
  productLoading.value = true
  try {
    const data = await getSeckillProducts(selectedSessionId.value)
    if (Array.isArray(data)) {
      products.value = data
    } else if (data && data.records) {
      products.value = data.records
    } else if (data && data.list) {
      products.value = data.list
    } else {
      products.value = []
    }
  } catch {
    products.value = []
  } finally {
    productLoading.value = false
  }
}

function openCreateSession() {
  sessionDialogMode.value = 'create'
  sessionForm.id = 0
  sessionForm.name = ''
  sessionForm.startTime = ''
  sessionForm.endTime = ''
  sessionDialogVisible.value = true
}

function openEditSession(row: Session) {
  sessionDialogMode.value = 'edit'
  sessionForm.id = row.id
  sessionForm.name = row.name
  sessionForm.startTime = row.startTime
  sessionForm.endTime = row.endTime
  sessionDialogVisible.value = true
}

async function submitSession() {
  const valid = await sessionFormRef.value?.validate().catch(() => false)
  if (!valid) return
  sessionSubmitLoading.value = true
  try {
    if (sessionDialogMode.value === 'create') {
      await createSeckillSession({
        name: sessionForm.name,
        startTime: sessionForm.startTime,
        endTime: sessionForm.endTime,
      })
      ElMessage.success('场次创建成功')
    } else {
      await updateSeckillSession(sessionForm.id, {
        name: sessionForm.name,
        startTime: sessionForm.startTime,
        endTime: sessionForm.endTime,
      })
      ElMessage.success('场次更新成功')
    }
    sessionDialogVisible.value = false
    await loadSessions()
  } finally {
    sessionSubmitLoading.value = false
  }
}

async function handleDeleteSession(row: Session) {
  try {
    await ElMessageBox.confirm(`确定要删除场次「${row.name}」吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
    await deleteSeckillSession(row.id)
    ElMessage.success('场次已删除')
    await loadSessions()
  } catch {
    /* cancelled by user or error handled by interceptor */
  }
}

function handleManageProducts(row: Session) {
  selectedSessionId.value = row.id
  activeTab.value = 'products'
  loadProducts()
}

function onSessionChange() {
  loadProducts()
}

function openAddProduct() {
  addProductForm.productId = null
  addProductForm.seckillPrice = null
  addProductForm.stock = null
  searchResults.value = []
  addProductDialogVisible.value = true
}

async function searchProducts(keyword: string) {
  if (!keyword || keyword.trim().length === 0) {
    searchResults.value = []
    return
  }
  searchLoading.value = true
  try {
    const data = await getProductList({ keyword: keyword.trim(), page: 1, size: 20 })
    if (Array.isArray(data)) {
      searchResults.value = data
    } else if (data && data.records) {
      searchResults.value = data.records
    } else if (data && data.list) {
      searchResults.value = data.list
    } else {
      searchResults.value = []
    }
  } catch {
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

async function submitAddProduct() {
  const valid = await addProductFormRef.value?.validate().catch(() => false)
  if (!valid) return
  addProductSubmitLoading.value = true
  try {
    await addSeckillProduct(selectedSessionId.value!, {
      productId: addProductForm.productId!,
      seckillPrice: addProductForm.seckillPrice!,
      stock: addProductForm.stock!,
    })
    ElMessage.success('商品添加成功')
    addProductDialogVisible.value = false
    await loadProducts()
  } finally {
    addProductSubmitLoading.value = false
  }
}

async function handleRemoveProduct(row: SeckillProduct) {
  try {
    await ElMessageBox.confirm('确定要从场次中移除该商品吗？', '确认移除', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
    await removeSeckillProduct(row.id)
    ElMessage.success('商品已移除')
    await loadProducts()
  } catch {
    /* cancelled by user or error handled by interceptor */
  }
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.seckill-container {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
}

.seckill-container h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 20px;
  color: #303133;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.session-picker {
  display: flex;
  align-items: center;
}

.picker-label {
  white-space: nowrap;
  font-size: 14px;
  color: #606266;
  margin-right: 8px;
}

.seckill-price {
  color: #f56c6c;
  font-weight: 600;
}

.product-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.option-price {
  color: #909399;
  font-size: 12px;
}

.img-placeholder {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 12px;
}
</style>