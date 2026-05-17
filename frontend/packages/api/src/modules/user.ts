import request from '../request'

export function register(phone: string, password: string) {
  return request.post('/user/register', { phone, password })
}

export function login(phone: string, password: string) {
  return request.post('/user/login', { phone, password })
}

export function getUserInfo(userId: string) {
  return request.get('/user/info', { params: { userId } })
}
