<template>
  <div class="data-table-wrap">
    <el-table :data="data" stripe border style="width:100%" @sort-change="$emit('sort-change', $event)">
      <el-table-column v-for="col in columns" :key="col.prop" v-bind="col" />
      <el-table-column v-if="$slots.actions" label="操作" width="180">
        <template #default="scope"><slot name="actions" :row="scope.row" /></template>
      </el-table-column>
    </el-table>
    <div class="table-pagination" v-if="total > pageSize">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
                     :current-page="currentPage" @current-change="$emit('page-change', $event)" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  data: any[]
  columns: any[]
  total: number
  pageSize?: number
  currentPage?: number
}>()
defineEmits<{ 'sort-change': [sort: any]; 'page-change': [page: number] }>()
</script>

<style scoped>
.data-table-wrap { background: #fff; }
.table-pagination { display: flex; justify-content: flex-end; padding: 12px 0; }
</style>
