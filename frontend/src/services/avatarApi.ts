/**
 * 头像 API 服务
 * 提供头像上传、删除、获取等功能
 * 预留 OSS 预签名直链接口
 */

import { API_CONFIG } from '@/config/api'

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
  // 验证文件类型
  if (!API_CONFIG.avatar.allowedTypes.includes(file.type)) {
    throw new Error('不支持的文件类型，请上传 JPG、PNG、GIF 或 WebP 格式的图片')
  }

  // 验证文件大小
  if (file.size > API_CONFIG.avatar.maxSize) {
    throw new Error(`图片大小不能超过 ${API_CONFIG.avatar.maxSize / 1024 / 1024}MB`)
  }

  try {
    // TODO: 第一步 - 获取预签名上传 URL
    // const presignResponse = await fetch(getApiUrl(API_CONFIG.avatar.uploadUrl), {
    //   method: 'POST',
    //   headers: {
    //     'Content-Type': 'application/json',
    //     // 如果需要认证，添加 token
    //     // 'Authorization': `Bearer ${token}`,
    //   },
    //   body: JSON.stringify({
    //     filename: file.name,
    //     contentType: file.type,
    //     size: file.size,
    //   }),
    // })
    //
    // if (!presignResponse.ok) {
    //   throw new Error('获取上传链接失败')
    // }
    //
    // const { uploadUrl, key } = await presignResponse.json()

    // TODO: 第二步 - 直接上传到 OSS
    // const uploadResponse = await fetch(uploadUrl, {
    //   method: 'PUT',
    //   body: file,
    //   headers: {
    //     'Content-Type': file.type,
    //   },
    // })
    //
    // if (!uploadResponse.ok) {
    //   throw new Error('上传失败')
    // }

    // TODO: 第三步 - 通知后端上传成功（可选）
    // await fetch(getApiUrl('/avatar/upload/success'), {
    //   method: 'POST',
    //   headers: {
    //     'Content-Type': 'application/json',
    //   },
    //   body: JSON.stringify({ key }),
    // })

    // 🔴 临时方案：转换为 base64 返回（开发测试用）
    // 后续替换为 OSS URL
    const reader = new FileReader()
    const dataUrl = await new Promise<string>((resolve, reject) => {
      reader.onload = (e) => resolve(e.target?.result as string)
      reader.onerror = () => reject(new Error('文件读取失败'))
      reader.readAsDataURL(file)
    })

    // 模拟 API 延迟
    await new Promise((resolve) => setTimeout(resolve, 500))

    return {
      url: dataUrl,
      key: file.name,
      message: '上传成功',
    }
  } catch (error) {
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
    // TODO: 调用后端删除接口
    // const response = await fetch(getApiUrl(API_CONFIG.avatar.deleteUrl), {
    //   method: 'DELETE',
    //   headers: {
    //     // 'Authorization': `Bearer ${token}`,
    //   },
    // })
    //
    // if (!response.ok) {
    //   throw new Error('删除失败')
    // }
    //
    // return await response.json()

    // 🔴 临时方案：直接返回成功
    await new Promise((resolve) => setTimeout(resolve, 300))
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
    // TODO: 调用后端获取接口
    // const response = await fetch(getApiUrl(API_CONFIG.avatar.getUrl), {
    //   headers: {
    //     // 'Authorization': `Bearer ${token}`,
    //   },
    // })
    //
    // if (!response.ok) {
    //   throw new Error('获取头像信息失败')
    // }
    //
    // return await response.json()

    // 🔴 临时方案：返回空（表示没有自定义头像）
    await new Promise((resolve) => setTimeout(resolve, 200))
    return { url: null }
  } catch (error) {
    // 获取头像信息失败时返回 null，不抛出错误
    return { url: null }
  }
}
