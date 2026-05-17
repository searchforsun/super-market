<template>
  <div class="merchant-page page-enter">
    <h2 class="page-title">商家审核</h2>

    <div class="filter-bar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="待审核" name="0" />
      <el-tab-pane label="已通过" name="1" />
      <el-tab-pane label="已驳回" name="2" />
    </el-tabs>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="companyName" label="商家名称" min-width="150" />
      <el-table-column label="店铺名称" min-width="150">
        <template #default="{ row }">
          {{ row.shopName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="legalPerson" label="联系人" width="110" />
      <el-table-column prop="contactPhone" label="联系电话" width="140" />
      <el-table-column prop="createdAt" label="申请时间" width="175" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTypeMap[row.auditStatus] || 'info'" effect="plain">
            {{ statusLabelMap[row.auditStatus] || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openAudit(row)">审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="审核详情" width="680px" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="100px" label-position="left">
        <el-form-item label="商家名称">
          <span>{{ detail.companyName }}</span>
        </el-form-item>
        <el-form-item label="店铺名称">
          <span>{{ detail.shopName || '-' }}</span>
        </el-form-item>
        <el-form-item label="联系人">
          <span>{{ detail.legalPerson }}</span>
        </el-form-item>
        <el-form-item label="联系电话">
          <span>{{ detail.contactPhone }}</span>
        </el-form-item>
        <el-form-item v-if="detail.shopLogo" label="店铺Logo">
          <el-image :src="detail.shopLogo" style="width: 80px; height: 80px" fit="contain" />
        </el-form-item>
        <el-form-item label="店铺描述">
          <span>{{ detail.shopDesc || '-' }}</span>
        </el-form-item>
        <el-form-item label="营业执照">
          <el-image
            v-if="detail.businessLicense"
            :src="detail.businessLicense"
            style="max-width: 300px; max-height: 180px"
            fit="contain"
            :preview-src-list="[detail.businessLicense]"
          />
          <span v-else>-</span>
        </el-form-item>
        <el-form-item label="申请时间">
          <span>{{ detail.createdAt }}</span>
        </el-form-item>
      </el-form>

      <div v-if="detail.auditStatus !== 0" class="audited-hint">
        该商家已{{ auditedLabel }}
        <template v-if="detail.auditStatus === 2 && detail.auditReason">
          ，原因：{{ detail.auditReason }}
        </template>
      </div>

      <div v-if="showReject" class="reject-area">
        <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
        <div class="reject-actions">
          <el-button @click="cancelReject">取消</el-button>
          <el-button type="danger" @click="confirmReject">确认驳回</el-button>
        </div>
      </div>

      <template #footer v-if="detail.auditStatus === 0 && !showReject">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" @click="showReject = true">驳回</el-button>
        <el-button type="primary" @click="handleApprove">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getMerchantList, auditMerchant, getShopByMerchantId } from '@supermarket/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusTypeMap: Record<number, string> = {
  0: 'warning',
  1: 'success',
  2: 'danger',
}

const statusLabelMap: Record<number, string> = {
  0: '待审核',
  1: '已通过',
  2: '已驳回',
}

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const activeTab = ref('0')
const dateRange = ref<string[] | null>(null)

const dialogVisible = ref(false)
const detail = ref<any>({})
const showReject = ref(false)
const rejectReason = ref('')

const auditedLabel = computed(() => {
  return statusLabelMap[detail.value.auditStatus] || ''
})

async function loadShopInfo(merchantId: number) {
  try {
    const shop: any = await getShopByMerchantId(merchantId)
    return shop
  } catch {
    return null
  }
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value,
      auditStatus: Number(activeTab.value),
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res: any = await getMerchantList(params)
    tableData.value = res.records || []
    total.value = res.total || 0

    const approvedIds = tableData.value
      .filter((m: any) => m.auditStatus === 1)
      .map((m: any) => m.id)

    if (approvedIds.length > 0) {
      const shopResults = await Promise.allSettled(
        approvedIds.map((id: number) => getShopByMerchantId(id))
      )
      shopResults.forEach((result, idx) => {
        if (result.status === 'fulfilled') {
          const shop: any = result.value
          const item = tableData.value.find((m: any) => m.id === approvedIds[idx])
          if (item && shop) {
            item.shopName = shop.shopName
            item.shopLogo = shop.shopLogo
            item.shopDesc = shop.shopDesc
          }
        }
      })
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function handleReset() {
  dateRange.value = null
  page.value = 1
  loadData()
}

function onTabChange() {
  page.value = 1
  loadData()
}

async function openAudit(row: any) {
  detail.value = { ...row }
  showReject.value = false
  rejectReason.value = ''

  if (row.auditStatus === 1 && !detail.value.shopName) {
    const shop: any = await loadShopInfo(row.id)
    if (shop) {
      detail.value.shopName = shop.shopName
      detail.value.shopLogo = shop.shopLogo
      detail.value.shopDesc = shop.shopDesc
    }
  }

  dialogVisible.value = true
}

async function handleApprove() {
  let confirmed = false
  try {
    await ElMessageBox.confirm('确认通过该商家的入驻申请？', '操作确认', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'info',
    })
    confirmed = true
  } catch {
    // cancelled
    return
  }
  if (!confirmed) return
  try {
    await auditMerchant(detail.value.id, 1)
    ElMessage.success('审核已通过')
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('审核操作失败')
  }
}

function cancelReject() {
  showReject.value = false
  rejectReason.value = ''
}

async function confirmReject() {
  const reason = rejectReason.value.trim()
  if (!reason) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  try {
    await auditMerchant(detail.value.id, 2, reason)
    ElMessage.success('已驳回')
    dialogVisible.value = false
    showReject.value = false
    rejectReason.value = ''
    loadData()
  } catch {
    ElMessage.error('驳回操作失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.merchant-page {
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

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.audited-hint {
  text-align: center;
  padding: 12px 0;
  color: var(--color-text-muted, #999);
  font-size: 14px;
}

.reject-area {
  border-top: 1px solid #ebeef5;
  padding: 16px 0 0;
  margin-top: 8px;
}

.reject-actions {
  margin-top: 10px;
  text-align: right;
}
</style>
