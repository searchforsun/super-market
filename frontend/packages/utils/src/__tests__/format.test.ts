import { describe, it, expect } from 'vitest'
import { formatPrice, formatDate, formatCountdown } from '../format'

describe('formatPrice', () => {
  it('formats a number with ¥ and two decimal places', () => {
    expect(formatPrice(0)).toBe('¥0.00')
    expect(formatPrice(10)).toBe('¥10.00')
    expect(formatPrice(10.5)).toBe('¥10.50')
    expect(formatPrice(1234.56)).toBe('¥1234.56')
  })

  it('formats a string input correctly', () => {
    expect(formatPrice('0')).toBe('¥0.00')
    expect(formatPrice('99.9')).toBe('¥99.90')
    expect(formatPrice('100')).toBe('¥100.00')
  })

  it('returns ¥0.00 for NaN / invalid input', () => {
    expect(formatPrice(NaN)).toBe('¥0.00')
    expect(formatPrice('abc')).toBe('¥0.00')
    expect(formatPrice('')).toBe('¥0.00')
  })

  it('rounds to two decimal places', () => {
    expect(formatPrice(10.999)).toBe('¥11.00')
    expect(formatPrice(10.001)).toBe('¥10.00')
    expect(formatPrice(3.14159)).toBe('¥3.14')
  })
})

describe('formatDate', () => {
  it('returns empty string for null / undefined', () => {
    expect(formatDate(null)).toBe('')
    expect(formatDate(undefined as unknown as null)).toBe('')
  })

  it('formats a Date object with the default format', () => {
    const d = new Date(2025, 0, 5, 9, 8, 7) // Jan 5, 2025, 09:08:07
    const result = formatDate(d)
    expect(result).toBe('2025-01-05 09:08:07')
  })

  it('formats a Date object with a custom format', () => {
    const d = new Date(2025, 11, 25, 14, 30, 45)
    expect(formatDate(d, 'YYYY-MM-DD')).toBe('2025-12-25')
    expect(formatDate(d, 'HH:mm:ss')).toBe('14:30:45')
    expect(formatDate(d, 'YYYY/MM/DD HH:mm')).toBe('2025/12/25 14:30')
  })

  it('formats an ISO date string', () => {
    const result = formatDate('2025-06-15T10:20:30')
    expect(result).toMatch(/2025-06-15/)
  })

  it('handles single-digit months/days/hours with padding', () => {
    const d = new Date(2025, 0, 1, 1, 2, 3)
    expect(formatDate(d)).toBe('2025-01-01 01:02:03')
  })
})

describe('formatCountdown', () => {
  it('formats zero seconds', () => {
    expect(formatCountdown(0)).toBe('00:00:00')
  })

  it('formats seconds only (< 60)', () => {
    expect(formatCountdown(5)).toBe('00:00:05')
    expect(formatCountdown(59)).toBe('00:00:59')
  })

  it('formats minutes and seconds', () => {
    expect(formatCountdown(60)).toBe('00:01:00')
    expect(formatCountdown(61)).toBe('00:01:01')
    expect(formatCountdown(3599)).toBe('00:59:59')
  })

  it('formats hours, minutes and seconds', () => {
    expect(formatCountdown(3600)).toBe('01:00:00')
    expect(formatCountdown(3661)).toBe('01:01:01')
    expect(formatCountdown(86399)).toBe('23:59:59')
  })

  it('handles large values beyond 24 hours', () => {
    expect(formatCountdown(90000)).toBe('25:00:00')
    expect(formatCountdown(100000)).toBe('27:46:40')
  })
})
