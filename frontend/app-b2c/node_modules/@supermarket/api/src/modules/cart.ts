import request from '../request'

export function getCartList(userId: number) {
  return request.get('/cart/list', { params: { userId } })
}

export function addToCart(userId: number, item: Record<string, any>) {
  return request.post('/cart/add', item, { params: { userId } })
}

export function updateCartQuantity(userId: number, skuId: number, quantity: number) {
  return request.put(`/cart/item/${skuId}`, null, { params: { userId, quantity } })
}

export function removeCartItem(userId: number, skuId: number) {
  return request.delete(`/cart/item/${skuId}`, { params: { userId } })
}

export function selectCartItem(userId: number, skuId: number, selected: boolean) {
  return request.put(`/cart/item/${skuId}/select`, null, { params: { userId, selected } })
}

export function selectAllCart(userId: number, selected: boolean) {
  return request.put('/cart/select-all', null, { params: { userId, selected } })
}

export function getCartCount(userId: number) {
  return request.get('/cart/count', { params: { userId } })
}
