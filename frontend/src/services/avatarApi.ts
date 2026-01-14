/**
 * 头像 API 服务（后台管理端）
 * 流程：后端预签名 -> 前端直传 OSS -> 回写头像 URL（落库）
 */

import { me as apiMe, presignAvatar, updateProfile, deleteAvatar as apiDeleteAvatar } from '@/api/admin'
import { apiFetch } from '@/api/http'

export interface UploadAvatarResponse {
  // 头像 URL（OSS 地址）
  url: string
  // 如果需要后续操作，返回的 key
  key?: string
  // 消息
  message?: string
}

export interface DeleteAvatarResponse {
  success: boolean
  message?: string
}

export interface AvatarInfo {
  // 当前头像 URL
  url: string | null
  // 上传时间
  uploadedAt?: string
}

/**
 * 上传头像
 *
 * 流程：
 * 1. 前端请求后端获取 OSS 预签名上传 URL
 * 2. 前端直接使用 PUT 请求上传文件到 OSS
 * 3. 上传成功后通知后端（可选，用于记录）
 *
 * @param file 图片文件
 * @returns Promise<UploadAvatarResponse>
 */
export async function uploadAvatar(file: File): Promise<UploadAvatarResponse> {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  const maxSize = 2 * 1024 * 1024
  if (!allowedTypes.includes(file.type)) throw new Error('不支持的文件类型，请上传 JPG、PNG、GIF 或 WebP')
  if (file.size > maxSize) throw new Error('图片大小不能超过 2MB')

  try {
    const presigned = await presignAvatar({ filename: file.name, contentType: file.type })

    console.log('[Avatar Upload] 预签名URL:', presigned.uploadUrl)
    console.log('[Avatar Upload] 文件信息:', { name: file.name, type: file.type, size: file.size })

    const uploadRes = await fetch(presigned.uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': file.type , 'x-oss-object-acl': 'public-read'},
      body: file,
    })

    console.log('[Avatar Upload] 响应状态:', uploadRes.status, uploadRes.statusText)

    if (!uploadRes.ok) {
      const responseText = await uploadRes.text().catch(() => '无法读取响应内容')
      console.error('[Avatar Upload] 上传失败，响应内容:', responseText)
      throw new Error(`上传失败: HTTP ${uploadRes.status} - ${responseText}`)
    }

    await updateProfile({ avatarUrl: presigned.url, avatarKey: presigned.key })

    return {
      url: presigned.url,
      key: presigned.key,
      message: '上传成功',
    }
  } catch (error) {
    console.error('[Avatar Upload] 上传异常:', error)
    throw error
  }
}

/**
 * 删除头像
 *
 * @returns Promise<DeleteAvatarResponse>
 */
export async function deleteAvatar(): Promise<DeleteAvatarResponse> {
  try {
    await apiDeleteAvatar()
    return { success: true, message: '删除成功' }
  } catch (error) {
    throw error
  }
}

/**
 * 获取当前头像信息
 *
 * @returns Promise<AvatarInfo>
 */
export async function getAvatarInfo(): Promise<AvatarInfo> {
  try {
    const admin = await apiMe()
    return { url: admin.avatarUrl || null }
  } catch (error) {
    try {
      const publicProfile = await apiFetch<{ avatarUrl?: string | null }>('/api/site/profile')
      return { url: publicProfile.avatarUrl || null }
    } catch {
      return { url: null }
    }
  }
}
