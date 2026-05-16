<template>
  <div class="shop-page">
    <h2 class="page-title">店铺设置</h2>

    <el-card shadow="never" class="section-card">
      <template #header>基本信息</template>
      <el-form :model="form" label-width="120px" label-position="right">
        <el-form-item label="店铺名称">
          <el-input v-model="form.shopName" placeholder="请输入店铺名称" maxlength="50" />
        </el-form-item>

        <el-form-item label="店铺Logo">
          <div class="logo-wrapper">
            <img v-if="form.shopLogo" :src="form.shopLogo" class="logo-preview" />
            <FileUploader @success="onLogoSuccess" tip="建议尺寸 200x200，支持 jpg/png/gif" />
          </div>
        </el-form-item>

        <el-form-item label="店铺简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请填写店铺简介"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>联系方式</template>
      <el-form :model="form" label-width="120px" label-position="right">
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" maxlength="20" />
        </el-form-item>

        <el-form-item label="联系邮箱">
          <el-input v-model="form.contactEmail" placeholder="请输入联系邮箱" maxlength="100" />
        </el-form-item>

        <el-form-item label="客服时间">
          <el-input v-model="form.serviceHours" placeholder="例如：周一至周五 09:00-18:00" maxlength="100" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>地址信息</template>
      <el-form :model="form" label-width="120px" label-position="right">
        <el-form-item label="所在地区">
          <div class="region-row">
            <el-input v-model="form.province" placeholder="省" class="region-input" />
            <el-input v-model="form.city" placeholder="市" class="region-input" />
            <el-input v-model="form.district" placeholder="区" class="region-input" />
          </div>
        </el-form-item>

        <el-form-item label="详细地址">
          <el-input
            v-model="form.detailAddress"
            placeholder="请输入详细地址"
            maxlength="200"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <div class="action-bar">
      <el-button type="primary" :loading="saving" size="large" @click="handleSave">
        保存设置
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { request, getShopByMerchantId } from '@supermarket/api'
import { FileUploader } from '@supermarket/ui'
import { USER_ID_KEY } from '@supermarket/utils'

interface ShopForm {
  shopName: string
  shopLogo: string
  description: string
  contactPhone: string
  contactEmail: string
  serviceHours: string
  province: string
  city: string
  district: string
  detailAddress: string
}

const form = reactive<ShopForm>({
  shopName: '',
  shopLogo: '',
  description: '',
  contactPhone: '',
  contactEmail: '',
  serviceHours: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
})

const saving = ref(false)

function onLogoSuccess(url: string) {
  form.shopLogo = url
}

async function fetchShopInfo() {
  try {
    const userId = Number(localStorage.getItem(USER_ID_KEY)) || 0
    if (!userId) return
    const data: Record<string, any> = await getShopByMerchantId(userId)
    if (data) {
      form.shopName = data.shopName || ''
      form.shopLogo = data.shopLogo || ''
      form.description = data.description || data.shopDesc || ''
      form.contactPhone = data.contactPhone || ''
      form.contactEmail = data.contactEmail || ''
      form.serviceHours = data.serviceHours || ''
      form.province = data.province || ''
      form.city = data.city || ''
      form.district = data.district || ''
      form.detailAddress = data.detailAddress || ''
    }
  } catch {
    ElMessage.error('获取店铺信息失败')
  }
}

async function handleSave() {
  saving.value = true
  try {
    await request.put('/shop', { ...form })
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchShopInfo()
})
</script>

<style scoped>
.shop-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #303133;
}

.section-card {
  margin-bottom: 20px;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-preview {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #e4e7ed;
}

.region-row {
  display: flex;
  gap: 12px;
}

.region-input {
  width: 160px;
}

.action-bar {
  margin-top: 32px;
  text-align: center;
}
</style>
