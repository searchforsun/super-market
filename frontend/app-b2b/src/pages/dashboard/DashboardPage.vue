<template><div class="dash"><h2>商家看板</h2><div style="display:flex;gap:12px"><StatCard label="今日订单" :value="stats.orders"/><StatCard label="商品数量" :value="stats.products"/><StatCard label="待处理" :value="stats.pending"/></div></div></template>
<script setup lang="ts">import { reactive } from 'vue'; import { StatCard } from '@supermarket/ui'
const stats = reactive({ orders: 35, products: 128, pending: 12 })</script>
<style scoped>.dash{background:#fff;padding:20px;border-radius:4px}</style>
