<template>
  <div class="cart-page page-enter">
    <h2>我的购物车</h2>
    <div v-if="items.length === 0" class="empty">购物车是空的，<router-link to="/">去逛逛</router-link></div>
    <div v-else>
      <div v-for="item in items" :key="item.skuId" class="cart-item">
        <el-checkbox v-model="item.selected" @change="onSelect(item)" />
        <img :src="item.skuImage || item.spuName" width="80" />
        <div class="item-info">
          <p>{{ item.spuName }} - {{ item.skuSpec }}</p>
          <PriceDisplay :price="item.price" />
        </div>
        <el-input-number v-model="item.quantity" :min="1" :max="99" size="small"
                         @change="() => updateQty(item)" />
        <span class="item-total">小计: ¥{{ (item.price * item.quantity).toFixed(2) }}</span>
        <el-button type="danger" size="small" @click="removeItem(item)">删除</el-button>
      </div>
      <div class="cart-footer">
        <el-checkbox v-model="allSelected" @change="selectAll">全选</el-checkbox>
        <span>合计: <PriceDisplay :price="totalPrice" /></span>
        <el-button type="danger" size="large" :disabled="totalPrice === 0" @click="checkout">去结算</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@supermarket/stores'
import { getCartList, updateCartQuantity, removeCartItem, selectCartItem, selectAllCart } from '@supermarket/api'
import { PriceDisplay } from '@supermarket/ui'

const router = useRouter()
const userStore = useUserStore()
const items = ref<any[]>([])

const allSelected = computed({
  get: () => items.value.length > 0 && items.value.every(i => i.selected),
  set: (v) => { selectAllCart(userStore.userId, v); items.value.forEach(i => i.selected = v) },
})
const totalPrice = computed(() =>
  items.value.filter(i => i.selected).reduce((s, i) => s + i.price * i.quantity, 0))

async function loadCart() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  items.value = await getCartList(userStore.userId)
}

async function onSelect(item: any) { await selectCartItem(userStore.userId, item.skuId, item.selected) }
async function updateQty(item: any) { await updateCartQuantity(userStore.userId, item.skuId, item.quantity) }
async function removeItem(item: any) {
  await removeCartItem(userStore.userId, item.skuId)
  items.value = items.value.filter(i => i.skuId !== item.skuId)
}
function selectAll(v: boolean) { selectAllCart(userStore.userId, v); items.value.forEach(i => i.selected = v) }
function checkout() { router.push('/order/confirm') }

onMounted(loadCart)
</script>

<style scoped>
.cart-page { background: #fff; padding: 20px; border-radius: 4px; }
.empty { text-align: center; padding: 60px 0; font-size: 16px; color: #999; }
.cart-item { display: flex; align-items: center; gap: 12px; padding: 16px 0; border-bottom: 1px solid #eee; }
.item-info { flex: 1; font-size: 13px; }
.item-total { font-size: 13px; color: #f30213; font-weight: 600; white-space: nowrap; }
.cart-footer { display: flex; align-items: center; gap: 16px; justify-content: flex-end; padding: 16px 0; }
</style>
