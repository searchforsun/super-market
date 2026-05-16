<template>
  <div class="register-wrapper">
    <div class="register-card">
      <div class="register-header">
        <h1>Super Market</h1>
        <p>商家注册</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="doRegister">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item prop="merchantName">
          <el-input v-model="form.merchantName" placeholder="商家名称" :prefix-icon="Shop" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">注 册</el-button>
        </el-form-item>
      </el-form>
      <p class="register-tip">
        已有账号？<router-link to="/login">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Phone, Lock, Shop } from '@element-plus/icons-vue'
import { register, authLogin } from '@supermarket/api'
import { applyMerchant } from '@supermarket/api'
import { saveAuth } from '@supermarket/utils'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const formRef = ref()

const form = reactive({
  phone: '',
  merchantName: '',
  password: '',
  confirmPassword: '',
})

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  merchantName: [
    { required: true, message: '请输入商家名称', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function doRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const userRes: any = await register(form.phone, form.password)
    const userId = userRes?.id || userRes?.data?.id
    if (userId && form.merchantName) {
      try {
        await applyMerchant({
          userId,
          merchantName: form.merchantName,
          contactName: form.merchantName,
          contactPhone: form.phone,
        })
      } catch { /* merchant apply failure shouldn't block login */ }
    }
    const data = await authLogin(form.phone, form.password)
    saveAuth(data)
    ElMessage.success('注册成功')
    router.push('/merchant')
  } catch {
    ElMessage.error('注册失败，该手机号可能已被注册')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}
.register-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}
.register-header {
  text-align: center;
  margin-bottom: 32px;
}
.register-header h1 {
  font-size: 24px;
  color: #3b82f6;
  margin: 0 0 8px;
}
.register-header p {
  font-size: 14px;
  color: #999;
  margin: 0;
}
.register-tip {
  text-align: center;
  font-size: 13px;
  color: #bbb;
  margin-top: 16px;
}
.register-tip a { color: #3b82f6; text-decoration: none; }
</style>
