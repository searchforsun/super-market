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

const mockProducts = [
  { spuId: 1001, spuName: '金龙鱼食用油 5L', spuImage: 'https://via.placeholder.com/80?text=Oil', skus: [
    { skuId: 100101, skuNo: 'SKU100101', specName: '5L/桶', specCode: '5L', price: 69.90, marketPrice: 89.90, totalStock: 500, availableStock: 328, lockedStock: 12, safetyStock: 50 },
    { skuId: 100102, skuNo: 'SKU100102', specName: '1.8L/桶', specCode: '1.8L', price: 29.90, marketPrice: 39.90, totalStock: 300, availableStock: 156, lockedStock: 8, safetyStock: 30 },
    { skuId: 100103, skuNo: 'SKU100103', specName: '4L/桶', specCode: '4L', price: 59.90, marketPrice: 79.90, totalStock: 200, availableStock: 3, lockedStock: 2, safetyStock: 20 },
  ]},
  { spuId: 1002, spuName: '福临门大米 10kg', spuImage: 'https://via.placeholder.com/80?text=Rice', skus: [
    { skuId: 100201, skuNo: 'SKU100201', specName: '10kg/袋', specCode: '10kg', price: 59.90, marketPrice: 69.90, totalStock: 800, availableStock: 623, lockedStock: 25, safetyStock: 100 },
    { skuId: 100202, skuNo: 'SKU100202', specName: '5kg/袋', specCode: '5kg', price: 32.90, marketPrice: 39.90, totalStock: 400, availableStock: 18, lockedStock: 5, safetyStock: 50 },
  ]},
  { spuId: 1003, spuName: '海天酱油 500ml', spuImage: 'https://via.placeholder.com/80?text=Sauce', skus: [
    { skuId: 100301, skuNo: 'SKU100301', specName: '500ml/瓶', specCode: '500ml', price: 9.90, marketPrice: 12.90, totalStock: 1200, availableStock: 876, lockedStock: 34, safetyStock: 200 },
    { skuId: 100302, skuNo: 'SKU100302', specName: '1L/瓶', specCode: '1L', price: 16.90, marketPrice: 19.90, totalStock: 600, availableStock: 0, lockedStock: 0, safetyStock: 100 },
  ]},
  { spuId: 1004, spuName: '蒙牛纯牛奶 250ml', spuImage: 'https://via.placeholder.com/80?text=Milk', skus: [
    { skuId: 100401, skuNo: 'SKU100401', specName: '250ml/盒', specCode: '250ml', price: 3.50, marketPrice: 4.50, totalStock: 3000, availableStock: 2145, lockedStock: 86, safetyStock: 500 },
    { skuId: 100402, skuNo: 'SKU100402', specName: '1L/盒', specCode: '1L', price: 12.80, marketPrice: 15.80, totalStock: 800, availableStock: 5, lockedStock: 2, safetyStock: 50 },
  ]},
  { spuId: 1005, spuName: '农夫山泉矿泉水 550ml', spuImage: 'https://via.placeholder.com/80?text=Water', skus: [
    { skuId: 100501, skuNo: 'SKU100501', specName: '550ml/瓶', specCode: '550ml', price: 1.50, marketPrice: 2.00, totalStock: 5000, availableStock: 3876, lockedStock: 120, safetyStock: 1000 },
    { skuId: 100502, skuNo: 'SKU100502', specName: '4L/桶', specCode: '4L', price: 8.00, marketPrice: 10.00, totalStock: 400, availableStock: 267, lockedStock: 15, safetyStock: 80 },
  ]},
  { spuId: 1006, spuName: '奥利奥饼干 原味', spuImage: 'https://via.placeholder.com/80?text=Cookie', skus: [
    { skuId: 100601, skuNo: 'SKU100601', specName: '97g/袋', specCode: '97g', price: 9.90, marketPrice: 12.90, totalStock: 600, availableStock: 423, lockedStock: 18, safetyStock: 80 },
    { skuId: 100602, skuNo: 'SKU100602', specName: '388g/盒', specCode: '388g', price: 29.80, marketPrice: 36.80, totalStock: 300, availableStock: 0, lockedStock: 0, safetyStock: 30 },
  ]},
  { spuId: 1007, spuName: '云南白药牙膏', spuImage: 'https://via.placeholder.com/80?text=Toothpaste', skus: [
    { skuId: 100701, skuNo: 'SKU100701', specName: '120g/支', specCode: '120g', price: 19.90, marketPrice: 25.90, totalStock: 900, availableStock: 567, lockedStock: 22, safetyStock: 100 },
    { skuId: 100702, skuNo: 'SKU100702', specName: '180g/支', specCode: '180g', price: 26.90, marketPrice: 32.90, totalStock: 500, availableStock: 2, lockedStock: 1, safetyStock: 50 },
  ]},
  { spuId: 1008, spuName: '心相印抽纸', spuImage: 'https://via.placeholder.com/80?text=Tissue', skus: [
    { skuId: 100801, skuNo: 'SKU100801', specName: '3层/100抽/包', specCode: '100抽', price: 3.50, marketPrice: 4.50, totalStock: 2000, availableStock: 1456, lockedStock: 54, safetyStock: 300 },
    { skuId: 100802, skuNo: 'SKU100802', specName: '3层/200抽/包', specCode: '200抽', price: 5.80, marketPrice: 7.50, totalStock: 1500, availableStock: 987, lockedStock: 32, safetyStock: 200 },
    { skuId: 100803, skuNo: 'SKU100803', specName: '整箱24包', specCode: '24包', price: 79.00, marketPrice: 99.00, totalStock: 200, availableStock: 11, lockedStock: 3, safetyStock: 30 },
  ]},
  { spuId: 1009, spuName: '可口可乐 330ml', spuImage: 'https://via.placeholder.com/80?text=Cola', skus: [
    { skuId: 100901, skuNo: 'SKU100901', specName: '330ml/罐', specCode: '330ml', price: 2.50, marketPrice: 3.00, totalStock: 4000, availableStock: 3245, lockedStock: 98, safetyStock: 600 },
    { skuId: 100902, skuNo: 'SKU100902', specName: '1.25L/瓶', specCode: '1.25L', price: 5.00, marketPrice: 6.50, totalStock: 800, availableStock: 534, lockedStock: 21, safetyStock: 100 },
    { skuId: 100903, skuNo: 'SKU100903', specName: '2L/瓶', specCode: '2L', price: 7.00, marketPrice: 8.50, totalStock: 600, availableStock: 8, lockedStock: 2, safetyStock: 60 },
  ]},
  { spuId: 1010, spuName: '统一方便面 红烧牛肉', spuImage: 'https://via.placeholder.com/80?text=Noodle', skus: [
    { skuId: 101001, skuNo: 'SKU101001', specName: '袋装/5包', specCode: '5包', price: 12.50, marketPrice: 15.00, totalStock: 1000, availableStock: 678, lockedStock: 30, safetyStock: 150 },
    { skuId: 101002, skuNo: 'SKU101002', specName: '桶装/12桶', specCode: '12桶', price: 48.00, marketPrice: 60.00, totalStock: 500, availableStock: 0, lockedStock: 0, safetyStock: 80 },
  ]},
]

