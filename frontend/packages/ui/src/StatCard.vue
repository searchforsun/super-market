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
.stat-card {
  background: var(--color-surface, #fff);
  border-radius: var(--radius-md, 8px);
  padding: 16px 20px;
  border: 1px solid var(--color-border, #eee);
  min-width: 160px;
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 3px; height: 100%;
  background: var(--color-accent, #d97706);
  border-radius: 3px 0 0 3px;
}
.stat-label { font-size: 12px; color: var(--color-text-muted, #999); margin-bottom: 6px; }
.stat-value { font-size: 24px; font-weight: 700; color: var(--color-text-primary, #333); }
.stat-trend { font-size: 11px; margin-top: 4px; }
.stat-card.up .stat-trend { color: #10b981; }
.stat-card.down .stat-trend { color: #ef4444; }
</style>
