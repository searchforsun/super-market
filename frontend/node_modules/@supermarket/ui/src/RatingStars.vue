<template>
  <span class="rating-stars">
    <span v-for="i in 5" :key="i" :class="['star', i <= rating ? 'filled' : '']" :style="{ fontSize: size + 'px' }">★</span>
  </span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ rating: number; size?: number }>(), { size: 14 })
</script>

<style scoped>
.rating-stars { display: inline-flex; gap: 1px; }
.star { color: #ddd; line-height: 1; }
.star.filled { color: #f5a623; }
</style>
