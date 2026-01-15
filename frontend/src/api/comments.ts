import { apiFetch } from './http'
import type { PostComment, PostCommentReply } from './types'

export type CreatePostCommentRequest = {
  user?: string
  text: string
}

export type CreatePostCommentReplyRequest = {
  user?: string
  text: string
  toUser?: string
}

export type LikeResponse = {
  likes: number
}

export async function listPostComments(postId: number): Promise<PostComment[]> {
  return apiFetch<PostComment[]>(`/api/posts/${postId}/comments`)
}

export async function createPostComment(postId: number, req: CreatePostCommentRequest): Promise<PostComment> {
  return apiFetch<PostComment>(`/api/posts/${postId}/comments`, { method: 'POST', body: JSON.stringify(req) })
}

export async function createPostCommentReply(
  postId: number,
  rootCommentId: number,
  req: CreatePostCommentReplyRequest,
): Promise<PostCommentReply> {
  return apiFetch<PostCommentReply>(`/api/posts/${postId}/comments/${rootCommentId}/replies`, {
    method: 'POST',
    body: JSON.stringify(req),
  })
}

export async function likePostComment(postId: number, commentId: number): Promise<LikeResponse> {
  return apiFetch<LikeResponse>(`/api/posts/${postId}/comments/${commentId}/likes`, { method: 'POST' })
}

export async function deletePostComment(postId: number, commentId: number): Promise<void> {
  return apiFetch<void>(`/api/posts/${postId}/comments/${commentId}`, { method: 'DELETE' })
}
