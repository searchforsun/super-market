import request from '../request'

export function getMerchantList(params: {
  page: number
  size: number
  auditStatus?: number
  startDate?: string
  endDate?: string
}) {
  return request.get('/shop/merchant/page', { params })
}

export function auditMerchant(id: number, auditStatus: number, reason?: string) {
  return request.put(`/shop/merchant/${id}/audit`, null, { params: { auditStatus, reason } })
}

export function getMerchantById(id: number) {
  return request.get(`/shop/merchant/${id}`)
}

export function getShopByMerchantId(merchantId: number) {
  return request.get(`/shop/merchant/${merchantId}`)
}

export function getShopById(id: number) {
  return request.get(`/shop/${id}`)
}
