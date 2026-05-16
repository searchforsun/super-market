<template>
  <div class="login-page">
    <div class="login-card">
      <router-link to="/admin" class="login-logo">
        <span class="logo-mark">S</span>
        <span class="logo-text">Super Market</span>
      </router-link>
      <h2 class="login-heading">运营管理平台</h2>
      <p class="login-sub">管理员登录</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleLogin">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="管理员手机号" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading"
            style="width:100%;height:44px;border-radius:8px;font-weight:500;background:var(--color-text-primary);border:none;">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <p class="login-note">管理员账号由系统分配，无法自助注册</p>
      <p v-if="error" class="login-error">{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Phone, Lock } from '@element-plus/icons-vue'
import { authLogin } from '@supermarket/api'
import { saveAuth, hasRole, ROLE_ADMIN } from '@supermarket/utils'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const formRef = ref()
const form = reactive({ phone: '', password: '' })
const rules = {
  phone: [{ required: true, message: '请输入管理员手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true; error.value = ''
  try {
    const data = await authLogin(form.phone, form.password)
    saveAuth(data)
    if (!hasRole(ROLE_ADMIN)) {
      error.value = '该账号没有管理员权限'
      return
    }
    ElMessage.success('登录成功')
    router.push(String(route.query.redirect || '/admin'))
  } catch (e: any) {
    error.value = e?.response?.data?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #8b5cf6 0%, #5b21b6 100%);
}
.login-card {
  background: var(--color-surface, #fff);
  padding: 48px 40px; border-radius: var(--radius-xl, 16px);
  width: 420px; box-shadow: var(--shadow-xl, 0 24px 64px rgba(0,0,0,0.2));
}
.login-logo {
  display: flex; align-items: center; justify-content: center;
  gap: 10px; margin-bottom: var(--space-xl, 32px); text-decoration: none;
}
.logo-mark {
  width: 40px; height: 40px;
  background: #7c3aed; color: #fff;
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
.login-heading {
  text-align: center; font-family: var(--font-display, 'Playfair Display', serif);
  font-size: 24px; font-weight: 600; margin-bottom: var(--space-sm, 8px);
}
.login-sub {
  text-align: center; font-size: 14px;
  color: var(--color-text-muted, #a09890);
  margin-bottom: var(--space-xl, 32px);
}
.login-note {
  text-align: center; font-size: 12px;
  color: var(--color-text-muted, #a09890);
  margin-top: var(--space-lg, 24px);
}
.login-error {
  text-align: center; font-size: 13px; color: var(--color-accent, #c41e3a);
  margin-top: var(--space-md, 16px);
}
</style>
