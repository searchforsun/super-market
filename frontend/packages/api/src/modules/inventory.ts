import request from '../request'

export interface InventoryRecord {
  spuId: number
  spuName: string
  spuImage: string
  skuId: number
  skuNo: string
  specName: string
  specCode: string
  price: number
  marketPrice: number
  totalStock: number
  availableStock: number
  lockedStock: number
  safetyStock: number
}

export interface InventoryListResult {
  records: InventoryRecord[]
  total: number
  page: number
  size: number
}

export function getInventoryList(params: Record<string, any>): Promise<InventoryListResult> {
  return request.get('/inventory/list', { params })
}

export function getSkuInventory(skuId: number): Promise<InventoryRecord> {
  return request.get('/inventory/sku/' + skuId)
}

export function initStock(skuId: number, totalStock: number, safetyStock?: number) {
  return request.post('/inventory/init', null, { params: { skuId, totalStock, safetyStock } })
}

export function deductStock(skuId: number, quantity: number) {
  return request.post('/inventory/deduct', null, { params: { skuId, quantity } })
}

export function updateStock(skuId: number, quantity: number, _reason = ''): Promise<boolean> {
  if (quantity > 0) {
    return request.post('/inventory/restock', null, { params: { skuId, quantity } })
  }
  return request.post('/inventory/deduct', null, { params: { skuId, quantity: Math.abs(quantity) } })
}
