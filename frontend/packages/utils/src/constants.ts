// API Base
export const API_BASE = '/api'

// Pagination
export const PAGE_SIZE = 20
export const PAGE_SIZE_MAX = 100

// Token Storage Keys (shared across all apps for SSO)
export const TOKEN_KEY = 'accessToken'
export const REFRESH_TOKEN_KEY = 'refreshToken'
export const USER_ID_KEY = 'userId'
export const ROLE_KEY = 'userRoles'

// Roles
export const ROLE_USER = 'ROLE_USER'
export const ROLE_MERCHANT = 'ROLE_MERCHANT'
export const ROLE_ADMIN = 'ROLE_ADMIN'

// App URLs (for SSO redirect)
export const APP_URLS: Record<string, string> = {
  ROLE_USER: 'http://localhost:5173',
  ROLE_MERCHANT: 'http://localhost:5174',
  ROLE_ADMIN: 'http://localhost:5175',
}

// Role to App mapping
export const ROLE_APPS: Record<string, string> = {
  ROLE_USER: 'B2C',
  ROLE_MERCHANT: 'B2B',
  ROLE_ADMIN: 'Admin',
}

// Order Status
export const ORDER_STATUS: Record<number, string> = {
  1: '待付款', 2: '待发货', 3: '待收货', 4: '已完成', 5: '已取消', 6: '已退款',
}

// Payment Methods
export const PAY_METHODS: Record<number, string> = { 1: '支付宝', 2: '微信支付' }

// Coupon Types
export const COUPON_TYPES: Record<number, string> = { 1: '满减券', 2: '折扣券', 3: '直减券' }
