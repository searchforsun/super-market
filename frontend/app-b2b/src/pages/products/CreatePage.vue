<template>
  <div class="product-create-page">
    <div class="page-header">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/merchant/products' }">商品管理</el-breadcrumb-item>
        <el-breadcrumb-item>发布新商品</el-breadcrumb-item>
      </el-breadcrumb>
      <h2 class="page-title">发布新商品</h2>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      label-position="right"
      class="product-form"
    >
      <el-card shadow="never" class="form-card">
        <template #header>
          <span class="card-title">基本信息</span>
        </template>
        <el-row :gutter="24">
          <el-col :span="16">
            <el-form-item label="商品名称" prop="name">
              <el-input
                v-model="form.name"
                placeholder="请输入商品名称"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="form.brand" placeholder="请输入品牌名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="副标题" prop="subtitle">
          <el-input
            v-model="form.subtitle"
            placeholder="请输入商品副标题"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-cascader
            v-model="categoryPath"
            :options="categoryOptions"
            :props="{ expandTrigger: 'hover', value: 'id', label: 'name', children: 'children' }"
            placeholder="请选择商品分类"
            style="width: 100%"
            clearable
            @change="onCategoryChange"
          />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入商品描述"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
      </el-card>

      <el-card shadow="never" class="form-card">
        <template #header>
          <span class="card-title">商品图片</span>
        </template>
        <el-form-item label="商品主图" prop="mainImage">
          <div class="image-upload-area">
            <FileUploader
              :key="mainImageKey"
              tip="建议尺寸 800x800 像素，支持 jpg/png，单张不超过 10MB"
              @success="onMainImageSuccess"
            />
            <div v-if="form.mainImage" class="image-preview">
              <el-image
                :src="form.mainImage"
                fit="contain"
                style="width: 120px; height: 120px; border-radius: 4px;"
              />
              <el-button
                type="danger"
                size="small"
                circle
                class="image-remove-btn"
                @click="removeMainImage"
              >
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>
        </el-form-item>
        <el-divider />
        <el-form-item label="商品图册">
          <div class="gallery-upload-area">
            <el-upload
              ref="galleryUploadRef"
              action="/api/file/upload"
              :headers="uploadHeaders"
              list-type="picture-card"
              :on-success="onGallerySuccess"
              :on-remove="onGalleryRemove"
              :on-error="onUploadError"
              :before-upload="beforeImageUpload"
              multiple
            >
              <el-icon><Plus /></el-icon>
              <template #file="{ file }">
                <div>
                  <img class="el-upload-list__item-thumbnail" :src="file.url" alt="" />
                  <span class="el-upload-list__item-actions">
                    <span class="el-upload-list__item-preview" @click="handleGalleryPreview(file.url)">
                      <el-icon><ZoomIn /></el-icon>
                    </span>
                    <span
                      class="el-upload-list__item-delete"
                      @click="handleGalleryRemoveItem(file)"
                    >
                      <el-icon><Delete /></el-icon>
                    </span>
                  </span>
                </div>
              </template>
            </el-upload>
            <p class="upload-tip">支持 jpg/png/gif 格式，单张不超过 10MB，建议尺寸 800x800</p>
          </div>
        </el-form-item>
      </el-card>

      <el-card shadow="never" class="form-card">
        <template #header>
          <span class="card-title">规格与库存</span>
        </template>
        <div class="spec-section">
          <div
            v-for="(spec, specIndex) in form.specs"
            :key="specIndex"
            class="spec-item"
          >
            <div class="spec-row">
              <el-input
                v-model="spec.name"
                placeholder="规格名称，如：颜色"
                style="width: 160px"
              />
              <div class="spec-values">
                <el-tag
                  v-for="(val, valIndex) in spec.values"
                  :key="valIndex"
                  closable
                  :disable-transitions="false"
                  class="spec-tag"
                  @close="removeSpecValue(specIndex, valIndex)"
                >
                  {{ val }}
                </el-tag>
                <el-input
                  v-model="specValueInputs[specIndex]"
                  size="small"
                  style="width: 120px"
                  placeholder="输入值"
                  @keyup.enter="addSpecValue(specIndex)"
                  @blur="addSpecValue(specIndex)"
                />
              </div>
              <el-button
                type="danger"
                :icon="Delete"
                circle
                size="small"
                @click="removeSpec(specIndex)"
              />
            </div>
          </div>
          <el-button class="add-spec-btn" @click="addSpec">
            <el-icon><Plus /></el-icon>添加规格
          </el-button>
        </div>

        <el-divider v-if="form.specs.length > 0" />
        <div v-if="form.specs.length > 0" class="sku-action">
          <el-button type="primary" @click="generateSkus">
            <el-icon><Refresh /></el-icon>生成 SKU
          </el-button>
          <span class="sku-hint">点击生成基于规格组合的 SKU 列表，然后为每个 SKU 设置价格、库存和编码</span>
        </div>

        <el-table
          v-if="form.skus.length > 0"
          :data="form.skus"
          border
          stripe
          style="width: 100%; margin-top: 16px;"
          size="small"
        >
          <el-table-column label="规格组合" min-width="200">
            <template #default="{ row }">
              <span class="sku-spec-desc">{{ row.specDesc }}</span>
            </template>
          </el-table-column>
          <el-table-column label="销售价" width="160">
            <template #default="{ $index }">
              <el-input-number
                v-model="form.skus[$index].price"
                :min="0"
                :precision="2"
                size="small"
                controls-position="right"
                style="width: 130px"
              />
            </template>
          </el-table-column>
          <el-table-column label="库存" width="130">
            <template #default="{ $index }">
              <el-input-number
                v-model="form.skus[$index].stock"
                :min="0"
                :step="1"
                size="small"
                controls-position="right"
                style="width: 100px"
              />
            </template>
          </el-table-column>
          <el-table-column label="SKU编码" width="160">
            <template #default="{ $index }">
              <el-input
                v-model="form.skus[$index].code"
                placeholder="可选"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" fixed="right">
            <template #default="{ $index }">
              <el-button
                type="danger"
                link
                size="small"
                :icon="Delete"
                @click="removeSku($index)"
              />
            </template>
          </el-table-column>
        </el-table>
        <el-alert
          v-if="form.skus.length === 0 && form.specs.length > 0"
          type="info"
          show-icon
          :closable="false"
          description="请点击"生成 SKU"按钮生成商品规格组合"
          style="margin-top: 12px;"
        />
      </el-card>

      <el-card shadow="never" class="form-card">
        <template #header>
          <span class="card-title">价格设置</span>
        </template>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="市场价" prop="marketPrice">
              <el-input-number
                v-model="form.marketPrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
                placeholder="0.00"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="销售价" prop="sellingPrice">
              <el-input-number
                v-model="form.sellingPrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
                placeholder="0.00"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成本价" prop="costPrice">
              <el-input-number
                v-model="form.costPrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
                placeholder="0.00"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <el-card shadow="never" class="form-card">
        <template #header>
          <span class="card-title">其他设置</span>
        </template>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="重量(kg)" prop="weight">
              <el-input-number
                v-model="form.weight"
                :min="0"
                :precision="3"
                :step="0.1"
                controls-position="right"
                style="width: 100%"
                placeholder="0.000"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计量单位" prop="unit">
              <el-select v-model="form.unit" placeholder="请选择单位" style="width: 100%" clearable>
                <el-option label="件" value="件" />
                <el-option label="个" value="个" />
                <el-option label="箱" value="箱" />
                <el-option label="瓶" value="瓶" />
                <el-option label="袋" value="袋" />
                <el-option label="盒" value="盒" />
                <el-option label="双" value="双" />
                <el-option label="套" value="套" />
                <el-option label="千克" value="千克" />
                <el-option label="克" value="克" />
                <el-option label="升" value="升" />
                <el-option label="毫升" value="毫升" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="运费模板" prop="shippingTemplate">
              <el-select v-model="form.shippingTemplate" placeholder="请选择运费模板" style="width: 100%" clearable>
                <el-option label="全国包邮" value="free" />
                <el-option label="按件计费" value="per_item" />
                <el-option label="按重量计费" value="by_weight" />
                <el-option label="到付" value="cod" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="服务承诺" prop="servicePromise">
          <el-input
            v-model="form.servicePromise"
            type="textarea"
            :rows="3"
            placeholder="如：7天无理由退换、正品保证等，多个承诺用换行分隔"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-card>

      <div class="form-footer">
        <el-button size="large" @click="router.back()">取消</el-button>
        <el-button size="large" :loading="draftLoading" @click="handleSubmit(0)">
          保存草稿
        </el-button>
        <el-button
          type="primary"
          size="large"
          :loading="publishLoading"
          @click="handleSubmit(1)"
        >
          发布商品
        </el-button>
      </div>
    </el-form>

    <el-dialog v-model="previewDialog.visible" title="图片预览" width="auto" align-center>
      <el-image
        :src="previewDialog.url"
        fit="contain"
        style="max-width: 600px; max-height: 600px;"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElForm,
  ElMessage,
  ElMessageBox,
} from 'element-plus'
import { Plus, Close, ZoomIn, Delete, Refresh } from '@element-plus/icons-vue'
import { request, getCategoryTree } from '@supermarket/api'
import { FileUploader } from '@supermarket/ui'
import { TOKEN_KEY } from '@supermarket/utils'

