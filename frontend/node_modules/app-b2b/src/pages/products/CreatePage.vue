<template><div class="create-prod"><h2>发布商品</h2><el-form label-width="80px"><el-form-item label="商品名称"><el-input v-model="form.name"/></el-form-item><el-form-item label="副标题"><el-input v-model="form.subtitle"/></el-form-item><el-form-item label="主图"><FileUploader @success="url => form.mainImage = url"/></el-form-item><el-button type="primary" @click="submit">提交审核</el-button></el-form></div></template>
<script setup lang="ts">import { reactive } from 'vue'; import { FileUploader } from '@supermarket/ui'; import { ElMessage } from 'element-plus'
const form = reactive({ name:'', subtitle:'', mainImage:'', shopId:1, categoryId:1 })
function submit() { ElMessage.success('商品已提交审核') }</script>
<style scoped>.create-prod{background:#fff;padding:20px;border-radius:4px}</style>
