import { apiFetch } from './http'
import type { MenuItem } from './types'

export type PublicProfile = {
  avatarUrl: string | null
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

