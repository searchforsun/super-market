import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE, TOKEN_KEY, REFRESH_TOKEN_KEY } from '@supermarket/utils'

const request = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
})

function extractMessage(data: any, fallback: string): string {
  if (!data) return fallback
  return data.message || data.msg || fallback
}

// 请求拦截器 — 注入 Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { code, data } = response.data
    if (code === 0) return data
    // Non-zero code in HTTP 200 response (legacy edge case)
    const msg = response.data.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  (error) => {
    const backendData = error.response?.data
    const backendMsg = extractMessage(backendData, '')

    // HTTP 409 — business rule conflict: backend already includes error details in body
    if (error.response?.status === 409) {
      ElMessage.error(backendMsg || '操作失败')
      return Promise.reject(new Error(backendMsg || '操作失败'))
    }

    // HTTP 401 — auth error (Token expired or invalid)
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
      const msg = backendMsg || '登录已过期，请重新登录'
      ElMessage.error(msg)
      window.location.href = '/login'
      return Promise.reject(error)
    }

    // HTTP 403 — permission denied
    if (error.response?.status === 403) {
      ElMessage.error(backendMsg || '权限不足')
      return Promise.reject(error)
    }

    // HTTP 400 — client error
    if (error.response?.status === 400) {
      ElMessage.error(backendMsg || '参数错误')
      return Promise.reject(new Error(backendMsg || '参数错误'))
    }

    // HTTP 404 — not found
    if (error.response?.status === 404) {
      ElMessage.error(backendMsg || '资源不存在')
      return Promise.reject(new Error(backendMsg || '资源不存在'))
    }

    // 500/503 — server error
    if (backendMsg) {
      ElMessage.error(backendMsg)
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请重试')
    } else if (!error.response) {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error('系统异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
