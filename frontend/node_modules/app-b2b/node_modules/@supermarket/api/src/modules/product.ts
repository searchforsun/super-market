import request from '../request'

export function getProductList(params: Record<string, any>) {
  return request.get('/product/list', { params })
}

export function getProductDetail(spuId: number) {
  return request.get(`/product/spu/${spuId}`)
}

export function getProductSkus(spuId: number) {
  return request.get(`/product/spu/${spuId}/skus`)
}
