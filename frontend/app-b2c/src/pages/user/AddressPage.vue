<template>
  <div class="address-page page-enter">
    <div class="page-header">
      <h2>收货地址</h2>
      <el-button type="primary" @click="openAdd">新增地址</el-button>
    </div>

    <div v-loading="loading" class="address-list">
      <template v-if="addresses.length">
        <div v-for="a in addresses" :key="a.id" class="addr-card" :class="{ 'is-default': a.isDefault === 1 }">
          <div class="card-top">
            <div class="name-row">
              <span class="name">{{ a.receiverName }}</span>
              <el-tag v-if="a.isDefault === 1" type="danger" size="small">默认</el-tag>
              <el-tag v-else size="small" class="loc-tag">{{ a.province }}</el-tag>
            </div>
            <span class="phone">{{ a.receiverPhone }}</span>
          </div>
          <p class="full-addr">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</p>
          <div class="card-actions">
            <el-button text type="primary" size="small" @click="openEdit(a)">编辑</el-button>
            <el-button text size="small" @click="handleDelete(a)">删除</el-button>
            <el-button v-if="a.isDefault !== 1" text type="primary" size="small" @click="handleSetDefault(a)">
              设为默认
            </el-button>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无收货地址" />
    </div>

    <!-- Add / Edit Dialog -->
    <el-dialog v-model="showDialog" :title="editId ? '编辑收货地址' : '新增收货地址'" width="540px"
      destroy-on-close :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="省" prop="province" label-width="40px">
              <el-input v-model="form.province" placeholder="省份" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="市" prop="city" label-width="40px">
              <el-input v-model="form.city" placeholder="城市" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区" prop="district" label-width="40px">
              <el-input v-model="form.district" placeholder="区县" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="form.detail" type="textarea" :rows="2" placeholder="街道门牌号等" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.label" placeholder="如：家、公司、学校" maxlength="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@supermarket/stores'
import { getAddressList, createAddress, updateAddress, deleteAddress, setDefaultAddress } from '@supermarket/api'

const userStore = useUserStore()
const addresses = ref<any[]>([])
const loading = ref(false)
const showDialog = ref(false)
const saving = ref(false)
const editId = ref<number | null>(null)
const formRef = ref()

interface AddrForm { receiverName: string; receiverPhone: string; province: string; city: string; district: string; detail: string; label: string }

const emptyForm = (): AddrForm => ({
  receiverName: '', receiverPhone: '', province: '', city: '', district: '', detail: '', label: '',
})

const form = reactive<AddrForm>(emptyForm())

const rules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }, { min: 2, max: 20, message: '2-20个字符', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }, { min: 2, max: 100, message: '2-100个字符', trigger: 'blur' }],
}

async function fetchAddresses() {
  loading.value = true
  try { addresses.value = (await getAddressList(userStore.userId)) || [] }
  catch { addresses.value = [] }
  finally { loading.value = false }
}

function openAdd() {
  editId.value = null
  Object.assign(form, emptyForm())
  showDialog.value = true
}

function openEdit(a: any) {
  editId.value = a.id
  Object.assign(form, { receiverName: a.receiverName || '', receiverPhone: a.receiverPhone || '', province: a.province || '', city: a.city || '', district: a.district || '', detail: a.detail || '', label: '' })
  showDialog.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload: Record<string, any> = {
      receiverName: form.receiverName, receiverPhone: form.receiverPhone,
      province: form.province, city: form.city, district: form.district,
      detail: form.detail, userId: userStore.userId,
    }
    if (editId.value) { payload.id = editId.value; await updateAddress(payload); ElMessage.success('修改成功') }
    else { await createAddress(payload); ElMessage.success('添加成功') }
    showDialog.value = false
    await fetchAddresses()
  } catch { /* request interceptor handles error toast */ }
  finally { saving.value = false }
}

async function handleSetDefault(a: any) {
  try { await setDefaultAddress(a.id, userStore.userId); ElMessage.success('已设为默认地址'); await fetchAddresses() }
  catch { /* handled */ }
}

async function handleDelete(a: any) {
  try {
    await ElMessageBox.confirm(`确定删除「${a.province}${a.city} ${a.receiverName}」的地址吗？`, '删除确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
  } catch { return }
  try { await deleteAddress(a.id, userStore.userId); ElMessage.success('已删除'); await fetchAddresses() }
  catch { /* handled */ }
}

onMounted(fetchAddresses)
</script>

<style scoped>
.address-page { background: #fff; padding: 24px; border-radius: 6px; min-height: 400px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #333; }
.address-list { min-height: 200px; }
.addr-card { padding: 14px 18px; border: 1px solid #eee; border-radius: 6px; margin-bottom: 12px; transition: border-color .2s; }
.addr-card:hover { border-color: #e1251b; }
.addr-card.is-default { border-color: #fde2e2; background: #fffafa; }
.card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.name-row { display: flex; align-items: center; gap: 8px; }
.name { font-size: 15px; font-weight: 600; color: #333; }
.loc-tag { color: #999; }
.phone { font-size: 14px; color: #666; }
.full-addr { font-size: 13px; color: #888; line-height: 1.6; margin: 0 0 8px; }
.card-actions { display: flex; gap: 4px; padding-top: 8px; border-top: 1px dashed #f0f0f0; }
</style>
