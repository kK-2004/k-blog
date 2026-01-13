/**
 * 用户个人信息管理 Composable
 * 提供 API 接口调用，预留后端集成
 */
import { ref, computed } from 'vue'
import type { UserProfile } from '@/types/user'
import { DEFAULT_USER_PROFILE } from '@/types/user'

// 全局状态
const userProfile = ref<UserProfile>({ ...DEFAULT_USER_PROFILE })
const isLoading = ref(false)
const error = ref<string | null>(null)

export function useUserProfile() {
  // 从服务器加载用户信息
  const loadProfile = async () => {
    try {
      isLoading.value = true
      error.value = null

      // TODO: 调用后端接口
      // const response = await fetch('/api/user/profile', {
      //   headers: { 'Authorization': `Bearer ${token}` }
      // })
      // const data = await response.json()
      // userProfile.value = data

      // 🔴 临时方案：从 localStorage 加载（开发测试用）
      const saved = localStorage.getItem('user-profile')
      if (saved) {
        userProfile.value = JSON.parse(saved)
      }

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

      // 更新本地状态
      userProfile.value = { ...userProfile.value, ...profile }

      // TODO: 调用后端接口
      // await fetch('/api/user/profile', {
      //   method: 'PUT',
      //   headers: {
      //     'Content-Type': 'application/json',
      //     'Authorization': `Bearer ${token}`,
      //   },
      //   body: JSON.stringify(profile),
      // })

      // 🔴 临时方案：保存到 localStorage（开发测试用）
      localStorage.setItem('user-profile', JSON.stringify(userProfile.value))

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

    // TODO: 调用后端接口保存可见性
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
