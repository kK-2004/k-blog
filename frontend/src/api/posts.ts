import { apiFetch } from './http'
import type { HotPost, Post } from './types'

export type CreatePostRequest = {
  title: string
  content: string
  pinned?: boolean
}

export async function listPosts(options?: { cache?: boolean }): Promise<Post[]> {
  return apiFetch<Post[]>('/api/posts', {}, options)
}

export async function listPostsPage(page: number, size = 5): Promise<Post[]> {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  return apiFetch<Post[]>(`/api/posts?${params.toString()}`)
}

export async function getPost(id: number): Promise<Post> {
  return apiFetch<Post>(`/api/posts/${id}`)
}

export async function createPost(req: CreatePostRequest): Promise<Post> {
  return apiFetch<Post>('/api/posts', { method: 'POST', body: JSON.stringify(req) })
}

export async function updatePost(id: number, post: Post): Promise<Post> {
  return apiFetch<Post>(`/api/posts/${id}`, { method: 'PUT', body: JSON.stringify(post) })
}

export async function deletePost(id: number): Promise<void> {
  return apiFetch<void>(`/api/posts/${id}`, { method: 'DELETE' })
}

export async function incrementViews(id: number): Promise<Post> {
  return apiFetch<Post>(`/api/posts/${id}/views`, { method: 'POST' })
}

export async function incrementLikes(id: number): Promise<Post> {
  return apiFetch<Post>(`/api/posts/${id}/likes`, { method: 'POST' })
}

export async function incrementComments(id: number): Promise<Post> {
  return apiFetch<Post>(`/api/posts/${id}/comments/increment`, { method: 'POST' })
}

export async function getHotPosts(): Promise<HotPost[]> {
  return apiFetch<HotPost[]>('/api/posts/hot', {}, { cache: true, cacheMaxAge: 30000 })
}
