import request from '../request'

export function searchProducts(params: Record<string, any>) {
  return request.get('/search/product', { params })
}
