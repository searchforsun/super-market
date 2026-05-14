import request from '../request'

export function getSeckillSessions() {
  return request.get('/seckill/sessions')
}

export function getSeckillProducts(sessionId: number) {
  return request.get('/seckill/products', { params: { sessionId } })
}

export function executeSeckill(userId: number, seckillProductId: number, quantity = 1) {
  return request.post('/seckill/execute', null, { params: { userId, seckillProductId, quantity } })
}

export function createSeckillSession(data: {
  name: string
  startTime: string
  endTime: string
}) {
  return request.post('/seckill/session', data)
}

export function updateSeckillSession(id: number, data: {
  name?: string
  startTime?: string
  endTime?: string
}) {
  return request.put(`/seckill/session/${id}`, data)
}

export function deleteSeckillSession(id: number) {
  return request.delete(`/seckill/session/${id}`)
}

export function addSeckillProduct(sessionId: number, data: {
  productId: number
  seckillPrice: number
  stock: number
}) {
  return request.post('/seckill/product', data, { params: { sessionId } })
}

export function removeSeckillProduct(id: number) {
  return request.delete(`/seckill/product/${id}`)
}
