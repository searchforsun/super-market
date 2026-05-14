<template>
  <div class="login-page">
    <div class="login-box">
      <h2>Super Market 登录</h2>
      <el-input v-model="phone" placeholder="手机号" style="margin-bottom:12px" />
      <el-input v-model="password" type="password" placeholder="密码" show-password @keyup.enter="doLogin" />
      <el-button type="danger" style="width:100%;margin-top:16px" @click="doLogin" :loading="loading">登 录</el-button>
      <p class="login-tip">未注册的手机号将自动注册</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@supermarket/stores'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const phone = ref('')
const password = ref('')
const loading = ref(false)

async function doLogin() {
  if (!phone.value || !password.value) { ElMessage.warning('请输入手机号和密码'); return }
  loading.value = true
  try {
    await userStore.login(phone.value, password.value)
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    ElMessage.error('登录失败，请检查手机号或密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { display: flex; align-items: center; justify-content: center; min-height: 70vh; background: #f4f4f4; }
.login-box { background: #fff; padding: 32px; border-radius: 6px; width: 360px; box-shadow: 0 2px 12px rgba(0,0,0,.08); }
.login-box h2 { text-align: center; margin-bottom: 20px; }
.login-tip { font-size: 12px; color: #999; text-align: center; margin-top: 12px; }
</style>
