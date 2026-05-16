<template>
  <div class="register-page">
    <div class="register-card">
      <router-link to="/" class="register-logo">
        <span class="logo-mark">S</span>
        <span class="logo-text">Super Market</span>
      </router-link>
      <h2 class="register-heading">创建账号</h2>
      <p class="register-sub">注册开始你的品质购物之旅</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="doRegister">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" native-type="submit" :loading="loading" style="width:100%;height:44px;border-radius:8px;font-weight:500">
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
import { Phone, Lock, User } from '@element-plus/icons-vue'
import { register } from '@supermarket/api'
import { useUserStore } from '@supermarket/stores'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref()
const form = reactive({
  phone: '', nickname: '', password: '', confirmPassword: '',
})

const validateConfirm = (_rule: any, value: string, callback: any) => {
  callback(value !== form.password ? new Error('两次密码输入不一致') : undefined)
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
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
    await register(form.phone, form.password)
    ElMessage.success('注册成功')
    await userStore.login(form.phone, form.password)
    router.push('/')
  } catch {
    ElMessage.error('注册失败，该手机号可能已被注册')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 80vh;
  display: flex; align-items: center; justify-content: center;
  background: var(--color-bg, #faf8f5);
}
.register-card {
  background: var(--color-surface, #fff);
  padding: 48px 40px;
  border-radius: var(--radius-xl, 16px);
  width: 420px;
  box-shadow: var(--shadow-lg, 0 12px 40px rgba(26,24,22,0.08));
}
.register-logo {
  display: flex; align-items: center; justify-content: center;
  gap: 10px; margin-bottom: var(--space-xl, 32px);
  text-decoration: none;
}
.logo-mark {
  width: 40px; height: 40px;
  background: var(--color-accent, #c41e3a);
  color: #fff;
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
  text-align: center;
  font-family: var(--font-display, 'Playfair Display', serif);
  font-size: 24px; font-weight: 600;
  margin-bottom: var(--space-sm, 8px);
}
.register-sub {
  text-align: center; font-size: 14px;
  color: var(--color-text-muted, #a09890);
  margin-bottom: var(--space-xl, 32px);
}
.register-switch {
  text-align: center; font-size: 13px;
  color: var(--color-text-muted, #a09890);
  margin-top: var(--space-lg, 24px);
}
.register-switch a {
  color: var(--color-accent, #c41e3a);
  font-weight: 500; text-decoration: none;
}
</style>
