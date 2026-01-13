import { apiFetch, ApiError, getCookie } from './http'
import { ElMessage } from 'element-plus'
import type { AdminMe } from './types'

export async function login(password: string): Promise<AdminMe> {
  const xsrf = getCookie('XSRF-TOKEN')
  const body = new URLSearchParams({ username: 'admin', password })
  const res = await fetch('/api/admin/login', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      ...(xsrf ? { 'X-XSRF-TOKEN': xsrf } : {}),
    },
    body,
  })
  if (!res.ok) {
    const contentType = res.headers.get('content-type') || ''
    const errBody = contentType.includes('application/json') ? await res.json().catch(() => null) : await res.text().catch(() => '')
    const message =
      typeof errBody === 'object' && errBody && 'message' in errBody ? String((errBody as any).message) : `HTTP ${res.status}`

    // 显示错误提示
    ElMessage.error(message)

    throw new ApiError(res.status, message, errBody)
  }
  return me()
}

export async function logout(): Promise<void> {
  return apiFetch<void>('/api/admin/logout', { method: 'POST' })
}

export async function me(): Promise<AdminMe> {
  return apiFetch<AdminMe>('/api/admin/me')
}