const router = useRouter()
const formRef = ref<InstanceType<typeof ElForm>>()
const galleryUploadRef = ref()
const mainImageKey = ref(0)
const categoryPath = ref<(number | string)[]>([])
const specValueInputs = ref<Record<number, string>>({})
const draftLoading = ref(false)
const publishLoading = ref(false)

const previewDialog = reactive({
  visible: false,
  url: '',
})

interface SpecItem {
  name: string
  values: string[]
}

interface SkuItem {
  specDesc: string
  price: number
  stock: number
  code: string
}

interface CascaderNode {
  id: number
  name: string
  children?: CascaderNode[]
}

const form = reactive({
  name: '',
  subtitle: '',
  categoryId: null as number | null,
  brand: '',
  description: '',
  mainImage: '',
  galleryImages: [] as string[],
  specs: [] as SpecItem[],
  skus: [] as SkuItem[],
  marketPrice: undefined as number | undefined,
  sellingPrice: undefined as number | undefined,
  costPrice: undefined as number | undefined,
  weight: undefined as number | undefined,
  unit: '',
  shippingTemplate: '',
  servicePromise: '',
})

const rules: Record<string, any[]> = {
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 200, message: '商品名称长度应在 2-200 个字符之间', trigger: 'blur' },
  ],
  categoryId: [
    { required: true, message: '请选择商品分类', trigger: 'change' },
  ],
  mainImage: [
    { required: true, message: '请上传商品主图', trigger: 'change' },
  ],
  sellingPrice: [
    { required: true, message: '请输入销售价', trigger: 'blur' },
  ],
  brand: [
    { min: 0, max: 100, message: '品牌名称不超过 100 个字符', trigger: 'blur' },
  ],
}

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}`,
}))

const categoryOptions = ref<CascaderNode[]>([])

function transformCategoryTree(nodes: any[]): CascaderNode[] {
  return nodes.map((n: any) => ({
    id: n.id,
    name: n.name,
    children: n.children ? transformCategoryTree(n.children) : undefined,
  }))
}

function onCategoryChange(value: (number | string)[]) {
  if (value && value.length > 0) {
    form.categoryId = Number(value[value.length - 1])
  } else {
    form.categoryId = null
  }
}

function onMainImageSuccess(url: string) {
  form.mainImage = url
}

function removeMainImage() {
  form.mainImage = ''
  mainImageKey.value++
}

function onGallerySuccess(response: any) {
  if (response?.data?.url) {
    form.galleryImages.push(response.data.url)
  }
}

function onGalleryRemove(file: any, fileList: any[]) {
  const url = file.response?.data?.url || file.url
  if (url) {
    const idx = form.galleryImages.indexOf(url)
    if (idx >= 0) {
      form.galleryImages.splice(idx, 1)
    }
  }
}

function handleGalleryPreview(url: string) {
  previewDialog.url = url
  previewDialog.visible = true
}

function handleGalleryRemoveItem(file: any) {
  const url = file.response?.data?.url || file.url
  if (url) {
    const idx = form.galleryImages.indexOf(url)
    if (idx >= 0) {
      form.galleryImages.splice(idx, 1)
    }
  }
}

function onUploadError() {
  ElMessage.error('图片上传失败，请重试')
}

function beforeImageUpload(file: File): boolean {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('不支持的文件类型，请上传 jpg/png/gif/webp 格式')
    return false
  }
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

function addSpec() {
  form.specs.push({ name: '', values: [] })
  form.skus = []
}

function removeSpec(index: number) {
  form.specs.splice(index, 1)
  form.skus = []
  delete specValueInputs.value[index]
}

function addSpecValue(specIndex: number) {
  const input = specValueInputs.value[specIndex]
  if (!input || !input.trim()) return
  const trimmed = input.trim()
  if (form.specs[specIndex].values.includes(trimmed)) {
    ElMessage.warning(`规格值"${trimmed}"已存在`)
    return
  }
  form.specs[specIndex].values.push(trimmed)
  specValueInputs.value[specIndex] = ''
  form.skus = []
}

function removeSpecValue(specIndex: number, valueIndex: number) {
  form.specs[specIndex].values.splice(valueIndex, 1)
  if (form.specs[specIndex].values.length === 0) {
    form.specs.splice(specIndex, 1)
  }
  form.skus = []
}

function cartesianProduct(arrays: string[][]): string[][] {
  if (arrays.length === 0) return []
  return arrays.reduce<string[][]>(
    (acc, curr) => {
      const result: string[][] = []
      acc.forEach((a) => {
        curr.forEach((c) => {
          result.push([...a, c])
        })
      })
      return result
    },
    [[]]
  )
}

function generateSkus() {
  const validSpecs = form.specs.filter((s) => s.name.trim() && s.values.length > 0)
  if (validSpecs.length === 0) {
    ElMessage.warning('请先填写规格名称和规格值')
    return
  }

  const valueArrays = validSpecs.map((s) => s.values)
  const combinations = cartesianProduct(valueArrays)

  if (combinations.length > 500) {
    ElMessageBox.confirm(
      `将生成 ${combinations.length} 个 SKU，数量较多是否继续？`,
      '确认',
      { confirmButtonText: '继续', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
      buildSkuList(validSpecs, combinations)
    }).catch(() => {})
    return
  }

  buildSkuList(validSpecs, combinations)
}

function buildSkuList(specs: SpecItem[], combinations: string[][]) {
  const newSkus: SkuItem[] = combinations.map((combo) => {
    const desc = specs.map((s, i) => `${s.name}:${combo[i]}`).join('; ')
    const existing = form.skus.find((sk) => sk.specDesc === desc)
    return {
      specDesc: desc,
      price: existing?.price ?? form.sellingPrice ?? 0,
      stock: existing?.stock ?? 0,
      code: existing?.code ?? '',
    }
  })
  form.skus = newSkus
  ElMessage.success(`已生成 ${newSkus.length} 个 SKU`)
}

function removeSku(index: number) {
  form.skus.splice(index, 1)
}

function handleSubmit(status: number) {
  if (status === 1) {
    publishLoading.value = true
  } else {
    draftLoading.value = true
  }

  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) {
      publishLoading.value = false
      draftLoading.value = false
      ElMessage.warning('请完善表单信息')
      return
    }

    try {
      const payload: Record<string, any> = {
        name: form.name,
        subtitle: form.subtitle,
        categoryId: form.categoryId,
        brand: form.brand,
        description: form.description,
        mainImage: form.mainImage,
        galleryImages: form.galleryImages.join(','),
        marketPrice: form.marketPrice ?? 0,
        price: form.sellingPrice ?? 0,
        costPrice: form.costPrice ?? 0,
        weight: form.weight ?? 0,
        unit: form.unit,
        shippingTemplate: form.shippingTemplate,
        servicePromise: form.servicePromise,
        status,
      }

      if (form.skus.length > 0) {
        payload.skus = form.skus.map((sk) => ({
          specDesc: sk.specDesc,
          price: sk.price,
          stock: sk.stock,
          code: sk.code,
        }))
      }

      await request.post('/product/spu', payload)
      ElMessage.success(status === 1 ? '商品发布成功' : '草稿保存成功')
      router.push({ name: 'products' })
    } catch (err: any) {
      ElMessage.error(err?.message || (status === 1 ? '发布失败，请重试' : '保存失败，请重试'))
    } finally {
      publishLoading.value = false
      draftLoading.value = false
    }
  })
}

onMounted(async () => {
  try {
    const res: any = await getCategoryTree()
    const treeData = Array.isArray(res) ? res : res?.data ?? []
    categoryOptions.value = transformCategoryTree(treeData)
  } catch {
    ElMessage.error('加载分类信息失败')
  }
})
</script>

<style scoped>
.product-create-page {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 100px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 12px 0 0 0;
}

.product-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.form-card :deep(.el-card__header) {
  padding: 16px 24px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.image-upload-area {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.image-preview {
  position: relative;
  display: inline-block;
}

.image-remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
}

.gallery-upload-area {
  width: 100%;
}

.gallery-upload-area :deep(.el-upload--picture-card) {
  width: 100px;
  height: 100px;
  line-height: 108px;
}

.gallery-upload-area :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 100px;
  height: 100px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.spec-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.spec-item {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px;
}

.spec-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.spec-values {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 4px 8px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.spec-tag {
  margin: 2px 0;
}

.add-spec-btn {
  align-self: flex-start;
}

.sku-action {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sku-hint {
  font-size: 13px;
  color: #909399;
}

.sku-spec-desc {
  font-size: 13px;
  color: #606266;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 24px 0;
  border-top: 1px solid #ebeef5;
  margin-top: 8px;
}
</style>
