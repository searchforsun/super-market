<template>
  <div class="search-bar">
    <input v-model="keyword" type="text" :placeholder="placeholder" @keyup.enter="onSearch" />
    <button @click="onSearch">🔍 搜索</button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(defineProps<{ placeholder?: string }>(), { placeholder: '搜索商品...' })
const emit = defineEmits<{ search: [keyword: string] }>()
const keyword = ref('')

function onSearch() {
  emit('search', keyword.value.trim())
}
</script>

<style scoped>
.search-bar { display: flex; width: 100%; max-width: 600px; }
.search-bar input {
  flex: 1; padding: 8px 14px; border: 2px solid #e1251b; border-right: none; border-radius: 2px 0 0 2px; outline: none; font-size: 14px;
}
.search-bar button {
  background: #e1251b; color: #fff; border: none; padding: 8px 20px; border-radius: 0 2px 2px 0; cursor: pointer; font-size: 14px;
}
</style>
