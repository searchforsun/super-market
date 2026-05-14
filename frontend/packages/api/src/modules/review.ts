import request from '../request'

export function createReview(data: Record<string, any>) {
  return request.post('/review', data)
}

export function getReviewsBySpu(spuId: number, params: Record<string, any>) {
  return request.get(`/review/list/spu/${spuId}`, { params })
}

export function getReviewsByUser(userId: number, page = 1, size = 10) {
  return request.get(`/review/list/user/${userId}`, { params: { page, size } })
}

export function getRatingSummary(spuId: number) {
  return request.get(`/review/rating/${spuId}`)
}

export function getReviewPage(params: Record<string, any>) {
  return request.get('/review/page', { params })
}

export function replyReview(id: number, content: string) {
  return request.put(`/review/${id}/reply`, null, { params: { content } })
}
