/**
 * 用户个人信息管理 Composable
 * 后台管理端个人中心（落库）
 */
import { ref, computed } from 'vue'
import type { UserProfile } from '@/types/user'
import { DEFAULT_USER_PROFILE, DEFAULT_VISIBILITY } from '@/types/user'
import { me as apiMe, updateProfile as apiUpdateProfile } from '@/api/admin'

// 全局状态
const userProfile = ref<UserProfile>({ ...DEFAULT_USER_PROFILE })
const isLoading = ref(false)
const error = ref<string | null>(null)

const normalizeVisibility = (v: any): UserProfile['visibility'] => {
  const safe = (x: any, fallback: any) => (x === 'public' || x === 'private' ? x : fallback)
  return {
    gender: safe(v?.gender, DEFAULT_VISIBILITY.gender),
    age: safe(v?.age, DEFAULT_VISIBILITY.age),
    email: safe(v?.email, DEFAULT_VISIBILITY.email),
    phone: safe(v?.phone, DEFAULT_VISIBILITY.phone),
    qq: safe(v?.qq, DEFAULT_VISIBILITY.qq),
    wechat: safe(v?.wechat, DEFAULT_VISIBILITY.wechat),
    github: safe(v?.github, DEFAULT_VISIBILITY.github),
    gitee: safe(v?.gitee, DEFAULT_VISIBILITY.gitee),
  }
}

const applyAdminMeToProfile = (admin: Awaited<ReturnType<typeof apiMe>>) => {
  userProfile.value = {
    username: admin.username,
    avatar: admin.avatarUrl || null,
    gender: (admin.gender as any) || null,
    age: admin.age ?? null,
    email: admin.email ?? null,
    phone: admin.phone ?? null,
    qq: admin.qq ?? null,
    wechat: admin.wechat ?? null,
    github: admin.github ?? null,
    gitee: admin.gitee ?? null,
    visibility: normalizeVisibility(admin.visibility),
  }
}

export function useUserProfile() {
  // 从服务器加载用户信息
  const loadProfile = async () => {
    try {
      isLoading.value = true
      error.value = null
      const admin = await apiMe()
      applyAdminMeToProfile(admin)

      return true
    } catch (e) {
      // 错误已在 http.ts 中通过 ElMessage 统一处理
      return false
    } finally {
      isLoading.value = false
    }
  }

  // 保存用户信息
  const saveProfile = async (profile: Partial<UserProfile>): Promise<boolean> => {
    try {
      isLoading.value = true
      error.value = null
      const merged: UserProfile = { ...userProfile.value, ...profile }
      const req: any = {}
      if (profile.username !== undefined) req.username = merged.username
      if (profile.avatar !== undefined) req.avatarUrl = merged.avatar || ''
      if (profile.gender !== undefined) req.gender = merged.gender
      if (profile.age !== undefined) req.age = merged.age
      if (profile.email !== undefined) req.email = merged.email
      if (profile.phone !== undefined) req.phone = merged.phone
      if (profile.qq !== undefined) req.qq = merged.qq
      if (profile.wechat !== undefined) req.wechat = merged.wechat
      if (profile.github !== undefined) req.github = merged.github
      if (profile.gitee !== undefined) req.gitee = merged.gitee
      if (profile.visibility !== undefined) req.visibility = merged.visibility as any

      const admin = await apiUpdateProfile(req)
      applyAdminMeToProfile(admin)

      return true
    } catch (e) {
      // 错误已在 http.ts 中通过 ElMessage 统一处理
      return false
    } finally {
      isLoading.value = false
    }
  }

  // 更新字段可见性
  const updateVisibility = async (field: keyof UserProfile['visibility'], visibility: 'public' | 'private') => {
    userProfile.value.visibility[field] = visibility

    await saveProfile({ visibility: userProfile.value.visibility })
  }

  // 获取当前用户信息
  const profile = computed(() => userProfile.value)

  // 检查字段是否可见
  const isFieldVisible = (field: keyof UserProfile['visibility']) => {
    return userProfile.value.visibility[field] === 'public'
  }

  // 检查字段是否已填写
  const isFieldFilled = (field: keyof Omit<UserProfile, 'username' | 'avatar' | 'visibility'>) => {
    const value = userProfile.value[field]
    return value !== null && value !== undefined && value !== ''
  }

  return {
    profile,
    isLoading,
    error,
    loadProfile,
    saveProfile,
    updateVisibility,
    isFieldVisible,
    isFieldFilled,
  }
}
