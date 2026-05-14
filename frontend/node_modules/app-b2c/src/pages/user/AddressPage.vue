<template>
  <div class="addr-page"><h2>收货地址</h2>
    <el-button type="primary" @click="showForm=true">新增地址</el-button>
    <div v-for="a in addresses" :key="a.id" class="addr-card">
      <strong>{{ a.receiverName }}</strong> {{ a.receiverPhone }}
      <p>{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</p>
      <el-button size="small" @click="setDefault(a)">{{ a.isDefault ? '默认' : '设为默认' }}</el-button>
      <el-button size="small" type="danger" @click="delAddr(a)">删除</el-button>
    </div>
    <el-dialog v-model="showForm" title="新增地址"><el-form :model="form">
      <el-form-item label="收货人"><el-input v-model="form.receiverName" /></el-form-item>
      <el-form-item label="电话"><el-input v-model="form.receiverPhone" /></el-form-item>
      <el-form-item label="省"><el-input v-model="form.province" /></el-form-item>
      <el-form-item label="市"><el-input v-model="form.city" /></el-form-item>
      <el-form-item label="区"><el-input v-model="form.district" /></el-form-item>
      <el-form-item label="详细地址"><el-input v-model="form.detail" /></el-form-item>
      <el-button type="primary" @click="saveAddr">保存</el-button>
    </el-form></el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'; import { useUserStore } from '@supermarket/stores'
import { getAddressList, createAddress, deleteAddress, setDefaultAddress } from '@supermarket/api'
const userStore = useUserStore(); const addresses = ref<any[]>([]); const showForm = ref(false)
const form = reactive({ receiverName:'', receiverPhone:'', province:'', city:'', district:'', detail:'', userId:0 })
onMounted(async () => { addresses.value = await getAddressList(userStore.userId) })
async function saveAddr() { form.userId = userStore.userId; await createAddress({...form}); showForm.value = false; addresses.value = await getAddressList(userStore.userId) }
async function setDefault(a: any) { await setDefaultAddress(a.id, userStore.userId); addresses.value = await getAddressList(userStore.userId) }
async function delAddr(a: any) { await deleteAddress(a.id, userStore.userId); addresses.value = addresses.value.filter(x => x.id !== a.id) }
</script>
<style scoped>
.addr-page{background:#fff;padding:20px;border-radius:4px}
.addr-card{padding:12px;border:1px solid #eee;border-radius:4px;margin:8px 0}
</style>
