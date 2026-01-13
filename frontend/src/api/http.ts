import { ElMessage } from 'element-plus'

export class ApiError extends Error {
  status: number
  body: unknown

  constructor(status: number, message: string, body: unknown) {
    super(message)
    this.status = status
    this.body = body
  }
}

export function getCookie(name: string): string | null {
  const parts = document.cookie.split(';').map((x) => x.trim())
  const prefix = `${encodeURIComponent(name)}=`
  for (const part of parts) {
    if (part.startsWith(prefix)) {
      return decodeURIComponent(part.slice(prefix.length))
    }
  }
  return null
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const url = path.startsWith('/') ? path : `/${path}`
  const method = (init.method || 'GET').toUpperCase()
  const isUnsafe = !['GET', 'HEAD', 'OPTIONS'].includes(method)
  const xsrf = isUnsafe ? getCookie('XSRF-TOKEN') : null
  const res = await fetch(url, {
    ...init,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(xsrf ? { 'X-XSRF-TOKEN': xsrf } : {}),
      ...(init.headers || {}),
    },
  })

  if (res.status === 204) return undefined as T

  const contentType = res.headers.get('content-type') || ''
  const body = contentType.includes('application/json') ? await res.json().catch(() => null) : await res.text().catch(() => '')

  if (!res.ok) {
    const message =
      typeof body === 'object' && body && 'message' in body
        ? String((body as any).message)
        : typeof body === 'object' && body && 'detail' in body
          ? String((body as any).detail)
        : `HTTP ${res.status}`

    // 对于特定的 API 错误，静默处理不显示错误消息
    const isSilentError =
      // 登录状态检查接口的 401 错误（用户未登录是正常情况）
      (path === '/api/admin/me' && res.status === 401) ||
      // 其他需要静默的错误可以在这里添加
      false

    if (!isSilentError) {
      // 统一显示错误提示
      ElMessage.error(message)
    }

    // 401 未登录错误处理：在后台管理页面跳转到登录页，但不在登录页跳转
    if (res.status === 401) {
      const hash = window.location.hash.slice(1) // Remove #
      const currentPath = hash.startsWith('/') ? hash.slice(1) : hash

      // 只有在 admin 或 settings 页面时才跳转到登录页
      // 在 login 页面（密码错误）或 blog 页面时不跳转
      if (currentPath === 'admin' || currentPath === 'settings') {
        window.location.hash = '#/login'
      }
    }

    throw new ApiError(res.status, message, body)
  }

  return body as T
}
