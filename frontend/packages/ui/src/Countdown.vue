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
  const t = new Date(props.endTime).getTime()
  if (isNaN(t)) return 0
  const diff = t - Date.now()
  return Math.max(0, Math.floor(diff / 1000))
}

const emit = defineEmits<{ finish: [] }>()
const remaining = ref(calcRemaining())
let timer: ReturnType<typeof setInterval>
let finished = false

onMounted(() => {
  if (remaining.value <= 0) { finished = true; return }
  timer = setInterval(() => {
    remaining.value = calcRemaining()
    if (remaining.value <= 0) {
      clearInterval(timer)
      if (!finished) { finished = true; emit('finish') }
    }
  }, 1000)
})

onUnmounted(() => { clearInterval(timer) })
</script>

<style scoped>
.countdown { font-size: 14px; font-weight: 600; color: #e1251b; }
.countdown.urgent { animation: pulse 0.5s infinite alternate; }
@keyframes pulse { from { opacity: 1; } to { opacity: 0.5; } }
</style>
