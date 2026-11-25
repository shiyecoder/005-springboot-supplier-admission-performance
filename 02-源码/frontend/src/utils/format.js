/**
 * 全局时间格式化工具。全系统统一调用，避免各页面重复实现、格式不一致。
 * 后端返回的时间可能是 "2026-10-31 15:20:00" 或 ISO 字符串，这里统一兜底。
 */
export function formatTime(value) {
  if (!value) return '—'
  const d = toDate(value)
  if (!d) return String(value)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

export function formatDate(value) {
  if (!value) return '—'
  const d = toDate(value)
  if (!d) return String(value)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

export function timeAgo(value) {
  if (!value) return '—'
  const d = toDate(value)
  if (!d) return String(value)
  const diff = Date.now() - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  return `${Math.floor(diff / 86_400_000)} 天前`
}

function toDate(value) {
  if (value instanceof Date) return value
  if (typeof value === 'number') return new Date(value)
  const s = String(value).replace('T', ' ').replace(/-/g, '/')
  const d = new Date(s)
  return Number.isNaN(d.getTime()) ? null : d
}
