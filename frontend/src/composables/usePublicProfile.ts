/**
 * 公开个人信息管理 Composable
 * 用于未登录用户显示博主的公开信息
 */
import { ref, computed } from 'vue'
import { getPublicProfile } from '@/api/site'

type PublicProfile = {
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

const DEFAULT_PUBLIC_PROFILE: PublicProfile = {
  username: 'Admin',
  avatarUrl: null,
  gender: null,
  age: null,
  email: null,
  phone: null,
  qq: null,
  wechat: null,
  github: null,
  gitee: null,
}

const publicProfile = ref<PublicProfile>(DEFAULT_PUBLIC_PROFILE)
const isLoading = ref(false)
const error = ref<string | null>(null)

export function usePublicProfile() {
  const loadProfile = async () => {
    try {
      isLoading.value = true
      error.value = null
      const data = await getPublicProfile()
      publicProfile.value = data as PublicProfile
      return true
    } catch (e) {
      error.value = '加载公开资料失败'
      publicProfile.value = DEFAULT_PUBLIC_PROFILE
      return false
    } finally {
      isLoading.value = false
    }
  }

  const profile = computed(() => publicProfile.value)

  // 公开资料模式下，所有已加载的字段都是可见的
  const isFieldVisible = () => true

  const isFieldFilled = (field: keyof Omit<PublicProfile, 'username' | 'avatarUrl'>) => {
    if (!publicProfile.value) return false
    const value = publicProfile.value[field]
    return value !== null && value !== undefined && value !== ''
  }

  return {
    profile,
    isLoading,
    error,
    loadProfile,
    isFieldVisible,
    isFieldFilled,
  }
}
