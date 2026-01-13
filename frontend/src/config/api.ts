/**
 * API 配置文件
 * 用于管理后端接口，便于后续接入阿里云 OSS
 */

export const API_CONFIG = {
  // 基础 API 地址
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',

  // 头像相关接口
  avatar: {
    // 上传头像（获取 OSS 预签名 URL）
    uploadUrl: '/avatar/upload',

    // 删除头像
    deleteUrl: '/avatar/delete',

    // 获取当前头像 URL
    getUrl: '/avatar',

    // 文件大小限制（字节）
    maxSize: 2 * 1024 * 1024, // 2MB

    // 允许的文件类型
    allowedTypes: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
  },
}

/**
 * 获取完整的 API URL
 */
export function getApiUrl(path: string): string {
  return `${API_CONFIG.baseURL}${path}`
}

/**
 * 阿里云 OSS 配置（未来使用）
 * 预留 OSS 相关配置
 */
export const OSS_CONFIG = {
  // OSS 访问域名
  endpoint: import.meta.env.VITE_OSS_ENDPOINT || '',
  // Bucket 名称
  bucket: import.meta.env.VITE_OSS_BUCKET || '',
  // 上传目录前缀
  uploadPath: 'avatars/',
  // CDN 域名（如果有的话）
  cdnDomain: import.meta.env.VITE_OSS_CDN_DOMAIN || '',
}

/**
 * 获取 OSS 完整 URL
 */
export function getOssUrl(objectKey: string): string {
  if (OSS_CONFIG.cdnDomain) {
    return `https://${OSS_CONFIG.cdnDomain}/${OSS_CONFIG.uploadPath}${objectKey}`
  }
  if (OSS_CONFIG.endpoint) {
    return `https://${OSS_CONFIG.bucket}.${OSS_CONFIG.endpoint}/${OSS_CONFIG.uploadPath}${objectKey}`
  }
  return objectKey
}
