<template>
  <div class="confirm-page">
    <h2>确认订单</h2>
    <div class="confirm-section"><h3>收货地址</h3>
      <el-select v-model="addressId" placeholder="请选择收货地址" style="width:300px">
        <el-option v-for="a in addresses" :key="a.id" :label="`${a.receiverName} ${a.receiverPhone} ${a.detail}`" :value="a.id" />
      </el-select>
    </div>
    <div class="confirm-section"><h3>商品明细</h3>
      <div v-for="item in cartItems" :key="item.skuId" class="confirm-item">
        <span>{{ item.spuName }} × {{ item.quantity }}</span>
        <PriceDisplay :price="item.price * item.quantity" />
      </div>
      <div class="confirm-total">合计: <PriceDisplay :price="totalPrice" /></div>
    </div>
    <div class="confirm-section"><h3>优惠券</h3>
      <el-select v-model="couponId" placeholder="选择优惠券" clearable style="width:300px">
        <el-option v-for="c in coupons" :key="c.id" :label="`${c.couponCode} - 满${c.minAmount || 0}减${c.discountValue || c.amount}`" :value="c.id" />
      </el-select>
    </div>
    <el-button type="danger" size="large" @click="submitOrder" :loading="submitting" style="margin-top:20px">提交订单</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@supermarket/stores'
import { getAddressList } from '@supermarket/api'
import { getCartList } from '@supermarket/api'
import { getAvailableList } from '@supermarket/api'
import { createOrder } from '@supermarket/api'
import { PriceDisplay } from '@supermarket/ui'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const addresses = ref<any[]>([])
const cartItems = ref<any[]>([])
const coupons = ref<any[]>([])
const addressId = ref<number>()
const couponId = ref<number>()
const submitting = ref(false)

const totalPrice = computed(() => cartItems.value.reduce((s, i) => s + i.price * i.quantity, 0))

onMounted(async () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  addresses.value = await getAddressList(userStore.userId)
  coupons.value = await getAvailableList(userStore.userId)
  const all = await getCartList(userStore.userId)
  cartItems.value = all.filter((i: any) => i.selected)
  if (cartItems.value.length === 0) { ElMessage.warning('请先选择商品'); router.push('/cart') }
  if (addresses.value.length) addressId.value = addresses.value.find((a: any) => a.isDefault)?.id || addresses.value[0].id
})

async function submitOrder() {
  if (!addressId.value) { ElMessage.warning('请选择收货地址'); return }
  submitting.value = true
  try {
    const addr = addresses.value.find((a: any) => a.id === addressId.value)
    const res: any = await createOrder({
      userId: userStore.userId,
      addressId: addressId.value,
      addressSnapshot: JSON.stringify(addr),
      items: cartItems.value.map((i: any) => ({ skuId: i.skuId, skuName: i.spuName, skuPrice: i.price, quantity: i.quantity })),
    })
    router.push({ path: '/order/result', query: { orderNo: res.orderNo } })
  } catch {} finally { submitting.value = false }
}
</script>

<style scoped>
.confirm-page { max-width: 800px; margin: 0 auto; background: #fff; padding: 24px; border-radius: 4px; }
.confirm-section { margin: 16px 0; padding: 12px 0; border-bottom: 1px solid #eee; }
.confirm-section h3 { font-size: 14px; margin-bottom: 8px; }
.confirm-item { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; }
.confirm-total { text-align: right; font-size: 16px; font-weight: 600; margin-top: 12px; }
</style>
