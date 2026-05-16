import request from '../request'

export function getActiveBanners(position = 'HOME_TOP') {
  return request.get('/platform/banners', { params: { position } })
}

export function listBanners(page = 1, size = 20) {
  return request.get('/platform/admin/banners', { params: { page, size } })
}

export function createBanner(data: any) {
  return request.post('/platform/admin/banner', data)
}

export function updateBanner(id: number, data: any) {
  return request.put(`/platform/admin/banner/${id}`, data)
}

export function deleteBanner(id: number) {
  return request.delete(`/platform/admin/banner/${id}`)
}

export function getRiskRules(params: {
  page?: number
  size?: number
  name?: string
  type?: number
  status?: number
}) {
  return request.get('/platform/admin/risk/rules', { params })
}

export function createRiskRule(data: {
  name: string
  type: number
  threshold: number
  action: number
  status: number
  description?: string
}) {
  return request.post('/platform/admin/risk/rules', data)
}

export function updateRiskRule(data: {
  id: number
  name?: string
  type?: number
  threshold?: number
  action?: number
  status?: number
  description?: string
}) {
  return request.put('/platform/admin/risk/rules', data)
}

export function deleteRiskRule(id: number) {
  return request.delete(`/platform/admin/risk/rules/${id}`)
}

export function getRiskLogs(params: {
  page?: number
  size?: number
  userId?: string
  ruleType?: number
  riskLevel?: number
  startTime?: string
  endTime?: string
  handled?: number
}) {
  return request.get('/platform/admin/risk/logs', { params })
}

export function handleRiskLog(id: number) {
  return request.put(`/platform/admin/risk/logs/${id}/handle`)
}

export function getRiskLogDetail(id: number) {
  return request.get(`/platform/admin/risk/logs/${id}`)
}

export interface GmvDailyRecord {
  date: string
  gmv: number
  orders: number
  avgPrice: number
  uv: number
  conversionRate: number
}

export interface GmvReportData {
  totalGmv: number
  totalOrders: number
  avgOrderValue: number
  gmvGrowth: number
  daily: GmvDailyRecord[]
}

export interface OrderDailyRecord {
  date: string
  orders: number
  completed: number
  cancelled: number
  refunds: number
  completionRate: number
}

export interface OrderReportData {
  totalOrders: number
  completedOrders: number
  cancelledOrders: number
  refundRate: number
  daily: OrderDailyRecord[]
}

export interface UserDailyRecord {
  date: string
  newRegistrations: number
  logins: number
  orderingUsers: number
  newCustomerOrders: number
}

export interface UserReportData {
  newRegistrations: number
  totalUsers: number
  activeUsers: number
  dailyActive: number
  daily: UserDailyRecord[]
}

export function getGmvReport(startDate: string, endDate: string) {
  return request.get('/platform/admin/report/gmv', { params: { startDate, endDate } })
}

export function getOrderReport(startDate: string, endDate: string) {
  return request.get('/platform/admin/report/orders', { params: { startDate, endDate } })
}

export function getUserReport(startDate: string, endDate: string) {
  return request.get('/platform/admin/report/users', { params: { startDate, endDate } })
}
