<template>
  <div class="notify-page page-enter">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">消息通知</h2>
        <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="unread-badge" />
      </div>
      <el-button
        v-if="notifications.some(n => !n.isRead && n.status !== 1)"
        size="small"
        @click="handleMarkAllRead"
        :loading="markingAll"
      >全部已读</el-button>
    </div>

    <!-- Notification List -->
    <div v-loading="loading">
      <div v-if="notifications.length" class="notify-list">
        <div
          v-for="n in notifications"
          :key="n.id"
          class="notify-card"
          :class="{ unread: !n.isRead && n.status !== 1 }"
          @click="handleRead(n)"
        >
          <div class="notify-indicator" v-if="!n.isRead && n.status !== 1"></div>
          <div class="notify-body">
            <div class="notify-title-row">
              <span class="notify-title">{{ n.title }}</span>
              <el-tag
                v-if="n.type || n.notifyType"
                size="small"
                type="info"
                effect="plain"
              >{{ n.type || n.notifyType }}</el-tag>
            </div>
            <p class="notify-content" v-if="n.content">{{ n.content }}</p>
            <span class="notify-time">{{ formatTime(n.createdAt || n.createTime) }}</span>
          </div>
          <div class="notify-action" v-if="n.link || n.targetUrl" @click.stop>
            <el-button size="small" text type="primary" @click="handleLink(n)">查看详情</el-button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!loading" class="empty-state">
        <el-empty description="暂无消息" :image-size="100" />
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="changePage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@supermarket/stores'
import {
  getNotificationList,
  markRead,
  markAllRead,
  getUnreadCount,
} from '@supermarket/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const notifications = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const unreadCount = ref(0)
const markingAll = ref(false)

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 7 * day) return Math.floor(diff / day) + '天前'

  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day_ = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day_}`
}

async function fetchNotifications() {
  loading.value = true
  try {
    const res: any = await getNotificationList(
      userStore.userId,
      currentPage.value,
      pageSize.value,
    )
    if (res?.records) {
      notifications.value = res.records.map((n: any) => ({
        ...n,
        isRead: n.status === 1 || n.isRead === true,
      }))
      total.value = res.total ?? 0
    } else if (Array.isArray(res)) {
      notifications.value = res.map((n: any) => ({
        ...n,
        isRead: n.status === 1 || n.isRead === true,
      }))
      total.value = res.length
    } else {
      notifications.value = []
      total.value = 0
    }
  } catch {
    notifications.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchUnreadCount() {
  try {
    const res: any = await getUnreadCount(userStore.userId)
    unreadCount.value = typeof res === 'number' ? res : (res?.data ?? res ?? 0)
  } catch {
    unreadCount.value = 0
  }
}

async function changePage(page: number) {
  currentPage.value = page
  await fetchNotifications()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function handleRead(notification: any) {
  if (notification.isRead || notification.status === 1) return

  try {
    await markRead(notification.id)
    notification.isRead = true
    notification.status = 1
    if (unreadCount.value > 0) unreadCount.value--
  } catch {
    // Ignore mark-read errors
  }
}

async function handleMarkAllRead() {
  markingAll.value = true
  try {
    await markAllRead(userStore.userId)
    notifications.value.forEach(n => {
      n.isRead = true
      n.status = 1
    })
    unreadCount.value = 0
    ElMessage.success('已全部标为已读')
  } catch {
    // Error handled by interceptor
  } finally {
    markingAll.value = false
  }
}

function handleLink(notification: any) {
  const url = notification.link || notification.targetUrl
  if (url) {
    if (url.startsWith('http')) {
      window.open(url, '_blank')
    } else {
      router.push(url)
    }
  }
}

onMounted(() => {
  fetchNotifications()
  fetchUnreadCount()
})
</script>

<style scoped>
.notify-page {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.unread-badge {
  margin-top: -3px;
}

/* Notification List */
.notify-list {
  display: flex;
  flex-direction: column;
}

.notify-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.notify-card:hover {
  background: #fafafa;
}

.notify-card.unread {
  background: #f0f7ff;
}

.notify-card.unread:hover {
  background: #e6f4ff;
}

.notify-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e1251b;
  margin-top: 6px;
  flex-shrink: 0;
}

.notify-body {
  flex: 1;
  min-width: 0;
}

.notify-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.notify-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.notify-content {
  font-size: 13px;
  color: #666;
  margin: 0 0 6px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notify-time {
  font-size: 11px;
  color: #bbb;
}

.notify-action {
  flex-shrink: 0;
  align-self: center;
}

/* Empty */
.empty-state {
  padding: 80px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 16px;
}
</style>
