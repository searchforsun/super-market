import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE, TOKEN_KEY, REFRESH_TOKEN_KEY } from '@supermarket/utils'

const request = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
})

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

// 响应拦截器 — 处理 401 / 统一错误提示
request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code === 200) return data
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
