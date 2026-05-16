// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../useUserStore'

// Mock @supermarket/api so HTTP requests are never actually sent
vi.mock('@supermarket/api', () => ({
  authLogin: vi.fn(),
  getUserInfo: vi.fn(),
}))

import * as api from '@supermarket/api'

/** Build a fake JWT (header.payload.sig) with the given payload. */
function buildToken(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.fake-signature`
}

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('starts with unauthenticated state', () => {
    const store = useUserStore()
    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBe(false)
    expect(store.userId).toBe(0)
    expect(store.userInfo).toBeNull()
    expect(store.roles).toEqual([])
  })

  it('detects an already-stored token on creation', () => {
    const token = buildToken({ roles: ['ROLE_USER'], exp: 9999999999 })
    localStorage.setItem('accessToken', token)
    localStorage.setItem('userId', '10')

    const store = useUserStore()
    expect(store.isLoggedIn).toBe(true)
    expect(store.token).toBe(true)
    expect(store.userId).toBe(10)
    expect(store.roles).toEqual(['ROLE_USER'])
  })

  it('login() calls authLogin, persists token, and fetches user info', async () => {
    const token = buildToken({ roles: ['ROLE_USER'], exp: 9999999999 })
    const loginResponse = { accessToken: token, refreshToken: 'rt', userId: '42' }
    vi.mocked(api.authLogin).mockResolvedValue(loginResponse)
    vi.mocked(api.getUserInfo).mockResolvedValue({ id: 42, name: 'Test User' })

    const store = useUserStore()
    await store.login('13800138000', 'password123')

    expect(api.authLogin).toHaveBeenCalledWith('13800138000', 'password123')
    expect(store.isLoggedIn).toBe(true)
    expect(store.userId).toBe(42)
    expect(store.roles).toEqual(['ROLE_USER'])
    // getUserInfo should have been called after login
    expect(api.getUserInfo).toHaveBeenCalledWith(42)
    expect(store.userInfo).toEqual({ id: 42, name: 'Test User' })
  })

  it('login() works even when fetchUserInfo fails', async () => {
    const token = buildToken({ roles: ['ROLE_USER'], exp: 9999999999 })
    vi.mocked(api.authLogin).mockResolvedValue({ accessToken: token, refreshToken: 'rt', userId: '7' })
    vi.mocked(api.getUserInfo).mockRejectedValue(new Error('network error'))

    const store = useUserStore()
    await store.login('13800138000', 'password')

    expect(store.isLoggedIn).toBe(true)
    // userInfo should still be null because getUserInfo threw
    expect(store.userInfo).toBeNull()
  })

  it('logout() clears all state and localStorage', () => {
    // Set up an authenticated state first
    const jwt = buildToken({ roles: ['ROLE_USER'], exp: 9999999999 })
    localStorage.setItem('accessToken', jwt)
    localStorage.setItem('refreshToken', 'rt')
    localStorage.setItem('userId', '1')
    localStorage.setItem('userRoles', '["ROLE_USER"]')

    const store = useUserStore()
    // Verify token ref was initialized correctly (isLoggedIn is a computed that
    // reads localStorage directly — we verify the reactive refs below instead)
    expect(store.token).toBe(true)

    store.logout()

    // Token ref and other reactive state are reset directly by logout()
    expect(store.token).toBe(false)
    expect(store.userId).toBe(0)
    expect(store.userInfo).toBeNull()
    expect(store.roles).toEqual([])
    // All localStorage auth items are removed by clearAuth()
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
    expect(localStorage.getItem('userId')).toBeNull()
    expect(localStorage.getItem('userRoles')).toBeNull()
  })

  it('primaryRole is null when unauthenticated', () => {
    const store = useUserStore()
    expect(store.primaryRole).toBeNull()
  })

  it('primaryRole is ROLE_USER for a user token', () => {
    const jwt = buildToken({ roles: ['ROLE_USER'], exp: 9999999999 })
    localStorage.setItem('accessToken', jwt)
    const store = useUserStore()
    expect(store.primaryRole).toBe('ROLE_USER')
  })

  it('primaryRole is ROLE_MERCHANT for a merchant token', () => {
    const jwt = buildToken({ roles: ['ROLE_MERCHANT'], exp: 9999999999 })
    localStorage.setItem('accessToken', jwt)
    const store = useUserStore()
    expect(store.primaryRole).toBe('ROLE_MERCHANT')
  })

  it('primaryRole prefers ROLE_ADMIN over other roles', () => {
    const jwt = buildToken({ roles: ['ROLE_USER', 'ROLE_MERCHANT', 'ROLE_ADMIN'], exp: 9999999999 })
    localStorage.setItem('accessToken', jwt)
    const store = useUserStore()
    expect(store.primaryRole).toBe('ROLE_ADMIN')
  })
})
