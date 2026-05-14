<template><div class="dash"><h2>运营看板</h2><div style="display:flex;gap:12px;flex-wrap:wrap">
  <StatCard label="今日 GMV" :value="'¥ 52,380'" trend="up" trendValue="12.5%"/>
  <StatCard label="总订单数" :value="1247" trend="up" trendValue="8.2%"/>
  <StatCard label="新增用户" :value="589" trend="up" trendValue="15.3%"/>
  <StatCard label="待审核商家" :value="23"/>
  <StatCard label="待审核商品" :value="67"/>
  <StatCard label="退款率" :value="'1.8%'" trend="down" trendValue="0.3%"/>
</div></div></template>
<script setup lang="ts">import { StatCard } from '@supermarket/ui'</script>
<style scoped>.dash{background:#fff;padding:20px;border-radius:4px}</style>
