import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { TOKEN_KEY, REFRESH_TOKEN_KEY, USER_ID_KEY } from '@supermarket/utils'
import { authLogin, getUserInfo } from '@supermarket/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userId = ref(Number(localStorage.getItem(USER_ID_KEY)) || 0)
  const userInfo = ref<any>(null)

  const isLoggedIn = computed(() => !!token.value)

  async function login(phone: string, password: string) {
    const res: any = await authLogin(phone, password)
    token.value = res.accessToken
    userId.value = Number(res.userId)
    localStorage.setItem(TOKEN_KEY, res.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken)
    localStorage.setItem(USER_ID_KEY, String(res.userId))
    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    if (!userId.value) return
    try {
      userInfo.value = await getUserInfo(userId.value)
    } catch { /* ignore */ }
  }

  function logout() {
    token.value = ''
    userId.value = 0
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_ID_KEY)
  }

  return { token, userId, userInfo, isLoggedIn, login, fetchUserInfo, logout }
})
