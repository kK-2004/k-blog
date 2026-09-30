import type { Post } from '@/api/types'

const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

export function formatCount(num: number): string {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return String(num)
}

export function postTime(post: Post): number {
  return post.updatedAt ?? post.createdAt ?? 0
}

export function formatRelativeTime(ms: number): string {
  if (!ms || !Number.isFinite(ms)) return ''
  const diff = Date.now() - ms
  if (diff < MINUTE) return '刚刚'
  if (diff < HOUR) return `${Math.floor(diff / MINUTE)} 分钟前`
  if (diff < DAY) return `${Math.floor(diff / HOUR)} 小时前`
  if (diff < 30 * DAY) return `${Math.floor(diff / DAY)} 天前`
  return new Date(ms).toLocaleDateString('zh-CN')
}

export function formatDateTime(value: number | string | null): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
