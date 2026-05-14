// 格式化金额
export function formatPrice(price: number | string): string {
  const n = typeof price === 'string' ? parseFloat(price) : price
  if (isNaN(n)) return '¥0.00'
  return '¥' + n.toFixed(2)
}

// 格式化日期
export function formatDate(date: string | Date | null, fmt = 'YYYY-MM-DD HH:mm:ss'): string {
  if (!date) return ''
  const d = typeof date === 'string' ? new Date(date) : date
  const o: Record<string, number> = {
    'Y+': d.getFullYear(),
    'M+': d.getMonth() + 1,
    'D+': d.getDate(),
    'H+': d.getHours(),
    'm+': d.getMinutes(),
    's+': d.getSeconds(),
  }
  for (const [k, v] of Object.entries(o)) {
    const reg = new RegExp('(' + k + ')')
    if (reg.test(fmt)) {
      fmt = fmt.replace(reg, String(v).padStart(k.length > 2 ? 2 : k.length, '0'))
    }
  }
  return fmt
}

// 格式化倒计时 (秒 → HH:MM:SS)
export function formatCountdown(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}
