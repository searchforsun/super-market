import request from '../request'

export function register(phone: string, password: string) {
  return request.post('/user/register', null, { params: { phone, password } })
}

export function login(phone: string, password: string) {
  return request.post('/user/login', null, { params: { phone, password } })
}

export function getUserInfo(userId: number) {
  return request.get('/user/info', { params: { userId } })
}
