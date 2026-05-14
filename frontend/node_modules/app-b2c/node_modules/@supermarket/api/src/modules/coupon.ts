import request from '../request'

export function getAvailableCoupons(page = 1, size = 20) {
  return request.get('/coupon/available', { params: { page, size } })
}

export function claimCoupon(userId: number, templateId: number) {
  return request.post('/coupon/claim', null, { params: { userId, templateId } })
}

export function getUserCoupons(userId: number, status?: number) {
  return request.get('/coupon/my', { params: { userId, status } })
}

export function getAvailableList(userId: number) {
  return request.get('/coupon/available/list', { params: { userId } })
}
