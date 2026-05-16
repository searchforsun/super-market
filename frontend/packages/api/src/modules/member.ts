import request from '../request'

export function getMemberInfo(userId: number) {
  return request.get('/member/' + userId)
}

export function addPoints(userId: number, points: number) {
  return request.post('/member/points/add', null, { params: { userId, points } })
}
