import request from '../request'

export function createOrder(data: Record<string, any>) {
  return request.post('/order/create', data)
}

export function getOrderDetail(orderNo: string) {
  return request.get(`/order/${orderNo}`)
}

export function getOrderItems(orderNo: string) {
  return request.get(`/order/${orderNo}/items`)
}

export function getOrderList(userId: number, params: Record<string, any>) {
  return request.get(`/order/list/user/${userId}`, { params })
}

export function cancelOrder(orderNo: string, reason: string) {
  return request.put(`/order/${orderNo}/cancel`, null, { params: { reason } })
}

export function confirmReceive(orderNo: string) {
  return request.put(`/order/${orderNo}/receive`)
}

export function getMerchantOrderList(shopId: number, params: Record<string, any>) {
  return request.get(`/order/list/shop/${shopId}`, { params })
}

export function shipOrder(orderNo: string) {
  return request.put(`/order/${orderNo}/ship`)
}
