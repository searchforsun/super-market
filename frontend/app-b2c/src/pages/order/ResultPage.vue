<template>
  <div class="result-page page-enter">
    <el-result icon="success" title="订单提交成功！" :sub-title="`订单号: ${route.query.orderNo}`">
      <template #extra>
        <el-button type="primary" @click="$router.push(`/order/${route.query.orderNo}`)">查看订单</el-button>
        <el-button @click="$router.push('/')">继续购物</el-button>
      </template>
    </el-result>
  </div>
</template>
<script setup lang="ts">import { useRoute } from 'vue-router'; const route = useRoute()</script>
