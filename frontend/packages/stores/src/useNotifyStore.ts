import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getNotificationList, getUnreadCount, markRead, markAllRead } from '@supermarket/api'

export const useNotifyStore = defineStore('notify', () => {
  const notifications = ref<any[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const total = ref(0)

  async function fetchList(userId: number, page = 1, size = 20) {
    loading.value = true
    try {
      const res: any = await getNotificationList(userId, page, size)
      if (res?.records) {
        notifications.value = res.records
        total.value = res.total ?? 0
      } else if (Array.isArray(res)) {
        notifications.value = res
        total.value = res.length
      } else {
        notifications.value = []
        total.value = 0
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchUnreadCount(userId: number) {
    try {
      const res: any = await getUnreadCount(userId)
      unreadCount.value = typeof res === 'number' ? res : (res?.data ?? res ?? 0)
    } catch {
      unreadCount.value = 0
    }
  }

  async function markAsRead(id: number) {
    await markRead(id)
    const idx = notifications.value.findIndex(n => n.id === id)
    if (idx !== -1) notifications.value[idx].isRead = true
    if (unreadCount.value > 0) unreadCount.value--
  }

  async function markAllAsRead(userId: number) {
    await markAllRead(userId)
    notifications.value.forEach(n => (n.isRead = true))
    unreadCount.value = 0
  }

  return { notifications, unreadCount, loading, total, fetchList, fetchUnreadCount, markAsRead, markAllAsRead }
})
