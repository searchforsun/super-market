import { TOKEN_KEY, REFRESH_TOKEN_KEY, USER_ID_KEY, ROLE_KEY, ROLE_USER, APP_URLS } from './constants'

/** Decode JWT payload without verifying signature */
function decodeJwt(token: string): Record<string, any> | null {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return null
  }
}

/** Get roles from stored JWT token */
export function getRoles(): string[] {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return []
  const decoded = decodeJwt(token)
  return decoded?.roles || []
}

/** Check if user has a specific role */
export function hasRole(role: string): boolean {
  return getRoles().includes(role)
}

/** Check if user is authenticated */
export function isAuthenticated(): boolean {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return false
  const decoded = decodeJwt(token)
  if (!decoded?.exp) return false
  return decoded.exp * 1000 > Date.now()
}

/** Save auth data from login response */
export function saveAuth(data: { accessToken: string; refreshToken: string; userId: string | number }) {
  localStorage.setItem(TOKEN_KEY, data.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
  localStorage.setItem(USER_ID_KEY, String(data.userId))
  const decoded = decodeJwt(data.accessToken)
  if (decoded?.roles) {
    localStorage.setItem(ROLE_KEY, JSON.stringify(decoded.roles))
  }
}

/** Clear all auth data */
export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_ID_KEY)
  localStorage.removeItem(ROLE_KEY)
}

/** Get the primary role for redirect after login */
export function getPrimaryRole(): string | null {
  const roles = getRoles()
  if (roles.includes('ROLE_ADMIN')) return 'ROLE_ADMIN'
  if (roles.includes('ROLE_MERCHANT')) return 'ROLE_MERCHANT'
  if (roles.includes('ROLE_USER')) return 'ROLE_USER'
  return null
}

/** Get the appropriate app URL for a given role */
export function getAppUrl(role: string): string {
  return APP_URLS[role] || APP_URLS[ROLE_USER]
}

/** Redirect to the appropriate app based on role */
export function redirectByRole() {
  const role = getPrimaryRole()
  if (role) {
    const token = localStorage.getItem(TOKEN_KEY)
    const url = new URL(getAppUrl(role))
    if (token) url.searchParams.set('token', token)
    window.location.href = url.toString()
  }
}

/** Absorb token from URL query param (for cross-app SSO) */
export function absorbTokenFromUrl() {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('token')
  if (!token) return false

  const decoded = decodeJwt(token)
  if (!decoded?.exp || decoded.exp * 1000 <= Date.now()) {
    window.history.replaceState({}, '', window.location.pathname)
    return false
  }

  localStorage.setItem(TOKEN_KEY, token)
  if (decoded.roles) localStorage.setItem(ROLE_KEY, JSON.stringify(decoded.roles))
  if (decoded.sub) localStorage.setItem(USER_ID_KEY, decoded.sub)

  // Clean URL
  window.history.replaceState({}, '', window.location.pathname)
  return true
}
