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

export function getNotifyTemplates(page = 1, size = 20) {
  return request.get('/notify/admin/templates', { params: { page, size } })
}

export function createNotifyTemplate(data: any) {
  return request.post('/notify/admin/template', data)
}

export function updateNotifyTemplate(data: any) {
  return request.put('/notify/admin/template', data)
}

export function deleteNotifyTemplate(id: number) {
  return request.delete(`/notify/admin/template/${id}`)
}

export function setNotifyTemplateStatus(id: number, status: number) {
  return request.put('/notify/admin/template/status', null, { params: { id, status } })
}

export function testNotifyTemplate(data: any) {
  return request.post('/notify/admin/template/test', data)
}
