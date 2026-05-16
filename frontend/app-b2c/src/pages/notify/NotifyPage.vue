<template>
  <div class="notify-page page-enter"><h2>消息通知</h2>
    <el-button @click="markAll">全部已读</el-button>
    <div v-for="n in notifications" :key="n.id" class="notify-item" :class="{ unread: n.status === 0 }">
      <strong>{{ n.title }}</strong><p>{{ n.content }}</p><span class="date">{{ n.createdAt }}</span>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'; import { useUserStore } from '@supermarket/stores'
import { getNotificationList, markAllRead } from '@supermarket/api'
const userStore = useUserStore(); const notifications = ref<any[]>([])
onMounted(async () => { const res: any = await getNotificationList(userStore.userId); notifications.value = res.records || [] })
async function markAll() { await markAllRead(userStore.userId); notifications.value.forEach(n => n.status = 1) }
</script>
<style scoped>
.notify-page{background:#fff;padding:20px;border-radius:4px}
.notify-item{padding:10px 0;border-bottom:1px solid #eee}.notify-item.unread{background:#f0f7ff}.date{font-size:11px;color:#999}
</style>
