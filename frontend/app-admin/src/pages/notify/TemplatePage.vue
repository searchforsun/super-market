<template>
  <div class="template-page page-enter">
    <div class="page-header">
      <h2 class="page-title">通知模板管理</h2>
      <el-button type="primary" @click="openCreate">新建模板</el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="templates" v-loading="loading" stripe empty-text="暂无通知模板">
        <el-table-column prop="name" label="模板名称" min-width="160" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTag(row._type)" size="small" effect="plain">
              {{ typeLabel(row._type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="触发场景" width="140">
          <template #default="{ row }">
            {{ sceneLabel(row._scene) }}
          </template>
        </el-table-column>
        <el-table-column label="发送方式" width="200">
          <template #default="{ row }">
            <el-tag
              v-if="row.channel & 1"
              size="small"
              style="margin-right: 4px"
            >站内信</el-tag>
            <el-tag
              v-if="row.channel & 4"
              size="small"
              type="success"
              style="margin-right: 4px"
            >邮件</el-tag>
            <el-tag
              v-if="row.channel & 2"
              size="small"
              type="warning"
            >短信</el-tag>
            <span v-if="!row.channel" class="no-channel">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="row._statusLoading"
              @change="(val) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">
            {{ row.createdAt }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" @click="openTest(row)">测试</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchData"
          @size-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑通知模板' : '新建通知模板'"
      width="680px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模板名称" maxlength="60" show-word-limit />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="通知类型" prop="_type">
              <el-select v-model="form._type" placeholder="请选择类型" style="width: 100%" @change="onTypeChange">
                <el-option
                  v-for="t in typeOptions"
                  :key="t.value"
                  :label="t.label"
                  :value="t.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="触发场景" prop="_scene">
              <el-select v-model="form._scene" placeholder="请选择场景" style="width: 100%">
                <el-option
                  v-for="s in currentScenes"
                  :key="s.value"
                  :label="s.label"
                  :value="s.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="发送方式" prop="channels">
          <el-checkbox-group v-model="form.channels">
            <el-checkbox :label="1">站内信</el-checkbox>
            <el-checkbox :label="4">邮件</el-checkbox>
            <el-checkbox :label="2">短信</el-checkbox>
          </el-checkbox-group>
          <div class="form-tip">至少选择一种发送方式</div>
        </el-form-item>

        <el-form-item label="标题模板" prop="title">
          <el-input v-model="form.title" placeholder="例如：订单 {{orderNo}} 已发货" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="内容模板" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="请输入通知内容模板，使用 {{变量名}} 作为占位符"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="可用变量">
          <div class="variable-hints">
            <span
              v-for="v in currentVariables"
              :key="v.value"
              class="variable-tag"
              @click="insertVariable(v.value)"
            >
              {{ v.value }}
              <span class="variable-label">{{ v.label }}</span>
            </span>
            <span v-if="currentVariables.length === 0" class="no-variable">请先选择触发场景</span>
          </div>
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="testVisible"
      title="测试发送通知"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="testFormRef" :model="testForm" :rules="testRules" label-width="90px">
        <el-form-item label="模板名称">
          <span class="test-template-name">{{ testForm.templateName }}</span>
        </el-form-item>
        <el-form-item label="发送方式">
          <el-tag
            v-if="testForm.channel & 1"
            size="small"
            style="margin-right: 4px"
          >站内信</el-tag>
          <el-tag
            v-if="testForm.channel & 4"
            size="small"
            type="success"
            style="margin-right: 4px"
          >邮件</el-tag>
          <el-tag
            v-if="testForm.channel & 2"
            size="small"
            type="warning"
          >短信</el-tag>
        </el-form-item>
        <el-form-item label="用户 ID" prop="userId">
          <el-input-number v-model="testForm.userId" :min="1" :max="99999999" style="width: 100%" placeholder="请输入用户 ID" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="testForm.phone" placeholder="发送短信时必填" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="testForm.email" placeholder="发送邮件时必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testVisible = false">取消</el-button>
        <el-button type="primary" :loading="testing" @click="handleTest">发送测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getNotifyTemplates,
  createNotifyTemplate,
  updateNotifyTemplate,
  deleteNotifyTemplate,
  setNotifyTemplateStatus,
  testNotifyTemplate,
} from '@supermarket/api'

const loading = ref(false)
const templates = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<any>(null)

const testVisible = ref(false)
const testing = ref(false)
const testFormRef = ref<any>(null)

const typeOptions = [
  { value: 'system', label: '系统通知' },
  { value: 'order', label: '订单通知' },
  { value: 'promotion', label: '促销通知' },
  { value: 'verify', label: '验证码' },
]

const sceneOptions: Record<string, { value: string; label: string }[]> = {
  system: [
    { value: 'system_notice', label: '系统公告' },
  ],
  order: [
    { value: 'order_create', label: '订单创建' },
    { value: 'order_ship', label: '订单发货' },
    { value: 'order_deliver', label: '订单送达' },
    { value: 'order_refund', label: '订单退款' },
  ],
  promotion: [
    { value: 'seckill_start', label: '秒杀开始' },
    { value: 'seckill_success', label: '秒杀成功' },
  ],
  verify: [
    { value: 'verify_register', label: '注册验证码' },
    { value: 'verify_login', label: '登录验证码' },
  ],
}

const sceneVariableMap: Record<string, { label: string; value: string }[]> = {
  order_create: [
    { label: '订单号', value: '{orderNo}' },
    { label: '用户名', value: '{userName}' },
    { label: '订单金额', value: '{amount}' },
  ],
  order_ship: [
    { label: '订单号', value: '{orderNo}' },
    { label: '用户名', value: '{userName}' },
    { label: '物流单号', value: '{trackingNo}' },
  ],
  order_deliver: [
    { label: '订单号', value: '{orderNo}' },
    { label: '用户名', value: '{userName}' },
  ],
  order_refund: [
    { label: '订单号', value: '{orderNo}' },
    { label: '用户名', value: '{userName}' },
    { label: '退款金额', value: '{amount}' },
  ],
  seckill_start: [
    { label: '秒杀活动', value: '{seckillName}' },
    { label: '商品名称', value: '{productName}' },
  ],
  seckill_success: [
    { label: '秒杀活动', value: '{seckillName}' },
    { label: '商品名称', value: '{productName}' },
    { label: '用户名', value: '{userName}' },
  ],
  verify_register: [
    { label: '验证码', value: '{code}' },
    { label: '用户名', value: '{userName}' },
  ],
  verify_login: [
    { label: '验证码', value: '{code}' },
    { label: '用户名', value: '{userName}' },
  ],
  system_notice: [
    { label: '用户名', value: '{userName}' },
  ],
}

function typeLabel(val: string): string {
  const t = typeOptions.find((o) => o.value === val)
  return t ? t.label : val
}

function typeTag(val: string): string {
  const map: Record<string, string> = {
    system: 'info',
    order: 'primary',
    promotion: 'warning',
    verify: 'danger',
  }
  return map[val] || 'info'
}

function sceneLabel(val: string): string {
  for (const group of Object.values(sceneOptions)) {
    const s = group.find((o) => o.value === val)
    if (s) return s.label
  }
  return val
}

function parseTemplateCode(code: string): { type: string; scene: string } {
  if (!code) return { type: 'system', scene: 'system_notice' }
  const parts = code.split('_')
  if (parts.length >= 2) {
    const type = parts[0]
    const scene = parts.slice(1).join('_')
    if (sceneOptions[type]?.some((s) => s.value === scene)) {
      return { type, scene }
    }
  }
  return { type: 'system', scene: 'system_notice' }
}

function buildCode(type: string, scene: string): string {
  return `${type}_${scene}`
}

const currentScenes = computed(() => {
  return sceneOptions[form._type] || []
})

const currentVariables = computed(() => {
  const key = buildCode(form._type, form._scene)
  return sceneVariableMap[key] || []
})

const form = reactive({
  name: '',
  _type: 'system',
  _scene: 'system_notice',
  channels: [1],
  title: '',
  content: '',
  status: 1,
})

const formRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  _type: [{ required: true, message: '请选择通知类型', trigger: 'change' }],
  _scene: [{ required: true, message: '请选择触发场景', trigger: 'change' }],
  channels: [{ required: true, message: '请至少选择一种发送方式', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题模板', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容模板', trigger: 'blur' }],
}

const testForm = reactive({
  templateId: 0,
  templateName: '',
  channel: 0,
  userId: undefined as number | undefined,
  phone: '',
  email: '',
})

const testRules = {
  userId: [{ required: true, message: '请输入用户 ID', trigger: 'blur' }],
}

function resetForm() {
  form.name = ''
  form._type = 'system'
  form._scene = 'system_notice'
  form.channels = [1]
  form.title = ''
  form.content = ''
  form.status = 1
  isEdit.value = false
  editingId.value = null
}

function onTypeChange() {
  const scenes = sceneOptions[form._type]
  if (scenes && scenes.length > 0) {
    form._scene = scenes[0].value
  }
}

function channelsToMask(arr: number[]): number {
  return arr.reduce((s, v) => s + v, 0)
}

function maskToChannels(mask: number): number[] {
  const result: number[] = []
  if (mask & 1) result.push(1)
  if (mask & 2) result.push(2)
  if (mask & 4) result.push(4)
  return result
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getNotifyTemplates(page.value, pageSize.value)
    const list: any[] = res.records || res || []
    list.forEach((item) => {
      const parsed = parseTemplateCode(item.code)
      item._type = parsed.type
      item._scene = parsed.scene
      item._statusLoading = false
    })
    templates.value = list
    total.value = res.total || list.length
  } catch {
    templates.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form._type = row._type
  form._scene = row._scene
  form.channels = maskToChannels(row.channel || 0)
  form.title = row.title || ''
  form.content = row.content || ''
  form.status = row.status ?? 1
  dialogVisible.value = true
}

function buildPayload() {
  return {
    name: form.name,
    code: buildCode(form._type, form._scene),
    channel: channelsToMask(form.channels),
    title: form.title,
    content: form.content,
    status: form.status,
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value && editingId.value) {
      payload.id = editingId.value
      await updateNotifyTemplate(payload)
      ElMessage.success('模板已更新')
    } else {
      await createNotifyTemplate(payload)
      ElMessage.success('模板已创建')
    }
    dialogVisible.value = false
    await fetchData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板「${row.name}」吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteNotifyTemplate(row.id)
    ElMessage.success('模板已删除')
    await fetchData()
  } catch {
    /* cancelled or error */
  }
}

async function toggleStatus(row: any, val: boolean) {
  const newStatus = val ? 1 : 0
  row._statusLoading = true
  try {
    await setNotifyTemplateStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus ? '模板已启用' : '模板已停用')
  } catch {
    ElMessage.error('操作失败')
  } finally {
    row._statusLoading = false
  }
}

