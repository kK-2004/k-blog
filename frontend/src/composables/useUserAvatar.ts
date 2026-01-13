/**
 * 头像管理 Composable
 * 提供 API 接口调用，预留 OSS 集成
 */
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadAvatar as apiUploadAvatar, deleteAvatar as apiDeleteAvatar, getAvatarInfo } from '@/services/avatarApi'

// 全局状态
const avatarUrl = ref<string | null>(null)
const isLoading = ref(false)
const error = ref<string | null>(null)

export function useUserAvatar() {
  // 从服务器加载头像信息
  const loadAvatar = async () => {
    try {
      isLoading.value = true
      error.value = null
      const info = await getAvatarInfo()
      avatarUrl.value = info.url
      return true
    } catch (e) {
      const message = e instanceof Error ? e.message : '加载头像失败'
      error.value = message
      ElMessage.error(message)
      return false
    } finally {
      isLoading.value = false
    }
  }

  // 设置头像 URL（用于手动设置）
  const setAvatar = (url: string) => {
    avatarUrl.value = url
    error.value = null
  }

  // 清除头像
  const clearAvatar = () => {
    avatarUrl.value = null
    error.value = null
  }

  // 上传头像
  const uploadAvatar = async (file: File): Promise<string> => {
    isLoading.value = true
    error.value = null

    try {
      const response = await apiUploadAvatar(file)
      avatarUrl.value = response.url
      return response.url
    } catch (e) {
      const message = e instanceof Error ? e.message : '上传失败'
      error.value = message
      ElMessage.error(message)
      throw e
    } finally {
      isLoading.value = false
    }
  }

  // 删除头像
  const removeAvatar = async () => {
    isLoading.value = true
    error.value = null

    try {
      await apiDeleteAvatar()
      avatarUrl.value = null
      return true
    } catch (e) {
      const message = e instanceof Error ? e.message : '删除失败'
      error.value = message
      ElMessage.error(message)
      throw e
    } finally {
      isLoading.value = false
    }
  }

  // 获取当前头像 URL
  const currentAvatar = computed(() => avatarUrl.value)

  // 是否有自定义头像
  const hasCustomAvatar = computed(() => !!avatarUrl.value)

  return {
    avatarUrl: currentAvatar,
    hasCustomAvatar,
    isLoading,
    error,
    loadAvatar,
    setAvatar,
    clearAvatar,
    uploadAvatar,
    removeAvatar,
  }
}
