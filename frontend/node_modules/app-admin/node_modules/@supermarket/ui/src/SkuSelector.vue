<template>
  <div class="sku-selector">
    <div v-for="spec in specs" :key="spec.name" class="spec-group">
      <span class="spec-label">{{ spec.name }}:</span>
      <span v-for="val in spec.values" :key="val" :class="['spec-val', selected[spec.name] === val ? 'active' : '']"
            @click="$emit('select', spec.name, val)">{{ val }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

defineProps<{ specs: Array<{ name: string; values: string[] }> }>()
const selected = reactive<Record<string, string>>({})
defineEmits<{ select: [name: string, value: string] }>()
</script>

<style scoped>
.spec-group { margin-bottom: 8px; }
.spec-label { font-size: 12px; color: #999; margin-right: 8px; }
.spec-val { display: inline-block; padding: 4px 12px; margin: 2px 4px; border: 1px solid #ddd; border-radius: 3px; cursor: pointer; font-size: 12px; }
.spec-val:hover { border-color: #e1251b; }
.spec-val.active { border-color: #e1251b; color: #e1251b; background: #fff0f0; }
</style>
