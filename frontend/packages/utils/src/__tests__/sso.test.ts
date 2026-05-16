// @vitest-environment jsdom
import { describe, it, expect, beforeEach } from 'vitest'
import {
  getRoles,
  hasRole,
  isAuthenticated,
  saveAuth,
  clearAuth,
  getPrimaryRole,
  absorbTokenFromUrl,
} from '../sso'

/** Build a fake JWT with the given payload (header + payload are base64url-encoded JSON). */
function buildToken(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.fake-signature`
}

describe('sso', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  describe('getRoles', () => {
    it('returns empty array when no token is stored', () => {
      expect(getRoles()).toEqual([])
    })

    it('returns roles from a stored JWT', () => {
      const token = buildToken({ roles: ['ROLE_USER'] })
      localStorage.setItem('accessToken', token)
      expect(getRoles()).toEqual(['ROLE_USER'])
    })

    it('returns empty array when token has no roles field', () => {
      const token = buildToken({ sub: '1' })
      localStorage.setItem('accessToken', token)
      expect(getRoles()).toEqual([])
    })

    it('returns multiple roles', () => {
      const token = buildToken({ roles: ['ROLE_USER', 'ROLE_ADMIN'] })
      localStorage.setItem('accessToken', token)
      expect(getRoles()).toEqual(['ROLE_USER', 'ROLE_ADMIN'])
    })
  })

  describe('hasRole', () => {
    it('returns false when no token', () => {
      expect(hasRole('ROLE_USER')).toBe(false)
    })

    it('returns true when the role is present', () => {
      const token = buildToken({ roles: ['ROLE_USER'] })
      localStorage.setItem('accessToken', token)
      expect(hasRole('ROLE_USER')).toBe(true)
      expect(hasRole('ROLE_ADMIN')).toBe(false)
    })
  })

  describe('isAuthenticated', () => {
    it('returns false when no token', () => {
      expect(isAuthenticated()).toBe(false)
    })

    it('returns false for an expired token', () => {
      const token = buildToken({ exp: 0 })
      localStorage.setItem('accessToken', token)
      expect(isAuthenticated()).toBe(false)
    })

    it('returns true for a valid token with future expiration', () => {
      const farFuture = 9999999999
      const token = buildToken({ exp: farFuture })
      localStorage.setItem('accessToken', token)
      expect(isAuthenticated()).toBe(true)
    })
  })

  describe('saveAuth / clearAuth', () => {
    it('persists accessToken, refreshToken and userId to localStorage', () => {
      const token = buildToken({ roles: ['ROLE_MERCHANT'] })
      saveAuth({ accessToken: token, refreshToken: 'rt123', userId: 42 })

      expect(localStorage.getItem('accessToken')).toBe(token)
      expect(localStorage.getItem('refreshToken')).toBe('rt123')
      expect(localStorage.getItem('userId')).toBe('42')
    })

    it('stores roles extracted from the access token', () => {
      const token = buildToken({ roles: ['ROLE_ADMIN'] })
      saveAuth({ accessToken: token, refreshToken: 'rt', userId: 1 })

      expect(localStorage.getItem('userRoles')).toBe('["ROLE_ADMIN"]')
    })

    it('clearAuth removes all auth keys', () => {
      localStorage.setItem('accessToken', 'x')
      localStorage.setItem('refreshToken', 'x')
      localStorage.setItem('userId', 'x')
      localStorage.setItem('userRoles', 'x')

      clearAuth()

      expect(localStorage.getItem('accessToken')).toBeNull()
      expect(localStorage.getItem('refreshToken')).toBeNull()
      expect(localStorage.getItem('userId')).toBeNull()
      expect(localStorage.getItem('userRoles')).toBeNull()
    })
  })

  describe('getPrimaryRole', () => {
    it('returns null when no roles', () => {
      expect(getPrimaryRole()).toBeNull()
    })

    it('returns ROLE_USER for a normal user', () => {
      const token = buildToken({ roles: ['ROLE_USER'] })
      localStorage.setItem('accessToken', token)
      expect(getPrimaryRole()).toBe('ROLE_USER')
    })

    it('prefers ROLE_ADMIN over lower roles', () => {
      const token = buildToken({ roles: ['ROLE_USER', 'ROLE_MERCHANT', 'ROLE_ADMIN'] })
      localStorage.setItem('accessToken', token)
      expect(getPrimaryRole()).toBe('ROLE_ADMIN')
    })

    it('prefers ROLE_MERCHANT over ROLE_USER', () => {
      const token = buildToken({ roles: ['ROLE_USER', 'ROLE_MERCHANT'] })
      localStorage.setItem('accessToken', token)
      expect(getPrimaryRole()).toBe('ROLE_MERCHANT')
    })
  })

  describe('absorbTokenFromUrl', () => {
    it('returns false when no token param in URL', () => {
      // jsdom default URL has no query params
      expect(absorbTokenFromUrl()).toBe(false)
    })

    it('stores a valid token from the URL and cleans the query string', () => {
      const token = buildToken({ exp: 9999999999, roles: ['ROLE_USER'], sub: '5' })

      // jsdom allows replacing the URL
      window.history.replaceState({}, '', `/?token=${encodeURIComponent(token)}`)

      const result = absorbTokenFromUrl()

      expect(result).toBe(true)
      expect(localStorage.getItem('accessToken')).toBe(token)
      expect(localStorage.getItem('userRoles')).toBe('["ROLE_USER"]')
      expect(localStorage.getItem('userId')).toBe('5')
      // URL should be cleaned (no query param)
      expect(window.location.search).toBe('')
    })

    it('returns false for an expired token and does not store it', () => {
      const token = buildToken({ exp: 0, roles: ['ROLE_USER'], sub: '5' })
      window.history.replaceState({}, '', `/?token=${encodeURIComponent(token)}`)

      const result = absorbTokenFromUrl()

      expect(result).toBe(false)
      expect(localStorage.getItem('accessToken')).toBeNull()
    })
  })
})
