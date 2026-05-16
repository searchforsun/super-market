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

// 注：后端暂无 /order/list/shop/{shopId} 端点，暂用用户订单列表代替
export function getMerchantOrderList(userId: number, params: Record<string, any>) {
  return request.get(`/order/list/user/${userId}`, { params })
}

export function shipOrder(orderNo: string) {
  return request.put(`/order/${orderNo}/ship`)
}
