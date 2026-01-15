<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserAvatar } from '@/composables/useUserAvatar'
import { useMessage } from '@/composables/useMessage'

const { avatarUrl, hasCustomAvatar, uploadAvatar, removeAvatar, loadAvatar, isLoading } = useUserAvatar(true)
const { success: showSuccess, error: showError } = useMessage()

const fileInput = ref<HTMLInputElement | null>(null)
const isDragging = ref(false)
const previewUrl = ref<string | null>(null)
const isUploading = ref(false)

onMounted(async () => {
  // 加载头像信息
  await loadAvatar()
  previewUrl.value = avatarUrl.value
})

const handleClick = () => {
  fileInput.value?.click()
}

const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    await processFile(file)
  }
  // 重置 input 以便允许选择同一文件
  target.value = ''
}

const handleDrop = async (event: DragEvent) => {
  event.preventDefault()
  isDragging.value = false

  const file = event.dataTransfer?.files[0]
  if (file) {
    await processFile(file)
  }
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
  isDragging.value = true
}

const handleDragLeave = () => {
  isDragging.value = false
}

const processFile = async (file: File) => {
  isUploading.value = true

  try {
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      showError('请选择图片文件（JPG、PNG、GIF 等）')
      return
    }

    // 验证文件大小（2MB）
    if (file.size > 2 * 1024 * 1024) {
      showError('图片大小不能超过 2MB')
      return
    }

    // 创建预览
    const reader = new FileReader()
    reader.onload = (e) => {
      previewUrl.value = e.target?.result as string
    }
    reader.readAsDataURL(file)

    // 上传到服务器（后续会替换为 OSS 上传）
    await uploadAvatar(file)
    showSuccess('头像上传成功！')
  } catch (err) {
    // 错误已在 useUserAvatar 中通过 ElMessage 处理
  } finally {
    isUploading.value = false
  }
}

const handleRemove = async () => {
  try {
    await removeAvatar()
    previewUrl.value = null
    showSuccess('头像已删除')
  } catch (err) {
    // 错误已在 useUserAvatar 中通过 ElMessage 处理
  }
}
</script>

<template>
  <div class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden">
    <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center px-6">
      <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
        <i class="ph ph-user-circle text-lg"></i>
        头像设置
      </h2>
    </div>

    <div class="p-6">
      <div class="flex items-start gap-6">
        <!-- 头像预览 -->
        <div class="flex-shrink-0">
          <div class="relative group">
            <div
              class="w-20 h-20 rounded-full overflow-hidden bg-gray-200 dark:bg-gray-700 border-4 border-white dark:border-gray-600 shadow-lg"
            >
              <img
                v-if="previewUrl"
                :src="previewUrl"
                alt="Avatar Preview"
                class="w-full h-full object-cover"
              />
              <div v-else class="w-full h-full flex items-center justify-center text-3xl font-bold text-gray-400">
                K
              </div>
            </div>
            <!-- 加载状态 -->
            <div
              v-if="isLoading || isUploading"
              class="absolute inset-0 bg-black/50 flex items-center justify-center"
            >
              <i class="ph ph-spinner animate-spin text-white text-2xl"></i>
            </div>
          </div>
        </div>

        <!-- 上传区域 -->
        <div class="flex-1 space-y-4">
          <!-- 提示信息 -->
          <div class="space-y-2">
            <p class="text-sm text-gray-600 dark:text-gray-300">
              上传自定义头像，支持 JPG、PNG、GIF 格式，最大 2MB
            </p>
            <p class="text-xs text-gray-400">
              后续将接入阿里云 OSS，支持更快的上传速度
            </p>
          </div>

          <!-- 上传按钮 -->
          <div class="flex gap-2">
            <button
              class="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium rounded-lg transition-colors shadow-sm flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="isUploading || isLoading"
              @click="handleClick"
            >
              <i class="ph ph-upload-simple" v-if="!isUploading"></i>
              <i class="ph ph-spinner animate-spin" v-else></i>
              {{ isUploading ? '上传中...' : '选择图片' }}
            </button>

            <button
              v-if="hasCustomAvatar"
              class="px-4 py-2 bg-red-500 hover:bg-red-600 text-white text-sm font-medium rounded-lg transition-colors shadow-sm flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="isLoading"
              @click="handleRemove"
            >
              <i class="ph ph-trash"></i>
              删除
            </button>
          </div>

          <!-- 拖拽上传区域 -->
          <div
            class="border-2 border-dashed rounded-lg p-4 text-center transition-colors cursor-pointer"
            :class="[
              isDragging
                ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                : 'border-gray-300 dark:border-gray-600 hover:border-gray-400 dark:hover:border-gray-500',
              (isUploading || isLoading) ? 'opacity-50 cursor-not-allowed' : ''
            ]"
            @click="!isUploading && !isLoading && handleClick()"
            @drop="!isUploading && !isLoading && handleDrop($event)"
            @dragover="handleDragOver"
            @dragleave="handleDragLeave"
          >
            <i class="ph ph-image text-2xl text-gray-400 mb-1"></i>
            <p class="text-xs text-gray-500 dark:text-gray-400">
              拖拽图片到此处上传
            </p>
          </div>

          <!-- 开发提示 -->
          <div
            v-if="hasCustomAvatar && avatarUrl?.startsWith('data:')"
            class="flex items-center gap-2 p-3 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg text-xs text-yellow-700 dark:text-yellow-400"
          >
            <i class="ph ph-info"></i>
            <span>当前使用 base64 临时存储，刷新后将会丢失。请尽快接入 OSS。</span>
          </div>
        </div>
      </div>

      <!-- 隐藏的文件输入 -->
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        class="hidden"
        @change="handleFileSelect"
      />
    </div>
  </div>
</template>
