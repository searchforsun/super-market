<template>
  <div class="risk-page page-enter">
    <h2 class="page-title">风控管理</h2>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="风控规则" name="rules">
          <div class="toolbar">
            <el-button type="primary" @click="openCreateDialog">新建规则</el-button>
          </div>

          <el-table :data="rules" v-loading="rulesLoading" stripe style="width:100%">
            <el-table-column prop="name" label="规则名称" min-width="140" />
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="ruleTypeTag(row.type)" effect="plain">
                  {{ ruleTypeLabel(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="threshold" label="阈值" width="100" />
            <el-table-column label="动作" width="120">
              <template #default="{ row }">
                {{ actionLabel(row.action) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.status === 1"
                  :loading="row._switchLoading"
                  @change="(val) => toggleRuleStatus(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="rulePage"
              v-model:page-size="rulePageSize"
              :total="ruleTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="fetchRules"
              @size-change="fetchRules"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="风控日志" name="logs">
          <div class="filter-bar">
            <el-input
              v-model="logFilter.userId"
              placeholder="用户ID"
              clearable
              style="width:160px"
              @clear="fetchLogs(1)"
            />
            <el-select
              v-model="logFilter.ruleType"
              placeholder="规则类型"
              clearable
              style="width:140px"
              @change="fetchLogs(1)"
            >
              <el-option label="全部类型" :value="undefined" />
              <el-option label="刷单" :value="1" />
              <el-option label="恶意退款" :value="2" />
              <el-option label="辱骂" :value="3" />
              <el-option label="异常IP" :value="4" />
            </el-select>
            <el-select
              v-model="logFilter.riskLevel"
              placeholder="风险等级"
              clearable
              style="width:130px"
              @change="fetchLogs(1)"
            >
              <el-option label="全部等级" :value="undefined" />
              <el-option label="高" :value="3" />
              <el-option label="中" :value="2" />
              <el-option label="低" :value="1" />
            </el-select>
            <el-date-picker
              v-model="logDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width:260px"
              @change="fetchLogs(1)"
            />
            <el-button type="primary" @click="fetchLogs(1)">搜索</el-button>
            <el-button @click="resetLogFilter">重置</el-button>
          </div>

          <el-table :data="logs" v-loading="logsLoading" stripe style="width:100%">
            <el-table-column prop="triggerTime" label="触发时间" width="170" />
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column prop="ruleName" label="规则名称" min-width="120" />
            <el-table-column label="风险等级" width="100">
              <template #default="{ row }">
                <el-tag :type="riskLevelTag(row.riskLevel)" effect="dark" size="small">
                  {{ riskLevelLabel(row.riskLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="详情描述" min-width="200" show-overflow-tooltip />
            <el-table-column label="处理状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.handled === 1 ? 'success' : 'warning'" size="small">
                  {{ row.handled === 1 ? '已处理' : '未处理' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  :disabled="row.handled === 1"
                  @click="handleMarkLog(row)"
                >
                  标记处理
                </el-button>
                <el-button link type="primary" size="small" @click="viewLogDetail(row)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="logPage"
              v-model:page-size="logPageSize"
              :total="logTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="fetchLogs"
              @size-change="fetchLogs(1)"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="ruleDialogVisible"
      :title="isEditing ? '编辑规则' : '新建规则'"
      width="560px"
      :close-on-click-modal="false"
      @close="resetRuleForm"
    >
      <el-form
        ref="ruleFormRef"
        :model="ruleForm"
        :rules="ruleFormRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="ruleForm.name" placeholder="请输入规则名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="ruleForm.type" placeholder="请选择类型" style="width:100%">
            <el-option label="刷单" :value="1" />
            <el-option label="恶意退款" :value="2" />
            <el-option label="辱骂" :value="3" />
            <el-option label="异常IP" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="threshold">
          <el-input-number
            v-model="ruleForm.threshold"
            :min="1"
            :max="999999"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="动作" prop="action">
          <el-select v-model="ruleForm.action" placeholder="请选择动作" style="width:100%">
            <el-option label="警告" :value="1" />
            <el-option label="限制下单" :value="2" />
            <el-option label="封禁" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="ruleForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="ruleForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入规则描述（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleSaving" @click="submitRuleForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="logDetailVisible"
      title="日志详情"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="log-detail" v-if="logDetail">
        <div class="detail-row">
          <span class="detail-label">触发时间</span>
          <span class="detail-value">{{ logDetail.triggerTime }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">用户ID</span>
          <span class="detail-value">{{ logDetail.userId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">规则名称</span>
          <span class="detail-value">{{ logDetail.ruleName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">规则类型</span>
          <span class="detail-value">
            <el-tag :type="ruleTypeTag(logDetail.ruleType)" effect="plain" size="small">
              {{ ruleTypeLabel(logDetail.ruleType) }}
            </el-tag>
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-label">风险等级</span>
          <span class="detail-value">
            <el-tag :type="riskLevelTag(logDetail.riskLevel)" effect="dark" size="small">
              {{ riskLevelLabel(logDetail.riskLevel) }}
            </el-tag>
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-label">详情描述</span>
          <span class="detail-value">{{ logDetail.description }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">处理状态</span>
          <span class="detail-value">
            <el-tag :type="logDetail.handled === 1 ? 'success' : 'warning'" size="small">
              {{ logDetail.handled === 1 ? '已处理' : '未处理' }}
            </el-tag>
          </span>
        </div>
        <div class="detail-row" v-if="logDetail.handleTime">
          <span class="detail-label">处理时间</span>
          <span class="detail-value">{{ logDetail.handleTime }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="logDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getRiskRules,
  createRiskRule,
  updateRiskRule,
  deleteRiskRule,
  getRiskLogs,
  handleRiskLog,
  getRiskLogDetail,
} from '@supermarket/api'

interface RiskRule {
  id: number
  name: string
  type: number
  threshold: number
  action: number
  status: number
  description?: string
  createTime?: string
  updateTime?: string
  _switchLoading?: boolean
}

interface RiskLog {
  id: number
  triggerTime: string
  userId: string | number
  ruleName: string
  ruleType: number
  riskLevel: number
  description: string
  handled: number
  handleTime?: string
  detail?: string
}

const activeTab = ref('rules')

const rules = ref<RiskRule[]>([])
const rulesLoading = ref(false)
const rulePage = ref(1)
const rulePageSize = ref(20)
const ruleTotal = ref(0)

async function fetchRules() {
  rulesLoading.value = true
  try {
    const res = await getRiskRules({
      page: rulePage.value,
      size: rulePageSize.value,
    })
    const data = res as any
    rules.value = (data.records || data.list || []).map((r: RiskRule) => ({
      ...r,
      _switchLoading: false,
    }))
    ruleTotal.value = data.total || 0
  } catch {
    rules.value = []
    ruleTotal.value = 0
  } finally {
    rulesLoading.value = false
  }
}

async function toggleRuleStatus(row: RiskRule, val: boolean | string | number) {
  const newStatus = val ? 1 : 0
  row._switchLoading = true
  try {
    await updateRiskRule({ id: row.id, status: newStatus })
    row.status = newStatus
    ElMessage.success('状态已更新')
  } catch {
    ElMessage.error('状态更新失败')
  } finally {
    row._switchLoading = false
  }
}

const ruleDialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const ruleSaving = ref(false)
const ruleFormRef = ref<FormInstance>()

const ruleForm = reactive({
  name: '',
  type: undefined as number | undefined,
  threshold: 10,
  action: undefined as number | undefined,
  status: 1 as number | string,
  description: '',
})

const ruleFormRules: FormRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  threshold: [{ required: true, message: '请输入阈值', trigger: 'blur' }],
  action: [{ required: true, message: '请选择动作', trigger: 'change' }],
}

function resetRuleForm() {
  ruleForm.name = ''
  ruleForm.type = undefined
  ruleForm.threshold = 10
  ruleForm.action = undefined
  ruleForm.status = 1
  ruleForm.description = ''
  isEditing.value = false
  editingId.value = null
}

function openCreateDialog() {
  resetRuleForm()
  ruleDialogVisible.value = true
}

function openEditDialog(row: RiskRule) {
  isEditing.value = true
  editingId.value = row.id
  ruleForm.name = row.name
  ruleForm.type = row.type
  ruleForm.threshold = row.threshold
  ruleForm.action = row.action
  ruleForm.status = row.status
  ruleForm.description = row.description || ''
  ruleDialogVisible.value = true
}

async function submitRuleForm() {
  const valid = await ruleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  ruleSaving.value = true
  try {
    if (isEditing.value && editingId.value) {
      await updateRiskRule({
        id: editingId.value,
        name: ruleForm.name,
        type: ruleForm.type as number,
        threshold: ruleForm.threshold,
        action: ruleForm.action as number,
        status: ruleForm.status as number,
        description: ruleForm.description,
      })
      ElMessage.success('规则已更新')
    } else {
      await createRiskRule({
        name: ruleForm.name,
        type: ruleForm.type as number,
        threshold: ruleForm.threshold,
        action: ruleForm.action as number,
        status: ruleForm.status as number,
        description: ruleForm.description,
      })
      ElMessage.success('规则已创建')
    }
    ruleDialogVisible.value = false
    fetchRules()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    ruleSaving.value = false
  }
}

async function handleDeleteRule(row: RiskRule) {
  try {
    await ElMessageBox.confirm(`确认删除规则「${row.name}」？`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteRiskRule(row.id)
    ElMessage.success('规则已删除')
    fetchRules()
  } catch {
    // cancelled
  }
}

const logs = ref<RiskLog[]>([])
const logsLoading = ref(false)
const logPage = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)

const logFilter = reactive({
  userId: '',
  ruleType: undefined as number | undefined,
  riskLevel: undefined as number | undefined,
})
const logDateRange = ref<[string, string] | null>(null)

function resetLogFilter() {
  logFilter.userId = ''
  logFilter.ruleType = undefined
  logFilter.riskLevel = undefined
  logDateRange.value = null
  fetchLogs(1)
}

async function fetchLogs(page?: number) {
  if (page) logPage.value = page
  logsLoading.value = true
  try {
    const params: Record<string, any> = {
      page: logPage.value,
      size: logPageSize.value,
    }
    if (logFilter.userId) params.userId = logFilter.userId
    if (logFilter.ruleType !== undefined) params.ruleType = logFilter.ruleType
    if (logFilter.riskLevel !== undefined) params.riskLevel = logFilter.riskLevel
    if (logDateRange.value) {
      params.startTime = logDateRange.value[0]
      params.endTime = logDateRange.value[1]
    }
    const res = await getRiskLogs(params)
    const data = res as any
    logs.value = data.records || data.list || []
    logTotal.value = data.total || 0
  } catch {
    logs.value = []
    logTotal.value = 0
  } finally {
    logsLoading.value = false
  }
}

async function handleMarkLog(row: RiskLog) {
  try {
    await handleRiskLog(row.id)
    row.handled = 1
    ElMessage.success('已标记为已处理')
  } catch {
    ElMessage.error('操作失败')
  }
}

const logDetailVisible = ref(false)
const logDetail = ref<RiskLog | null>(null)

async function viewLogDetail(row: RiskLog) {
  logDetail.value = null
  try {
    const res = await getRiskLogDetail(row.id)
    logDetail.value = res as any
  } catch {
    logDetail.value = { ...row }
  }
  logDetailVisible.value = true
}

function ruleTypeLabel(type: number): string {
  const map: Record<number, string> = { 1: '刷单', 2: '恶意退款', 3: '辱骂', 4: '异常IP' }
  return map[type] || '未知'
}

function ruleTypeTag(type: number): string {
  const map: Record<number, string> = { 1: 'danger', 2: 'warning', 3: 'danger', 4: 'info' }
  return map[type] || ''
}

function actionLabel(action: number): string {
  const map: Record<number, string> = { 1: '警告', 2: '限制下单', 3: '封禁' }
  return map[action] || '未知'
}

function riskLevelLabel(level: number): string {
  const map: Record<number, string> = { 3: '高', 2: '中', 1: '低' }
  return map[level] || '未知'
}

function riskLevelTag(level: number): string {
  const map: Record<number, string> = { 3: 'danger', 2: 'warning', 1: 'warning' }
  return map[level] || ''
}

onMounted(() => {
  fetchRules()
})
</script>

<style scoped>
.risk-page {
  padding: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 16px 0;
  color: #1a1a1a;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.log-detail {
  padding: 0 8px;
}

.detail-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  width: 100px;
  flex-shrink: 0;
  color: #666;
  font-size: 14px;
}

.detail-value {
  flex: 1;
  color: #1a1a1a;
  font-size: 14px;
  word-break: break-all;
}
</style>