function paginate<T>(items: T[], page: number, size: number) {
  const start = (page - 1) * size
  return {
    records: items.slice(start, start + size),
    total: items.length,
    page,
    size,
  }
}

export async function getInventoryList(params: Record<string, any>): Promise<InventoryListResult> {
  const page = params.page || 1
  const size = params.size || 20
  const keyword = (params.keyword || '').toLowerCase()

  let allRecords: InventoryRecord[] = []
  for (const product of mockProducts) {
    for (const sku of product.skus) {
      if (keyword) {
        if (!product.spuName.toLowerCase().includes(keyword) && !sku.skuNo.toLowerCase().includes(keyword)) {
          continue
        }
      }
      allRecords.push({
        spuId: product.spuId,
        spuName: product.spuName,
        spuImage: product.spuImage,
        skuId: sku.skuId,
        skuNo: sku.skuNo,
        specName: sku.specName,
        specCode: sku.specCode,
        price: sku.price,
        marketPrice: sku.marketPrice,
        totalStock: sku.totalStock,
        availableStock: sku.availableStock,
        lockedStock: sku.lockedStock,
        safetyStock: sku.safetyStock,
      })
    }
  }

  return paginate(allRecords, page, size)
}

export async function getSkuInventory(skuId: number): Promise<InventoryRecord | null> {
  for (const product of mockProducts) {
    for (const sku of product.skus) {
      if (sku.skuId === skuId) {
        return {
          spuId: product.spuId,
          spuName: product.spuName,
          spuImage: product.spuImage,
          skuId: sku.skuId,
          skuNo: sku.skuNo,
          specName: sku.specName,
          specCode: sku.specCode,
          price: sku.price,
          marketPrice: sku.marketPrice,
          totalStock: sku.totalStock,
          availableStock: sku.availableStock,
          lockedStock: sku.lockedStock,
          safetyStock: sku.safetyStock,
        }
      }
    }
  }
  try {
    const data: any = await request.get(`/inventory/sku/${skuId}`)
    return data as InventoryRecord
  } catch {
    return null
  }
}

export async function updateStock(skuId: number, quantity: number, reason: string): Promise<boolean> {
  for (const product of mockProducts) {
    for (const sku of product.skus) {
      if (sku.skuId === skuId) {
        sku.availableStock += quantity
        if (quantity > 0) {
          sku.totalStock += quantity
        }
        return true
      }
    }
  }
  try {
    const data: any = await request.post('/inventory/deduct', null, {
      params: { skuId, quantity: Math.abs(quantity) },
    })
    return data === true || data === 'true'
  } catch {
    return false
  }
}
