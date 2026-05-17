<template>
  <div class="profile-edit-page page-enter">
    <div class="page-header">
      <el-button :icon="ArrowLeft" text @click="$router.back()">返回</el-button>
      <h2 class="page-title">编辑资料</h2>
    </div>

    <el-card shadow="never" class="form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        label-position="right"
      >
        <el-form-item label="头像">
          <div class="avatar-section">
            <el-avatar :size="72" :src="resolveImageUrl(form.avatarUrl)">
              {{ form.nickname?.charAt(0)?.toUpperCase() || '用' }}
            </el-avatar>
            <FileUploader
              :max-count="1"
              :current-files="form.avatarUrl ? [resolveImageUrl(form.avatarUrl)] : []"
              tip="支持 jpg/png，不超过 10MB"
              @success="onAvatarUploaded"
              @update:files="(urls: string[]) => form.avatarUrl = urls[0] || ''"
            />
          </div>
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="请输入昵称"
            maxlength="30"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱地址"
            type="email"
            clearable
          />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input :model-value="maskedPhone" disabled />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">
            保存修改
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@supermarket/stores'
import { resolveImageUrl } from '@supermarket/utils'
import { FileUploader } from '@supermarket/ui'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  avatarUrl: '',
})

const rules = {
  nickname: [
    { min: 2, max: 30, message: '昵称长度应在 2-30 个字符之间', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
  ],
}

const maskedPhone = computed(() => {
  const phone = userStore.userInfo?.phone || ''
  if (phone.length === 11) {
    return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
  }
  return phone
})

function initForm() {
  const info = userStore.userInfo
  if (info) {
    form.nickname = info.nickname || ''
    form.email = info.email || ''
    form.avatarUrl = info.avatarUrl || ''
  }
}

function onAvatarUploaded(url: string) {
  form.avatarUrl = url
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await userStore.updateProfile({
      nickname: form.nickname,
      email: form.email,
      avatarUrl: form.avatarUrl,
    })
    ElMessage.success('资料已更新')
    router.back()
  } catch (err: any) {
    ElMessage.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (!userStore.userId) {
    router.replace('/login')
    return
  }
  initForm()
})
</script>

<style scoped>
.profile-edit-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 0 0 32px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 0 4px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary, #303133);
  margin: 0;
}

.form-card {
  border-radius: 12px;
  border: 1px solid var(--color-border, #ebeef5);
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}
</style>
