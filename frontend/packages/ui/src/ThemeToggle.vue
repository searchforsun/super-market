<template>
  <button class="theme-toggle" :title="isDark ? '切换浅色模式' : '切换深色模式'" @click="toggle">
    <svg v-if="isDark" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
      <circle cx="12" cy="12" r="5"/>
      <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
    </svg>
    <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
    </svg>
  </button>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const KEY = 'theme'
const isDark = ref(false)

function apply(t: 'light' | 'dark') {
  if (t === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark')
    isDark.value = true
  } else {
    document.documentElement.removeAttribute('data-theme')
    isDark.value = false
  }
}

function toggle() {
  const next = isDark.value ? 'light' : 'dark'
  localStorage.setItem(KEY, next)
  apply(next)
}

onMounted(() => {
  const saved = localStorage.getItem(KEY)
  apply(saved === 'dark' ? 'dark' : 'light')
})
</script>

<style scoped>
.theme-toggle {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-secondary, #5c5a55);
  background: none; border: none;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.theme-toggle:hover {
  color: var(--color-text-primary, #1a1a18);
  background: var(--color-surface-hover, #f0efe8);
}
</style>
