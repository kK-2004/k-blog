/**
 * 用户个人信息类型定义
 */

// 性别枚举
export type Gender = 'male' | 'female' | 'other' | 'secret'

// 字段可见性
export type FieldVisibility = 'public' | 'private'

// 用户个人信息
export interface UserProfile {
  // 基本信息
  username: string // 昵称（必填）
  avatar: string | null // 头像 URL
  gender: Gender | null // 性别
  age: number | null // 年龄

  // 联系方式
  email: string | null // 邮箱
  phone: string | null // 手机号
  qq: string | null // QQ 号
  wechat: string | null // 微信号

  // 社交链接
  github: string | null // GitHub 链接
  gitee: string | null // Gitee 链接

  // 字段可见性配置
  visibility: UserProfileVisibility
}

// 字段可见性配置
export interface UserProfileVisibility {
  gender: FieldVisibility
  age: FieldVisibility
  email: FieldVisibility
  phone: FieldVisibility
  qq: FieldVisibility
  wechat: FieldVisibility
  github: FieldVisibility
  gitee: FieldVisibility
}

// 默认可见性配置
export const DEFAULT_VISIBILITY: UserProfileVisibility = {
  gender: 'public',
  age: 'private',
  email: 'private',
  phone: 'private',
  qq: 'private',
  wechat: 'private',
  github: 'public',
  gitee: 'public',
}

// 默认用户信息
export const DEFAULT_USER_PROFILE: UserProfile = {
  username: 'KK',
  avatar: null,
  gender: null,
  age: null,
  email: null,
  phone: null,
  qq: null,
  wechat: null,
  github: null,
  gitee: null,
  visibility: DEFAULT_VISIBILITY,
}
