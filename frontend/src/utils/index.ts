/** 格式化价格（分 → 元，或直接保留两位） */
export function formatPrice(price: number): string {
  return `¥${price.toFixed(2)}`
}

/** 格式化日期时间 */
export function formatDateTime(iso: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** 格式化日期 */
export function formatDate(iso: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}
