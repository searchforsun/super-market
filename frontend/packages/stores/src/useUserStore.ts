import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authLogin, getUserInfo } from '@supermarket/api'
import { saveAuth, clearAuth, getRoles, isAuthenticated, getPrimaryRole, redirectByRole } from '@supermarket/utils'

export const useUserStore = defineStore('user', () => {
  const token = ref(isAuthenticated())
  const userId = ref(localStorage.getItem('userId') || '')
  const userInfo = ref<any>(null)
  const roles = ref<string[]>(getRoles())

  const isLoggedIn = computed(() => token.value)
  const primaryRole = computed(() => getPrimaryRole())

  async function login(phone: string, password: string) {
    const res: any = await authLogin(phone, password)
    saveAuth(res)
    token.value = true
    userId.value = res.userId
    roles.value = getRoles()
    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    if (!userId.value) return
    try { userInfo.value = await getUserInfo(userId.value) } catch {}
  }

  function logout() {
    clearAuth()
    token.value = false
    userId.value = ''
    userInfo.value = null
    roles.value = []
  }

  function goToApp() {
    redirectByRole()
  }

  return { token, userId, userInfo, roles, isLoggedIn, primaryRole, login, fetchUserInfo, logout, goToApp }
})
