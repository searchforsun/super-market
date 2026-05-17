<template>
  <div class="category-page page-enter">
    <h2 class="page-title">类目管理</h2>
    <div class="category-layout">
      <div class="tree-panel">
        <el-card shadow="never">
          <template #header>
            <div class="tree-header">
              <span>分类目录</span>
              <el-button text size="small" @click="refreshTree">刷新</el-button>
            </div>
          </template>
          <div class="tree-scroll">
            <el-tree
              ref="treeRef"
              :key="treeKey"
              node-key="id"
              :props="treeProps"
              :highlight-current="true"
              :expand-on-click-node="false"
              lazy
              :load="loadTreeNode"
              @node-click="handleNodeClick"
              empty-text="暂无类目"
            />
          </div>
        </el-card>
      </div>
      <div class="detail-panel">
        <div class="toolbar">
          <el-button type="primary" @click="openCreateDialog(null)">添加一级类目</el-button>
          <el-button :disabled="!selectedNode" @click="openCreateDialog(selectedNode)">添加子类目</el-button>
        </div>
        <div v-if="selectedNode" class="selected-info">
          <span class="selected-label">当前类目：</span>
          <el-tag closable @close="clearSelection">{{ selectedNode.name }}</el-tag>
          <span class="selected-hint">下方显示其子类目列表</span>
        </div>
        <el-table
          :data="tableData"
          v-loading="tableLoading"
          border
          stripe
          style="width:100%"
          empty-text="暂无子类目"
        >
          <el-table-column prop="name" label="类目名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="层级" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" :type="levelTagType(row.level)">
                第{{ row.level }}级
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
          <el-table-column label="子类目" width="90" align="center">
            <template #default="{ row }">
              <span>{{ row.subCount !== undefined ? row.subCount : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                @change="(val) => handleToggleStatus(row, val)"
                active-text="启用"
                inactive-text="停用"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
              <el-button size="small" type="primary" plain @click="viewChildren(row)">查看子类目</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      :close-on-click-modal="false"
      :before-close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" label-position="right">
        <el-form-item label="父级类目">
          <el-input :model-value="form.parentName" disabled>
            <template #prefix>
              <span style="color:#909399">PID: {{ form.parentId }}</span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="类目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入类目名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序序号">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width:200px" />
          <span class="form-hint">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="图标URL">
          <el-input v-model="form.iconUrl" placeholder="可选，输入图标的图片地址" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.statusBool" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryTree, getCategoryChildren, request } from '@supermarket/api'

const treeRef = ref<any>(null)
const treeKey = ref(0)

const treeProps = {
  children: 'children',
  label: 'name',
}

const selectedNode = ref<any>(null)
const tableLoading = ref(false)
const tableData = ref<any[]>([])

const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<any>(null)

function defaultForm() {
  return {
    parentId: 0,
    parentName: '顶级类目',
    name: '',
    sortOrder: 0,
    iconUrl: '',
    statusBool: true,
  }
}

const form = reactive(defaultForm())

const formRules = {
  name: [{ required: true, message: '请输入类目名称', trigger: 'blur' }],
}

const dialogTitle = computed(() => (isEditing.value ? '编辑类目' : '添加类目'))

function levelTagType(level: number): 'info' | 'success' | 'warning' | 'primary' {
  if (level <= 1) return 'info'
  if (level === 2) return 'success'
  if (level === 3) return 'warning'
  return 'primary'
}

async function loadTreeNode(node: any, resolve: (data: any[]) => void) {
  try {
    let data: any[]
    if (node.level === 0) {
      data = await getCategoryTree()
    } else {
      data = await getCategoryChildren(node.data.id)
    }
    resolve(Array.isArray(data) ? data : [])
  } catch {
    resolve([])
  }
}

function handleNodeClick(data: any) {
  selectedNode.value = data
  if (treeRef.value) {
    treeRef.value.setCurrentKey(data.id)
  }
  loadTableData(data.id)
}

async function loadTableData(parentId: number) {
  tableLoading.value = true
  try {
    const data = await getCategoryChildren(parentId)
    const items = Array.isArray(data) ? data : []
    tableData.value = items
    const countPromises = items.map(async (item: any) => {
      try {
        const sub = await getCategoryChildren(item.id)
        item.subCount = Array.isArray(sub) ? sub.length : 0
      } catch {
        item.subCount = 0
      }
    })
    await Promise.all(countPromises)
  } catch {
    tableData.value = []
  } finally {
    tableLoading.value = false
  }
}

function openCreateDialog(parentNode: any) {
  isEditing.value = false
  editingId.value = null
  form.parentId = parentNode ? parentNode.id : 0
  form.parentName = parentNode ? parentNode.name : '顶级类目'
  form.name = ''
  form.sortOrder = 0
  form.iconUrl = ''
  form.statusBool = true
  dialogVisible.value = true
}

async function openEditDialog(row: any) {
  isEditing.value = true
  editingId.value = row.id
  form.parentId = row.parentId || 0
  if (row.parentId && row.parentId > 0) {
    form.parentName = row.parentName || `父级 ID: ${row.parentId}`
  } else {
    form.parentName = '顶级类目'
  }
  form.name = row.name
  form.sortOrder = row.sortOrder ?? 0
  form.iconUrl = row.iconUrl || ''
  form.statusBool = row.status === 1
  dialogVisible.value = true
}

function handleDialogClose() {
  dialogVisible.value = false
}

function clearSelection() {
  selectedNode.value = null
  if (treeRef.value) {
    treeRef.value.setCurrentKey(null)
  }
  loadRootTableData()
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: Record<string, any> = {
      parentId: form.parentId,
      name: form.name,
      sortOrder: form.sortOrder,
      iconUrl: form.iconUrl || undefined,
      status: form.statusBool ? 1 : 0,
    }
    if (isEditing.value && editingId.value) {
      payload.id = editingId.value
      await request.put('/category', payload)
      ElMessage.success('类目已更新')
    } else {
      await request.post('/category', payload)
      ElMessage.success('类目已创建')
    }
    dialogVisible.value = false
    treeKey.value++
    if (selectedNode.value) {
      await loadTableData(selectedNode.value.id)
    } else {
      await loadRootTableData()
    }
  } catch {
    /* error handled by interceptor */
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: any) {
  try {
    const children = await getCategoryChildren(row.id)
    const hasSubCategories = Array.isArray(children) && children.length > 0
    let message = `确定要删除类目「${row.name}」吗？此操作不可恢复。`
    if (hasSubCategories) {
      message = `类目「${row.name}」下存在 ${children.length} 个子类目，无法直接删除。请先删除所有子类目。`
    }
    await ElMessageBox.confirm(message, '确认删除', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await request.delete(`/category/${row.id}`)
    ElMessage.success('类目已删除')
    treeKey.value++
    if (selectedNode.value) {
      await loadTableData(selectedNode.value.id)
    } else {
      await loadRootTableData()
    }
  } catch {
    /* cancelled or backend error */
  }
}

async function handleToggleStatus(row: any, enabled: boolean) {
  const newStatus = enabled ? 1 : 0
  try {
    await request.put('/category', { id: row.id, status: newStatus })
    row.status = newStatus
    ElMessage.success(enabled ? '已启用' : '已停用')
  } catch {
    /* error handled by interceptor */
  }
}

function viewChildren(row: any) {
  selectedNode.value = row
  if (treeRef.value) {
    treeRef.value.setCurrentKey(row.id)
  }
  loadTableData(row.id)
}

async function loadRootTableData() {
  selectedNode.value = null
  tableLoading.value = true
  try {
    const data = await getCategoryTree()
    tableData.value = Array.isArray(data) ? data : []
  } catch {
    tableData.value = []
  } finally {
    tableLoading.value = false
  }
}

function refreshTree() {
  treeKey.value++
  selectedNode.value = null
  if (treeRef.value) {
    treeRef.value.setCurrentKey(null)
  }
  loadRootTableData()
}

onMounted(() => {
  loadRootTableData()
})
</script>

<style scoped>
.category-page {
  background: var(--color-surface, #fff);
  padding: 24px;
  border-radius: 8px;
  min-height: 500px;
}

.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary, #303133);
}

.category-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.tree-panel {
  width: 260px;
  flex-shrink: 0;
}

.tree-panel :deep(.el-card__header) {
  padding: 12px 16px;
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 500;
}

.tree-scroll {
  max-height: 560px;
  overflow-y: auto;
}

.tree-scroll :deep(.el-tree-node__content) {
  height: 32px;
}

.detail-panel {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.selected-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 8px 12px;
  background: var(--color-surface-hover, #f5f7fa);
  border-radius: 4px;
  font-size: 13px;
}

.selected-label {
  color: var(--color-text-secondary, #606266);
}

.selected-hint {
  color: var(--color-text-muted, #909399);
  font-size: 12px;
}

.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
