<template>
  <div class="b2b-notifications page-enter">
    <div class="page-header">
      <h2>通知中心</h2>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" type="danger">
        <el-button size="small" @click="handleMarkAllRead" :loading="markingAll" :disabled="unreadCount === 0">
          全部已读
        </el-button>
      </el-badge>
    </div>

    <el-card shadow="never" class="notify-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部通知" name="all" />
        <el-tab-pane label="系统通知" name="system" />
        <el-tab-pane label="订单通知" name="order" />
        <el-tab-pane label="促销通知" name="promotion" />
      </el-tabs>

      <div class="notify-list" v-loading="loading">
        <div v-if="list.length === 0 && !loading" class="empty-wrapper">
          <el-empty description="暂无通知" />
        </div>

        <div v-for="item in list" :key="item.id" class="notify-item" :class="{ unread: item.status === 0 }">
          <div class="notify-item__main">
            <div class="notify-item__header">
              <span class="notify-item__title" :class="{ 'is-bold': item.status === 0 }">{{ item.title }}</span>
              <div class="notify-item__meta">
                <el-tag
                  :type="tagType(item.type)"
                  size="small"
                  effect="plain"
                >
                  {{ tagLabel(item.type) }}
                </el-tag>
                <span class="notify-item__type">{{ item.createdAt || item.createTime }}</span>
                <el-button
                  v-if="item.status === 0"
                  text
                  size="small"
                  type="primary"
                  @click="handleMarkRead(item)"
                  :loading="readingIds.includes(item.id)"
                >
                  标记已读
                </el-button>
              </div>
            </div>
            <p class="notify-item__content">{{ item.content }}</p>
          </div>
        </div>
      </div>

      <div class="notify-pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getNotificationList, getUnreadCount, markRead, markAllRead } from '@supermarket/api'
import { USER_ID_KEY } from '@supermarket/utils'
import { ElMessage } from 'element-plus'

const userId = Number(localStorage.getItem(USER_ID_KEY)) || 0

const activeTab = ref('all')
const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const unreadCount = ref(0)
const markingAll = ref(false)
const readingIds = ref<number[]>([])

function tagType(type?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (type === 'system') return 'info'
  if (type === 'order') return 'success'
  if (type === 'promotion') return 'warning'
  return 'info'
}

function tagLabel(type?: string): string {
  if (type === 'system') return '系统'
  if (type === 'order') return '订单'
  if (type === 'promotion') return '促销'
  return '通知'
}

async function fetchList() {
  if (!userId) return
  loading.value = true
  try {
    const typeMap: Record<string, string | undefined> = {
      all: undefined,
      system: 'system',
      order: 'order',
      promotion: 'promotion',
    }
    const params: any = { page: page.value, size: size.value }
    const typeVal = typeMap[activeTab.value]
    if (typeVal) params.type = typeVal
    const res: any = await getNotificationList(userId, page.value, size.value)
    list.value = res.records || res.list || []
    total.value = res.total || 0
  } catch {
    ElMessage.error('加载通知列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchUnreadCount() {
  if (!userId) return
  try {
    const res: any = await getUnreadCount(userId)
    unreadCount.value = res.count || res.data || 0
  } catch {}
}

function handleTabChange() {
  page.value = 1
  fetchList()
}

function handlePageChange(val: number) {
  page.value = val
  fetchList()
}

async function handleMarkRead(item: any) {
  readingIds.value.push(item.id)
  try {
    await markRead(item.id)
    item.status = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch {
    ElMessage.error('标记已读失败')
  } finally {
    readingIds.value = readingIds.value.filter(id => id !== item.id)
  }
}

async function handleMarkAllRead() {
  markingAll.value = true
  try {
    await markAllRead(userId)
    list.value.forEach(item => { item.status = 1 })
    unreadCount.value = 0
    ElMessage.success('已全部标记已读')
  } catch {
    ElMessage.error('全部标记已读失败')
  } finally {
    markingAll.value = false
  }
}

onMounted(() => {
  fetchUnreadCount()
  fetchList()
})
</script>

<style scoped>
.b2b-notifications {
  background: var(--color-bg, #f5f7fa);
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary, #303133);
  margin: 0;
}

.notify-card {
  border-radius: 8px;
}

.notify-card :deep(.el-tabs__header) {
  margin-bottom: 0;
  padding: 0 20px;
  border-bottom: 1px solid #f0f0f0;
}

.notify-card :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.notify-list {
  min-height: 200px;
}

.empty-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80px 0;
}

.notify-item {
  display: flex;
  align-items: flex-start;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border, #f0f0f0);
  transition: background-color 0.2s;
}

.notify-item:hover {
  background-color: var(--color-surface-hover, #fafafa);
}

.notify-item.unread {
  background-color: var(--color-accent-light, #f0f7ff);
}

.notify-item.unread:hover {
  background-color: var(--color-accent-soft, #e6f2ff);
}

.notify-item__main {
  flex: 1;
  min-width: 0;
}

.notify-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.notify-item__title {
  font-size: 14px;
  color: var(--color-text-primary, #303133);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notify-item__title.is-bold {
  font-weight: 600;
  color: var(--color-text-primary, #1a1a2e);
}

.notify-item__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.notify-item__type {
  font-size: 12px;
  color: var(--color-text-muted, #999);
  white-space: nowrap;
}

.notify-item__content {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary, #606266);
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notify-pagination {
  display: flex;
  justify-content: center;
  padding: 20px;
}
</style>
