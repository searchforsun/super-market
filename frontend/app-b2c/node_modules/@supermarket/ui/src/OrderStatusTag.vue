<template>
  <span :class="['status-tag', `status-${status}`]">{{ ORDER_STATUS[status] || '未知' }}</span>
</template>

<script setup lang="ts">
import { ORDER_STATUS } from '@supermarket/utils'
defineProps<{ status: number }>()
</script>

<style scoped>
.status-tag { font-size: 12px; padding: 2px 8px; border-radius: 3px; }
.status-1 { background: #fff3cd; color: #856404; }
.status-2 { background: #cce5ff; color: #004085; }
.status-3 { background: #d4edda; color: #155724; }
.status-4 { background: #d1ecf1; color: #0c5460; }
.status-5 { background: #e2e3e5; color: #383d41; }
.status-6 { background: #f8d7da; color: #721c24; }
</style>