function openTest(row: any) {
  testForm.templateId = row.id
  testForm.templateName = row.name
  testForm.channel = row.channel || 0
  testForm.userId = undefined
  testForm.phone = ''
  testForm.email = ''
  testVisible.value = true
}

async function handleTest() {
  const valid = await testFormRef.value.validate().catch(() => false)
  if (!valid) return
  testing.value = true
  try {
    await testNotifyTemplate({
      templateId: testForm.templateId,
      userId: testForm.userId,
      phone: testForm.phone || undefined,
      email: testForm.email || undefined,
    })
    ElMessage.success('测试通知已发送')
    testVisible.value = false
  } catch {
    ElMessage.error('发送失败')
  } finally {
    testing.value = false
  }
}

function insertVariable(variable: string) {
  const target = document.activeElement as HTMLInputElement | HTMLTextAreaElement | null
  if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) {
    const start = target.selectionStart ?? target.value.length
    const end = target.selectionEnd ?? start
    target.value =
      target.value.substring(0, start) + variable + target.value.substring(end)
    target.dispatchEvent(new Event('input'))
    target.selectionStart = target.selectionEnd = start + variable.length
    target.focus()
    return
  }
  if (!form.content.includes(variable)) {
    form.content += variable
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.template-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
}

.table-card {
  border-radius: 6px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}

.no-channel {
  color: #aaa;
  font-size: 13px;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  line-height: 1.4;
}

.variable-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.variable-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
  user-select: none;
}

.variable-tag:hover {
  background: #d9ecff;
}

.variable-label {
  font-size: 11px;
  color: #79bbff;
}

.no-variable {
  font-size: 13px;
  color: #bbb;
}

.test-template-name {
  font-weight: 600;
  color: #333;
}
</style>
