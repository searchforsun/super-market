// API 基础路径
export const API_BASE = '/api'

// 默认分页
export const PAGE_SIZE = 20
export const PAGE_SIZE_MAX = 100

// Token 存储 Key
export const TOKEN_KEY = 'accessToken'
export const REFRESH_TOKEN_KEY = 'refreshToken'
export const USER_ID_KEY = 'userId'

// 订单状态
export const ORDER_STATUS: Record<number, string> = {
  1: '待付款',
  2: '待发货',
  3: '待收货',
  4: '已完成',
  5: '已取消',
  6: '已退款',
}

// 支付方式
export const PAY_METHODS: Record<number, string> = {
  1: '支付宝',
  2: '微信支付',
}

// 优惠券类型
export const COUPON_TYPES: Record<number, string> = {
  1: '满减券',
  2: '折扣券',
  3: '直减券',
}
