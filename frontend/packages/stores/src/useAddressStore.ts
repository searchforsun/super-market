import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAddressList, setDefaultAddress, deleteAddress } from '@supermarket/api'

export interface Address {
  id: number
  userId: number
  name: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
}

export const useAddressStore = defineStore('address', () => {
  const addresses = ref<Address[]>([])
  const loading = ref(false)

  async function fetchAddresses(userId: number) {
    loading.value = true
    try {
      const res: any = await getAddressList(userId)
      addresses.value = res?.data ?? res ?? []
    } finally {
      loading.value = false
    }
  }

  async function removeAddress(id: number, userId: number) {
    await deleteAddress(id, userId)
    addresses.value = addresses.value.filter(a => a.id !== id)
  }

  async function setDefault(id: number, userId: number) {
    await setDefaultAddress(id, userId)
    addresses.value.forEach(a => (a.isDefault = a.id === id))
  }

  const defaultAddress = computed(() => addresses.value.find(a => a.isDefault))

  return { addresses, loading, fetchAddresses, removeAddress, setDefault, defaultAddress }
})
