import { apiFetch } from './http'
import type { MenuItem, QuickAction } from './types'

export type PublicProfile = {
  username: string
  avatarUrl: string | null
  gender: string | null
  age: number | null
  email: string | null
  phone: string | null
  qq: string | null
  wechat: string | null
  github: string | null
  gitee: string | null
}

export async function getPublicProfile(): Promise<PublicProfile> {
  return apiFetch<PublicProfile>('/api/site/profile')
}

export async function listMenuItems(): Promise<MenuItem[]> {
  return apiFetch<MenuItem[]>('/api/site/sidebar-menu-items')
}

export async function listVisibleMenuItems(): Promise<MenuItem[]> {
  return apiFetch<MenuItem[]>('/api/site/sidebar-menu-items/visible')
}

export async function saveMenuItems(items: MenuItem[]): Promise<MenuItem[]> {
  return apiFetch<MenuItem[]>('/api/site/sidebar-menu-items', { method: 'PUT', body: JSON.stringify(items) })
}

export async function listQuickActions(): Promise<QuickAction[]> {
  return apiFetch<QuickAction[]>('/api/site/quick-actions')
}

export async function listVisibleQuickActions(): Promise<QuickAction[]> {
  return apiFetch<QuickAction[]>('/api/site/quick-actions/visible')
}

export async function saveQuickActions(items: QuickAction[]): Promise<QuickAction[]> {
  return apiFetch<QuickAction[]>('/api/site/quick-actions', { method: 'PUT', body: JSON.stringify(items) })
}

export async function listBlogQuickActions(): Promise<QuickAction[]> {
  return apiFetch<QuickAction[]>('/api/site/blog-quick-actions')
}

export async function listVisibleBlogQuickActions(): Promise<QuickAction[]> {
  return apiFetch<QuickAction[]>('/api/site/blog-quick-actions/visible')
}

export async function saveBlogQuickActions(items: QuickAction[]): Promise<QuickAction[]> {
  return apiFetch<QuickAction[]>('/api/site/blog-quick-actions', { method: 'PUT', body: JSON.stringify(items) })
}
