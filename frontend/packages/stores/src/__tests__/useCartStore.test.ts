// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCartStore } from '../useCartStore'

vi.mock('@supermarket/api', () => ({
  getCartCount: vi.fn(),
}))

import * as api from '@supermarket/api'

describe('useCartStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('starts with zero count and no items', () => {
    const store = useCartStore()
    expect(store.count).toBe(0)
    expect(store.hasItems).toBe(false)
  })

  it('fetchCount does nothing when userId is 0 (not logged in)', async () => {
    const store = useCartStore()
    await store.fetchCount()
    expect(api.getCartCount).not.toHaveBeenCalled()
    expect(store.count).toBe(0)
  })

  it('fetchCount updates count and hasItems when a userId is set', async () => {
    // Simulate a logged-in user by setting localStorage for the user store
    localStorage.setItem('userId', '5')
    // Re-create the user store to pick up the userId
    const { useUserStore } = await import('../useUserStore')
    const userStore = useUserStore()
    userStore.userId = 5

    vi.mocked(api.getCartCount).mockResolvedValue(3)

    const cartStore = useCartStore()
    await cartStore.fetchCount()

    expect(api.getCartCount).toHaveBeenCalledWith(5)
    expect(cartStore.count).toBe(3)
    expect(cartStore.hasItems).toBe(true)
  })

  it('fetchCount gracefully handles API failure', async () => {
    localStorage.setItem('userId', '5')
    const { useUserStore } = await import('../useUserStore')
    const userStore = useUserStore()
    userStore.userId = 5

    vi.mocked(api.getCartCount).mockRejectedValue(new Error('API error'))

    const cartStore = useCartStore()
    await cartStore.fetchCount()

    expect(cartStore.count).toBe(0)
    expect(cartStore.hasItems).toBe(false)
  })

  it('hasItems returns true only when count > 0', () => {
    const store = useCartStore()
    expect(store.hasItems).toBe(false)

    store.count = 1
    expect(store.hasItems).toBe(true)

    store.count = 0
    expect(store.hasItems).toBe(false)
  })
})
