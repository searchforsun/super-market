<template>
  <span :class="['status-tag', `status-${status}`]">{{ ORDER_STATUS[status] || '未知' }}</span>
</template>

<script setup lang="ts">
import { ORDER_STATUS } from '@supermarket/utils'
defineProps<{ status: number }>()
</script>

<style scoped>
.status-tag { font-size: 12px; padding: 2px 8px; border-radius: 3px; }
.status-1 { background: var(--status-pending-bg, #fff3cd); color: var(--status-pending-text, #856404); }
.status-2 { background: var(--status-processing-bg, #cce5ff); color: var(--status-processing-text, #004085); }
.status-3 { background: var(--status-completed-bg, #d4edda); color: var(--status-completed-text, #155724); }
.status-4 { background: var(--status-transit-bg, #d1ecf1); color: var(--status-transit-text, #0c5460); }
.status-5 { background: var(--status-neutral-bg, #e2e3e5); color: var(--status-neutral-text, #383d41); }
.status-6 { background: var(--status-danger-bg, #f8d7da); color: var(--status-danger-text, #721c24); }
</style>
