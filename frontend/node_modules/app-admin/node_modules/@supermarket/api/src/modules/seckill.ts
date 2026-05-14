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
