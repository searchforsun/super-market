import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getOrderList, getOrderDetail, cancelOrder, confirmReceive } from '@supermarket/api'

export const useOrderStore = defineStore('order', () => {
  const orders = ref<any[]>([])
  const currentOrder = ref<any | null>(null)
  const loading = ref(false)
  const total = ref(0)

  async function fetchList(userId: number, params: Record<string, any> = {}) {
    loading.value = true
    try {
      const res: any = await getOrderList(userId, params)
      if (res?.records) {
        orders.value = res.records
        total.value = res.total ?? 0
      } else if (Array.isArray(res)) {
        orders.value = res
        total.value = res.length
      } else {
        orders.value = []
        total.value = 0
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(orderNo: string) {
    const res: any = await getOrderDetail(orderNo)
    currentOrder.value = res
    return res
  }

  async function cancel(orderNo: string, reason: string) {
    await cancelOrder(orderNo, reason)
    if (currentOrder.value?.orderNo === orderNo) {
      currentOrder.value.status = -1
    }
  }

  async function confirm(orderNo: string) {
    await confirmReceive(orderNo)
    if (currentOrder.value?.orderNo === orderNo) {
      currentOrder.value.status = 4
    }
  }

  return { orders, currentOrder, loading, total, fetchList, fetchDetail, cancel, confirm }
})
