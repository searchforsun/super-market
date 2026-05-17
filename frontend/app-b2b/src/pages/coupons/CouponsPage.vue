<template>
  <div class="coupon-page page-enter">
    <h2 class="page-title">优惠券管理</h2>
    <el-tabs v-model="activeTab" class="page-tabs">
      <el-tab-pane label="优惠券模板" name="templates">
        <div class="toolbar">
          <el-button type="primary" @click="openCreateDialog">新建模板</el-button>
        </div>
        <el-table :data="templates" v-loading="loading" border stripe>
          <el-table-column prop="name" label="模板名称" min-width="160" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 1 ? 'success' : 'warning'" effect="plain">
                {{ row.type === 1 ? '满减' : '折扣' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="面值" width="120" align="right">
            <template #default="{ row }">
              <span>{{ row.type === 1 ? '¥' : '' }}{{ row.discountValue }}{{ row.type === 2 ? '%' : '' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="使用门槛" width="130" align="right">
            <template #default="{ row }">
              <span>满 ¥{{ row.minAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="有效期" width="140">
            <template #default="{ row }">
              <span>领取后 {{ row.validDays }} 天</span>
            </template>
          </el-table-column>
          <el-table-column label="库存" width="110" align="center">
            <template #default="{ row }">
              <span>{{ row.remainingStock }}/{{ row.totalStock }}</span>
            </template>
          </el-table-column>
          <el-table-column label="每人限领" width="90" align="center">
            <template #default="{ row }">
              <span>{{ row.perUserLimit }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                :loading="row._statusLoading"
                @change="(val) => toggleStatus(row, val)"
                active-text="启用"
                inactive-text="停用"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
              <el-button size="small" type="success" plain @click="openDistributeDialog(row)">发放</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="templatePage"
            v-model:page-size="templatePageSize"
            :total="templateTotal"
            layout="total, prev, pager, next, jumper"
            @current-change="loadTemplates"
          />
        </div>
      </el-tab-pane>
      <el-tab-pane label="发放记录" name="records">
        <el-table :data="batches" v-loading="batchLoading" border stripe>
          <el-table-column prop="batchName" label="批次名称" min-width="160" />
          <el-table-column label="关联模板" min-width="160">
            <template #default="{ row }">
              <span>{{ getTemplateName(row.templateId) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="发放数量" width="100" align="center" />
          <el-table-column label="发放方式" width="120" align="center">
            <template #default="{ row }">
              <span>{{ row.distributeType === 1 ? '平台发放' : '用户领取' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="发放时间" width="180" />
        </el-table>
        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="batchPage"
            v-model:page-size="batchPageSize"
            :total="batchTotal"
            layout="total, prev, pager, next, jumper"
            @current-change="loadBatches"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑模板' : '新建模板'"
      width="540px"
      :close-on-click-modal="false"
      :before-close="cancelDialog"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" label-position="right">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模板名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="优惠类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option :value="1" label="满减" />
            <el-option :value="2" label="折扣" />
          </el-select>
        </el-form-item>
        <el-form-item label="面值" prop="discountValue">
          <el-input-number v-model="form.discountValue" :min="0" :precision="2" :step="10" style="width:200px" />
          <span class="form-hint">{{ form.type === 1 ? '减免金额（元）' : '折扣比例（如 80 表示 8 折）' }}</span>
        </el-form-item>
        <el-form-item label="使用门槛" prop="minAmount">
          <el-input-number v-model="form.minAmount" :min="0" :precision="2" :step="10" style="width:200px" />
          <span class="form-hint">满 {{ form.minAmount }} 元可用，0 表示无门槛</span>
        </el-form-item>
        <el-form-item label="有效天数" prop="validDays">
          <el-input-number v-model="form.validDays" :min="1" :max="365" style="width:200px" />
          <span class="form-hint">领取后有效天数</span>
        </el-form-item>
        <el-form-item label="总库存" prop="totalStock">
          <el-input-number v-model="form.totalStock" :min="1" :max="999999" style="width:200px" />
        </el-form-item>
        <el-form-item label="每人限领" prop="perUserLimit">
          <el-input-number v-model="form.perUserLimit" :min="1" :max="100" style="width:200px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="distributeVisible"
      title="发放优惠券"
      width="540px"
      :close-on-click-modal="false"
      :before-close="cancelDistribute"
    >
      <el-form ref="distributeFormRef" :model="distributeForm" :rules="distributeRules" label-width="100px" label-position="right">
        <el-form-item label="选择模板" prop="templateId">
          <el-select v-model="distributeForm.templateId" placeholder="请选择优惠券模板" filterable style="width:100%">
            <el-option
              v-for="t in templates"
              :key="t.id"
              :label="t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标用户" prop="userIds">
          <el-input
            v-model="distributeForm.userIdsText"
            type="textarea"
            :rows="4"
            placeholder="输入用户 ID，多个用逗号或换行分隔"
          />
          <span class="form-hint">共 {{ userCount }} 个用户</span>
        </el-form-item>
        <el-form-item label="操作人备注" prop="remark">
          <el-input v-model="distributeForm.remark" placeholder="选填，发放备注" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDistribute">取消</el-button>
        <el-button type="primary" :loading="distributing" @click="submitDistribute">确定发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCouponTemplates,
  createCouponTemplate,
  updateCouponTemplate,
  setCouponTemplateStatus,
  deleteCouponTemplate,
  distributeCoupons,
  getDistributeRecords,
} from '@supermarket/api'

const activeTab = ref('templates')

const loading = ref(false)
const templates = ref<any[]>([])
const templatePage = ref(1)
const templatePageSize = ref(20)
const templateTotal = ref(0)

const batchLoading = ref(false)
const batches = ref<any[]>([])
const batchPage = ref(1)
const batchPageSize = ref(20)
const batchTotal = ref(0)

const templateNameMap = computed(() => {
  const map: Record<number, string> = {}
  for (const t of templates.value) {
    map[t.id] = t.name
  }
  return map
})

function getTemplateName(id: number): string {
  return templateNameMap.value[id] || `模板 #${id}`
}

async function loadTemplates() {
  loading.value = true
  try {
    const res: any = await getCouponTemplates(templatePage.value, templatePageSize.value)
    templates.value = res.records || []
    templateTotal.value = res.total || 0
  } catch {
    templates.value = []
    templateTotal.value = 0
  } finally {
    loading.value = false
  }
}

async function loadBatches() {
  batchLoading.value = true
  try {
    const res: any = await getDistributeRecords(batchPage.value, batchPageSize.value)
    batches.value = res.records || []
    batchTotal.value = res.total || 0
  } catch {
    batches.value = []
    batchTotal.value = 0
  } finally {
    batchLoading.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'templates' && templates.value.length === 0) {
    loadTemplates()
  } else if (tab === 'records' && batches.value.length === 0) {
    loadBatches()
  }
})

loadTemplates()

const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<any>(null)

const defaultForm = () => ({
  name: '',
  type: 1,
  discountValue: 0,
  minAmount: 0,
  validDays: 30,
  totalStock: 100,
  perUserLimit: 1,
})

const form = reactive(defaultForm())

const formRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择优惠类型', trigger: 'change' }],
  discountValue: [{ required: true, message: '请输入面值', trigger: 'blur' }],
  minAmount: [{ required: true, message: '请输入使用门槛', trigger: 'blur' }],
  validDays: [{ required: true, message: '请输入有效天数', trigger: 'blur' }],
  totalStock: [{ required: true, message: '请输入总库存', trigger: 'blur' }],
  perUserLimit: [{ required: true, message: '请输入每人限领数量', trigger: 'blur' }],
}

function openCreateDialog() {
  isEditing.value = false
  editingId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEditDialog(row: any) {
  isEditing.value = true
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.discountValue = row.discountValue
  form.minAmount = row.minAmount
  form.validDays = row.validDays
  form.totalStock = row.totalStock
  form.perUserLimit = row.perUserLimit
  dialogVisible.value = true
}

function cancelDialog() {
  dialogVisible.value = false
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form }
    if (isEditing.value && editingId.value) {
      payload.id = editingId.value
      await updateCouponTemplate(payload)
      ElMessage.success('模板已更新')
    } else {
      await createCouponTemplate(payload)
      ElMessage.success('模板已创建')
    }
    dialogVisible.value = false
    templatePage.value = 1
    await loadTemplates()
  } catch {
    /* message shown by interceptor */
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: any, enabled: boolean) {
  const newStatus = enabled ? 1 : 0
  row._statusLoading = true
  try {
    await setCouponTemplateStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(enabled ? '已启用' : '已停用')
  } catch {
    /* message shown by interceptor */
  } finally {
    row._statusLoading = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定要删除模板「${row.name}」吗？删除后不可恢复。`, '确认删除', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteCouponTemplate(row.id)
    ElMessage.success('模板已删除')
    await loadTemplates()
  } catch {
    /* cancelled or error */
  }
}

const distributeVisible = ref(false)
const distributing = ref(false)
const distributeFormRef = ref<any>(null)

const distributeForm = reactive({
  templateId: null as number | null,
  userIdsText: '',
  remark: '',
})

const distributeRules = {
  templateId: [{ required: true, message: '请选择优惠券模板', trigger: 'change' }],
  userIds: [{ required: true, message: '请输入目标用户 ID', trigger: 'blur' }],
}

const userCount = computed(() => {
  const text = distributeForm.userIdsText.trim()
  if (!text) return 0
  return text.split(/[,，\n\r]+/).filter(Boolean).length
})

function openDistributeDialog(row: any) {
  distributeForm.templateId = row.id
  distributeForm.userIdsText = ''
  distributeForm.remark = ''
  distributeVisible.value = true
}

function cancelDistribute() {
  distributeVisible.value = false
}

async function submitDistribute() {
  const valid = await distributeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (userCount.value === 0) {
    ElMessage.warning('请至少输入一个用户 ID')
    return
  }
  distributing.value = true
  try {
    const ids = distributeForm.userIdsText
      .trim()
      .split(/[,，\n\r]+/)
      .map((s: string) => parseInt(s.trim(), 10))
      .filter((n: number) => !isNaN(n))
    await distributeCoupons(distributeForm.templateId!, ids)
    ElMessage.success(`已向 ${ids.length} 个用户发放优惠券`)
    distributeVisible.value = false
    batchPage.value = 1
    await loadBatches()
  } catch {
    /* message shown by interceptor */
  } finally {
    distributing.value = false
  }
}
</script>

<style scoped>
.coupon-page {
  background: var(--color-surface, #fff);
  padding: 24px;
  border-radius: 8px;
  min-height: 400px;
}
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary, #303133);
}
.page-tabs {
  margin-bottom: 0;
}
.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
