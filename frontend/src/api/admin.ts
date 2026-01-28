import { apiFetch, ApiError, getCookie } from './http'
import { ElMessage } from 'element-plus'
import type { AdminMe, LastLoginInfo } from './types'

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

export async function getLastLoginInfo(): Promise<LastLoginInfo> {
  return apiFetch<LastLoginInfo>('/api/public/last-login', {}, { cacheMaxAge: 60000 })
}

export async function logout(): Promise<void> {
  return apiFetch<void>('/api/admin/logout', { method: 'POST' })
}

export async function me(): Promise<AdminMe> {
  // 缓存 30 秒，登录状态信息变化不频繁
  return apiFetch<AdminMe>('/api/admin/me', {}, { cacheMaxAge: 30000 })
}

export type UpdateAdminProfileRequest = {
  username?: string
  avatarUrl?: string
  avatarKey?: string
  gender?: string | null
  age?: number | null
  email?: string | null
  phone?: string | null
  qq?: string | null
  wechat?: string | null
  github?: string | null
  gitee?: string | null
  visibility?: AdminMe['visibility']
}

export async function updateProfile(req: UpdateAdminProfileRequest): Promise<AdminMe> {
  return apiFetch<AdminMe>('/api/admin/profile', { method: 'PUT', body: JSON.stringify(req) })
}

export type PresignAvatarRequest = {
  filename: string
  contentType: string
}

export type PresignAvatarResponse = {
  uploadUrl: string
  url: string
  key: string
  expireSeconds: number
}

export async function presignAvatar(req: PresignAvatarRequest): Promise<PresignAvatarResponse> {
  return apiFetch<PresignAvatarResponse>('/api/admin/avatar/presign', { method: 'POST', body: JSON.stringify(req) })
}

export async function deleteAvatar(): Promise<void> {
  return apiFetch<void>('/api/admin/avatar', { method: 'DELETE' })
}

export type UpdatePasswordRequest = {
  oldPassword: string
  newPassword: string
}

export async function updatePassword(req: UpdatePasswordRequest): Promise<void> {
  return apiFetch<void>('/api/admin/password', { method: 'PUT', body: JSON.stringify(req) })
}
