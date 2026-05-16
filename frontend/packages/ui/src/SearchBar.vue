<template>
  <div class="search-bar">
    <div class="search-input-wrap">
      <svg class="search-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
      </svg>
      <input ref="inputEl" v-model="keyword" type="text" :placeholder="placeholder"
        @keyup.enter="onSearch" @focus="focused = true" @blur="focused = false" />
    </div>
    <button @click="onSearch">搜索</button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(defineProps<{ placeholder?: string }>(), {
  placeholder: '搜索你想要的商品...',
})
const emit = defineEmits<{ search: [keyword: string] }>()
const keyword = ref('')
const focused = ref(false)
const inputEl = ref<HTMLInputElement>()

function onSearch() {
  emit('search', keyword.value.trim())
}
</script>

<style scoped>
.search-bar {
  display: flex;
  width: 100%;
}
.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: var(--color-bg, #faf8f5);
  border: 1.5px solid var(--color-border, #e8e3db);
  border-right: none;
  border-radius: var(--radius-full, 9999px) 0 0 var(--radius-full, 9999px);
  padding: 0 14px;
  transition: border-color 0.2s ease;
}
.search-input-wrap:focus-within {
  border-color: var(--color-accent, #c41e3a);
}
.search-icon {
  color: var(--color-text-muted, #a09890);
  flex-shrink: 0;
  margin-right: 8px;
}
.search-input-wrap input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px 0;
  font-size: 14px;
  color: var(--color-text-primary, #1a1816);
  outline: none;
  font-family: inherit;
}
.search-input-wrap input::placeholder {
  color: var(--color-text-muted, #a09890);
}
.search-bar button {
  background: var(--color-accent, #c41e3a);
  color: #fff;
  border: none;
  padding: 10px 24px;
  border-radius: 0 var(--radius-full, 9999px) var(--radius-full, 9999px) 0;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease;
  white-space: nowrap;
}
.search-bar button:hover {
  background: var(--color-accent-hover, #a01830);
}
</style>
