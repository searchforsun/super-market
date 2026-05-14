import request from '../request'

export function getCategoryTree() {
  return request.get('/category/tree')
}

export function getCategoryChildren(parentId: number) {
  return request.get(`/category/children/${parentId}`)
}
