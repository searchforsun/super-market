import request from '../request'

export interface DashboardStats {
  todayGmv: number
  todayOrderCount: number
  newUsers: number
  pendingMerchants: number
}

export interface DailyTrend {
  date: string
  gmv: number
  orderCount: number
}

export interface CategorySales {
  categoryName: string
  amount: number
  percentage: number
}

export interface StatusDistItem {
  name: string
  status: number
  value: number
}

export interface UserTrendItem {
  date: string
  count: number
}

export interface RecentOrder {
  orderNo: string
  userName: string
  actualAmount: number
  status: string
  createTime: string
}

export interface PendingAuditResult {
  pendingMerchants: number
  pendingProducts: number
}

export interface MerchantStats {
  todayOrders: number
  todayGmv: number
  pendingShip: number
  pendingRefund: number
  productCount?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// Admin dashboard
export function getDashboardStats(): Promise<DashboardStats> {
  return request.get('/platform/admin/dashboard/stats') as Promise<DashboardStats>
}

export function getDailyTrend(days = 7): Promise<DailyTrend[]> {
  return request.get('/platform/admin/dashboard/trend', { params: { days } }) as Promise<DailyTrend[]>
}

export function getCategorySales(): Promise<CategorySales[]> {
  return request.get('/platform/admin/dashboard/category-sales') as Promise<CategorySales[]>
}

export function getStatusDistribution(): Promise<StatusDistItem[]> {
  return request.get('/platform/admin/dashboard/status-distribution') as Promise<StatusDistItem[]>
}

export function getUserTrend(days = 7): Promise<UserTrendItem[]> {
  return request.get('/platform/admin/dashboard/user-trend', { params: { days } }) as Promise<UserTrendItem[]>
}

export function getAdminOrders(params: {
  page?: number
  size?: number
  status?: string
}): Promise<PageResult<RecentOrder>> {
  return request.get('/order/admin/list', { params }) as Promise<PageResult<RecentOrder>>
}

export function getPendingAudit(): Promise<PendingAuditResult> {
  return request.get('/platform/admin/dashboard/pending-audit') as Promise<PendingAuditResult>
}

// Merchant dashboard
export function getMerchantStats(shopId: number): Promise<MerchantStats> {
  return request.get('/platform/merchant/dashboard/stats', { params: { shopId } }) as Promise<MerchantStats>
}

export function getMerchantTrend(shopId: number, days = 7): Promise<DailyTrend[]> {
  return request.get('/platform/merchant/dashboard/trend', { params: { shopId, days } }) as Promise<DailyTrend[]>
}

export function getMerchantStatusDistribution(shopId: number): Promise<StatusDistItem[]> {
  return request.get('/platform/merchant/dashboard/status-distribution', { params: { shopId } }) as Promise<StatusDistItem[]>
}
