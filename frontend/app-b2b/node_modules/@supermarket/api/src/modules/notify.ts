import request from '../request'

export function getNotificationList(userId: number, page = 1, size = 20) {
  return request.get('/notify/list', { params: { userId, page, size } })
}

export function getUnreadCount(userId: number) {
  return request.get('/notify/unread-count', { params: { userId } })
}

export function markRead(id: number) {
  return request.put(`/notify/${id}/read`)
}

export function markAllRead(userId: number) {
  return request.put('/notify/read-all', null, { params: { userId } })
}
