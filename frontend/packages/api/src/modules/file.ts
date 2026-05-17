import request from '../request'

export function uploadFile(file: File, bucket?: string, uploaderId?: number) {
  const form = new FormData()
  form.append('file', file)
  if (bucket) form.append('bucket', bucket)
  if (uploaderId) form.append('uploaderId', String(uploaderId))
  return request.post('/file/upload', form)
}
