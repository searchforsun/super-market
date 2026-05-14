<template>
  <div class="file-uploader">
    <el-upload :action="uploadUrl" :headers="headers" :before-upload="beforeUpload"
               :on-success="onSuccess" :on-error="onError" list-type="picture-card">
      <el-icon><Plus /></el-icon>
    </el-upload>
    <p class="upload-tip">{{ tip }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage, ElUpload } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { TOKEN_KEY } from '@supermarket/utils'

const props = withDefaults(defineProps<{ tip?: string; maxSize?: number }>(), {
  tip: '支持 jpg/png/gif，单张不超过 10MB',
  maxSize: 10,
})

const emit = defineEmits<{ success: [url: string] }>()

const uploadUrl = '/api/file/upload'
const headers = computed(() => ({ Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}` }))

function beforeUpload(file: File) {
  const allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowed.includes(file.type)) { ElMessage.error('不支持的文件类型'); return false }
  if (file.size > props.maxSize * 1024 * 1024) { ElMessage.error(`文件不能超过${props.maxSize}MB`); return false }
  return true
}

function onSuccess(response: any) {
  if (response.data?.url) emit('success', response.data.url)
}

function onError() { ElMessage.error('上传失败') }
</script>

<style scoped>
.file-uploader { padding: 8px 0; }
.upload-tip { font-size: 11px; color: #999; margin-top: 4px; }
</style>
