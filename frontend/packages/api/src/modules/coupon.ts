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

export function getCouponTemplates(page = 1, size = 20) {
  return request.get('/coupon/admin/templates', { params: { page, size } })
}

export function createCouponTemplate(data: any) {
  return request.post('/coupon/admin/template', data)
}

export function updateCouponTemplate(data: any) {
  return request.put('/coupon/admin/template', data)
}

export function setCouponTemplateStatus(id: number, status: number) {
  return request.put('/coupon/admin/template/status', null, { params: { id, status } })
}

export function deleteCouponTemplate(id: number) {
  return request.delete(`/coupon/admin/template/${id}`)
}

export function distributeCoupons(templateId: number, userIds: number[]) {
  return request.post('/coupon/admin/distribute', userIds, { params: { templateId } })
}

export function getDistributeRecords(page = 1, size = 20) {
  return request.get('/coupon/admin/batches', { params: { page, size } })
}
