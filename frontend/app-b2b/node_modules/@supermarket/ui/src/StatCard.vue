<template>
  <div :class="['stat-card', trend]">
    <div class="stat-label">{{ label }}</div>
    <div class="stat-value">{{ value }}</div>
    <div v-if="trend && trendValue" class="stat-trend">{{ trend === 'up' ? '↑' : '↓' }} {{ trendValue }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ label: string; value: string | number; trend?: 'up' | 'down'; trendValue?: string }>()
</script>

<style scoped>
.stat-card { background: #fff; border-radius: 6px; padding: 16px 20px; border: 1px solid #eee; min-width: 160px; }
.stat-label { font-size: 12px; color: #999; margin-bottom: 6px; }
.stat-value { font-size: 24px; font-weight: 700; color: #333; }
.stat-trend { font-size: 11px; margin-top: 4px; }
.stat-card.up .stat-trend { color: #10b981; }
.stat-card.down .stat-trend { color: #ef4444; }
</style>
