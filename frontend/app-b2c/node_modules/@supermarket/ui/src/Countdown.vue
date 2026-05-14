<template>
  <span class="countdown" :class="{ urgent: remaining < 300 }">
    ⚡ {{ formatCountdown(remaining) }}
  </span>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { formatCountdown } from '@supermarket/utils'

const props = defineProps<{ endTime: string | Date }>()

function calcRemaining(): number {
  const diff = new Date(props.endTime).getTime() - Date.now()
  return Math.max(0, Math.floor(diff / 1000))
}

const remaining = ref(calcRemaining())
let timer: ReturnType<typeof setInterval>

onMounted(() => {
  timer = setInterval(() => { remaining.value = calcRemaining() }, 1000)
})

onUnmounted(() => { clearInterval(timer) })

defineEmits<{ finish: [] }>()
</script>

<style scoped>
.countdown { font-size: 14px; font-weight: 600; color: #e1251b; }
.countdown.urgent { animation: pulse 0.5s infinite alternate; }
@keyframes pulse { from { opacity: 1; } to { opacity: 0.5; } }
</style>
