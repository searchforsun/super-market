<template>
  <div class="file-uploader">
    <el-upload
      :action="uploadUrl"
      :headers="headers"
      :data="uploadData"
      :before-upload="beforeUpload"
      :on-success="onSuccess"
      :on-error="onError"
      :on-remove="onRemove"
      :file-list="fileList"
      list-type="picture-card"
    >
      <el-icon v-if="fileList.length < maxCount"><Plus /></el-icon>
    </el-upload>
    <p v-if="tip" class="upload-tip">{{ tip }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadRawFile } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { TOKEN_KEY } from '@supermarket/utils'

interface FileItem {
  name: string
  url: string
}

const props = withDefaults(defineProps<{
  tip?: string
  maxSize?: number
  maxCount?: number
  currentFiles?: string[]
  bucket?: string
  uploaderId?: string | number
}>(), {
  tip: '支持 jpg/png/gif，单张不超过 10MB',
  maxSize: 10,
  maxCount: 9,
  currentFiles: () => [],
  bucket: '',
  uploaderId: '',
})

const emit = defineEmits<{
  success: [url: string]
  'update:files': [urls: string[]]
}>()

const uploadUrl = import.meta.env.VITE_API_BASE ? import.meta.env.VITE_API_BASE + '/file/upload' : '/api/file/upload'
const headers = computed(() => ({ Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}` }))
const uploadData = computed(() => {
  const data: Record<string, any> = {}
  if (props.bucket) data.bucket = props.bucket
  if (props.uploaderId) data.uploaderId = props.uploaderId
  return data
})

const uploadedUrls = ref<string[]>([...props.currentFiles])

const fileList = computed<FileItem[]>(() =>
  uploadedUrls.value.map((url, i) => ({ name: `image-${i + 1}`, url }))
)

watch(() => props.currentFiles, (val) => {
  uploadedUrls.value = [...val]
})

function beforeUpload(file: UploadRawFile) {
  const allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowed.includes(file.type)) { ElMessage.error('不支持的文件类型'); return false }
  if (file.size > props.maxSize * 1024 * 1024) { ElMessage.error(`文件不能超过${props.maxSize}MB`); return false }
  if (uploadedUrls.value.length >= props.maxCount) { ElMessage.error(`最多上传${props.maxCount}张`); return false }
  return true
}

function onSuccess(response: any) {
  const url = response?.data?.url
  if (url) {
    uploadedUrls.value = [...uploadedUrls.value, url]
    emit('success', url)
    emit('update:files', uploadedUrls.value)
  }
}

function onRemove(_file: UploadFile) {
  const url = _file.url || _file.response?.data?.url
  if (url) {
    uploadedUrls.value = uploadedUrls.value.filter(u => u !== url)
    emit('update:files', uploadedUrls.value)
  }
}

function onError() { ElMessage.error('上传失败') }
</script>

<style scoped>
.file-uploader { padding: 8px 0; }
.upload-tip { font-size: 11px; color: var(--color-text-muted, #999); margin-top: 4px; }
</style>
