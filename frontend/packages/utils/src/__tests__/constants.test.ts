import { describe, it, expect } from 'vitest'
import {
  API_BASE,
  PAGE_SIZE,
  PAGE_SIZE_MAX,
  TOKEN_KEY,
  REFRESH_TOKEN_KEY,
  USER_ID_KEY,
  ROLE_KEY,
  ROLE_USER,
  ROLE_MERCHANT,
  ROLE_ADMIN,
  ORDER_STATUS,
  PAY_METHODS,
  COUPON_TYPES,
} from '../constants'

describe('constants', () => {
  it('API_BASE is /api', () => {
    expect(API_BASE).toBe('/api')
  })

  it('has default pagination values', () => {
    expect(PAGE_SIZE).toBe(20)
    expect(PAGE_SIZE_MAX).toBe(100)
  })

  it('has token storage keys', () => {
    expect(TOKEN_KEY).toBe('accessToken')
    expect(REFRESH_TOKEN_KEY).toBe('refreshToken')
    expect(USER_ID_KEY).toBe('userId')
    expect(ROLE_KEY).toBe('userRoles')
  })

  it('has role constants', () => {
    expect(ROLE_USER).toBe('ROLE_USER')
    expect(ROLE_MERCHANT).toBe('ROLE_MERCHANT')
    expect(ROLE_ADMIN).toBe('ROLE_ADMIN')
  })

  it('has order statuses', () => {
    expect(ORDER_STATUS[1]).toBe('待付款')
    expect(ORDER_STATUS[2]).toBe('待发货')
    expect(ORDER_STATUS[3]).toBe('待收货')
    expect(ORDER_STATUS[4]).toBe('已完成')
    expect(ORDER_STATUS[5]).toBe('已取消')
    expect(ORDER_STATUS[6]).toBe('已退款')
  })

  it('has payment methods', () => {
    expect(PAY_METHODS[1]).toBe('支付宝')
    expect(PAY_METHODS[2]).toBe('微信支付')
  })

  it('has coupon types', () => {
    expect(COUPON_TYPES[1]).toBe('满减券')
    expect(COUPON_TYPES[2]).toBe('折扣券')
    expect(COUPON_TYPES[3]).toBe('直减券')
  })
})
