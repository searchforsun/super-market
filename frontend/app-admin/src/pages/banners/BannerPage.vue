<template>
  <div class="banner-page">
    <el-card>
      <div class="page-header">
        <h2>Banner/广告位管理</h2>
        <el-button type="primary" @click="handleAdd">新建Banner</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="预览" width="140">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" style="width: 100px; height: 56px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column label="位置" width="130">
          <template #default="{ row }">
            <el-tag :type="positionTagType(row.position)" size="small">
              {{ positionLabel(row.position) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">
            {{ row.startTime ? formatTime(row.startTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="170">
          <template #default="{ row }">
            {{ row.endTime ? formatTime(row.endTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="statusSwitching"
              @change="(val: boolean) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑Banner' : '新建Banner'"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入Banner标题" maxlength="100" />
        </el-form-item>

        <el-form-item label="Banner图片" prop="imageUrl">
          <FileUploader @success="handleUploadSuccess" />
          <div v-if="form.imageUrl" class="upload-preview">
            <el-image :src="form.imageUrl" style="width: 200px; height: 112px; margin-top: 8px" fit="cover" />
          </div>
        </el-form-item>

        <el-form-item label="链接地址" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="点击跳转链接，可选" />
        </el-form-item>

        <el-form-item label="展示位置" prop="position">
          <el-select v-model="form.position" placeholder="请选择展示位置" style="width: 100%">
            <el-option label="首页顶部" value="HOME_TOP" />
            <el-option label="首页中部" value="HOME_MID" />
            <el-option label="侧边栏" value="SIDEBAR" />
          </el-select>
        </el-form-item>

        <el-form-item label="排序值" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>

        <el-form-item label="有效时段" required>
          <div class="date-range">
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </div>
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="form.statusBoolean" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { listBanners, createBanner, updateBanner, deleteBanner } from '@supermarket/api'
import FileUploader from '@supermarket/ui/FileUploader.vue'

interface BannerRecord {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  sortOrder: number
  position: string
  status: number
  startTime: string
  endTime: string
  createdAt: string
  updatedAt: string
}

const loading = ref(false)
const statusSwitching = ref(false)
const tableData = ref<BannerRecord[]>([])
const total = ref(0)
const queryParams = reactive({ page: 1, size: 20 })

const dialogVisible = ref(false)
const isEditing = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const dateRange = ref<[string, string] | null>(null)

const form = reactive({
  title: '',
  imageUrl: '',
  linkUrl: '',
  position: 'HOME_TOP',
  sortOrder: 0,
  startTime: '',
  endTime: '',
  statusBoolean: true,
})

const formRules: FormRules = {
  title: [{ required: true, message: '请输入Banner标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请上传Banner图片', trigger: 'change' }],
  position: [{ required: true, message: '请选择展示位置', trigger: 'change' }],
}

function positionLabel(position: string): string {
  const map: Record<string, string> = { HOME_TOP: '首页顶部', HOME_MID: '首页中部', SIDEBAR: '侧边栏' }
  return map[position] || position
}

function positionTagType(position: string): 'success' | 'warning' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'info'> = { HOME_TOP: 'success', HOME_MID: 'warning', SIDEBAR: 'info' }
  return map[position] || 'info'
}

function formatTime(t: string): string {
  if (!t) return '-'
  return t.slice(0, 19).replace('T', ' ')
}

function resetForm() {
  form.title = ''
  form.imageUrl = ''
  form.linkUrl = ''
  form.position = 'HOME_TOP'
  form.sortOrder = 0
  form.startTime = ''
  form.endTime = ''
  form.statusBoolean = true
  dateRange.value = null
  editingId.value = null
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await listBanners(queryParams.page, queryParams.size)
    tableData.value = res.records || []
    total.value = res.total || 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEditing.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: BannerRecord) {
  isEditing.value = true
  editingId.value = row.id
  form.title = row.title
  form.imageUrl = row.imageUrl
  form.linkUrl = row.linkUrl
  form.position = row.position
  form.sortOrder = row.sortOrder
  form.statusBoolean = row.status === 1
  form.startTime = row.startTime || ''
  form.endTime = row.endTime || ''
  if (row.startTime && row.endTime) {
    dateRange.value = [row.startTime, row.endTime]
  } else {
    dateRange.value = null
  }
  dialogVisible.value = true
}

async function handleDelete(row: BannerRecord) {
  try {
    await ElMessageBox.confirm(`确定要删除Banner「${row.title}」吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteBanner(row.id)
    ElMessage.success('删除成功')
    await fetchData()
  } catch {
    // cancelled
  }
}

async function handleStatusChange(row: BannerRecord, val: boolean) {
  statusSwitching.value = true
  try {
    await updateBanner(row.id, { ...row, status: val ? 1 : 0 })
    row.status = val ? 1 : 0
    ElMessage.success(val ? '已启用' : '已停用')
  } catch {
    // revert handled by re-fetch
  } finally {
    statusSwitching.value = false
  }
}

function handleUploadSuccess(url: string) {
  form.imageUrl = url
}

watch(dateRange, (val) => {
  if (val) {
    form.startTime = val[0]
    form.endTime = val[1]
  } else {
    form.startTime = ''
    form.endTime = ''
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      title: form.title,
      imageUrl: form.imageUrl,
      linkUrl: form.linkUrl,
      position: form.position,
      sortOrder: form.sortOrder,
      status: form.statusBoolean ? 1 : 0,
      startTime: form.startTime,
      endTime: form.endTime,
    }

    if (isEditing.value && editingId.value) {
      await updateBanner(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createBanner(payload)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    await fetchData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.banner-page {
  padding: 20px;
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
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.upload-preview {
  display: flex;
}

.date-range {
  width: 100%;
}
</style>
