<template>
  <span class="price-display">
    <span class="symbol">¥</span>
    <span class="value">{{ integer }}</span>
    <span class="decimal">.{{ decimal }}</span>
    <span v-if="originalPrice && originalPrice > price" class="original">¥{{ originalPrice.toFixed(2) }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ price: number; originalPrice?: number }>()

const safePrice = computed(() => (typeof props.price === 'number' && !isNaN(props.price)) ? props.price : 0)
const parts = computed(() => safePrice.value.toFixed(2).split('.'))
const integer = computed(() => parts.value[0])
const decimal = computed(() => parts.value[1])
</script>

<style scoped>
.price-display { font-weight: 700; color: #f30213; }
.symbol { font-size: 12px; }
.value { font-size: 18px; }
.decimal { font-size: 12px; }
.original { font-size: 11px; color: #999; text-decoration: line-through; margin-left: 6px; font-weight: 400; }
</style>
