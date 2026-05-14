import request from '../request'

export function getAddressList(userId: number) {
  return request.get('/address/list', { params: { userId } })
}

export function createAddress(data: Record<string, any>) {
  return request.post('/address', data)
}

export function updateAddress(data: Record<string, any>) {
  return request.put('/address', data)
}

export function deleteAddress(id: number, userId: number) {
  return request.delete(`/address/${id}`, { params: { userId } })
}

export function setDefaultAddress(id: number, userId: number) {
  return request.put(`/address/${id}/default`, null, { params: { userId } })
}
