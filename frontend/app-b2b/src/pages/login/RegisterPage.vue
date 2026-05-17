<template>
  <div class="register-page page-enter">
    <div class="register-card">
      <router-link to="/merchant" class="register-logo">
        <span class="logo-mark">S</span>
        <span class="logo-text">Super Market</span>
      </router-link>
      <h2 class="register-heading">商家注册</h2>
      <p class="register-sub">创建你的商家账号</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="doRegister">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item prop="merchantName">
          <el-input v-model="form.merchantName" placeholder="商家名称" :prefix-icon="Shop" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading"
            style="width:100%;height:44px;border-radius:8px;font-weight:500;background:var(--color-text-primary);border:none;">
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <p class="register-switch">
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
    // 1. Register user
    await register(form.phone, form.password)
    // 2. Login to get correct userId (string from auth response, no JS precision loss)
    let data = await authLogin(form.phone, form.password)
    // 3. Apply merchant with correct userId
    if (data.userId && form.merchantName) {
      try {
        await applyMerchant({
          userId: data.userId,
          merchantName: form.merchantName,
          contactName: form.merchantName,
          contactPhone: form.phone,
        })
        // 4. Re-login so auth-service picks up ROLE_MERCHANT via hasMerchant
        data = await authLogin(form.phone, form.password)
      } catch { /* merchant apply failure shouldn't block login */ }
    }
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
.register-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1a1816 0%, #2d2824 100%);
}
.register-card {
  background: var(--color-surface, #fff);
  padding: 48px 40px; border-radius: var(--radius-xl, 16px);
  width: 420px; box-shadow: var(--shadow-xl, 0 24px 64px rgba(0,0,0,0.2));
}
.register-logo {
  display: flex; align-items: center; justify-content: center;
  gap: 10px; margin-bottom: var(--space-xl, 32px); text-decoration: none;
}
.logo-mark {
  width: 40px; height: 40px;
  background: var(--color-accent, #c41e3a); color: #fff;
  font-family: var(--font-display, 'Playfair Display', serif);
  font-size: 22px; font-weight: 700;
  border-radius: var(--radius-md, 8px);
  display: flex; align-items: center; justify-content: center;
}
.logo-text {
  font-family: var(--font-display, 'Playfair Display', serif);
  font-size: 22px; font-weight: 600;
  color: var(--color-text-primary, #1a1816);
}
.register-heading {
  text-align: center; font-family: var(--font-display, 'Playfair Display', serif);
  font-size: 24px; font-weight: 600; margin-bottom: var(--space-sm, 8px);
}
.register-sub {
  text-align: center; font-size: 14px;
  color: var(--color-text-muted, #a09890);
  margin-bottom: var(--space-xl, 32px);
}
.register-switch {
  text-align: center; font-size: 13px;
  color: var(--color-text-muted, #a09890); margin-top: var(--space-lg, 24px);
}
.register-switch a { color: var(--color-accent, #c41e3a); font-weight: 500; }
</style>
