import request from '../request'

export function createPayment(orderNo: string, userId: number, amount: number, payMethod = 1) {
  return request.post('/payment/pay', null, { params: { orderNo, userId, amount, payMethod } })
}

export function getPayment(payNo: string) {
  return request.get(`/payment/${payNo}`)
}
