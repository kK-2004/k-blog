<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AvatarCircle from './AvatarCircle.vue'
import { useUserAvatar } from '@/composables/useUserAvatar'

defineProps<{
  timeStr: string
  theme: 'light' | 'dark'
  isAuthenticated: boolean
}>()

defineEmits<{
  (e: 'toggle-sidebar'): void
  (e: 'avatar-click'): void
  (e: 'toggle-theme'): void
  (e: 'logout'): void
}>()

const { avatarUrl, hasCustomAvatar, loadAvatar } = useUserAvatar()
const imageError = ref(false)

onMounted(() => {
  loadAvatar()
})

const handleImageError = () => {
  imageError.value = true
}

const handleImageLoad = () => {
  imageError.value = false
}
</script>

<template>
  <header
    class="fixed top-0 left-0 right-0 z-40 h-8 bg-white/70 dark:bg-black/40 backdrop-blur-xl border-b border-white/10 dark:border-white/5 flex items-center justify-between px-4 select-none text-sm text-gray-800 dark:text-gray-200 shadow-sm transition-colors duration-500"
  >
    <div class="flex items-center gap-4 h-full">
      <button class="hover:bg-black/5 dark:hover:bg-white/10 p-1 rounded transition-colors" @click="$emit('toggle-sidebar')">
        <i class="ph ph-sidebar text-lg"></i>
      </button>
      <div class="flex items-center gap-3">
        <button class="relative group cursor-pointer" @click="$emit('avatar-click')">
          <!-- 自定义头像 -->
          <img
            v-if="hasCustomAvatar && !imageError && avatarUrl"
            :src="avatarUrl"
            alt="User Avatar"
            class="w-5 h-5 rounded-full object-cover shadow-sm"
            @error="handleImageError"
            @load="handleImageLoad"
          />
          <!-- 降级到默认头像 -->
          <AvatarCircle v-else name="kk" size="sm" />
          <span
            class="absolute top-0 right-0 w-1.5 h-1.5 border border-white dark:border-black rounded-full"
            :class="isAuthenticated ? 'bg-green-500' : 'bg-gray-400'"
          ></span>
        </button>
        <span class="font-bold tracking-tight">KK Blog</span>
      </div>
    </div>

    <div class="flex items-center gap-4">
      <button
        v-if="isAuthenticated"
        class="p-1 rounded hover:bg-red-500/10 dark:hover:bg-red-500/20 text-red-600 dark:text-red-400"
        @click="$emit('logout')"
        title="退出登录"
      >
        <i class="ph ph-sign-out text-lg"></i>
      </button>
      <button class="p-1 rounded hover:bg-black/5 dark:hover:bg-white/10" @click="$emit('toggle-theme')">
        <i :class="['ph', theme === 'light' ? 'ph-moon' : 'ph-sun', 'text-lg']"></i>
      </button>
      <div class="flex items-center gap-2 opacity-90 text-xs font-medium font-mono pl-3 border-l border-gray-300 dark:border-gray-700 h-4">
        <span>{{ timeStr }}</span>
      </div>
    </div>
  </header>
</template>

