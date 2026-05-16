import request from '../request'

export function authLogin(phone: string, password: string) {
  return request.post('/auth/login', { phone, password })
}

export function refreshToken(refreshToken: string) {
  return request.post('/auth/refresh', null, { params: { refreshToken } })
}
