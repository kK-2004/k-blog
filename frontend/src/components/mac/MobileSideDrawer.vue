<script setup lang="ts">
import { watch, computed } from 'vue'
import ProfileOverview from './ProfileOverview.vue'
import HotPostsList from './HotPostsList.vue'
import type { PublicProfile } from '@/api/site'

const props = defineProps<{
  isOpen: boolean
  activeTab: 'profile' | 'hotposts'
  isAuthenticated: boolean
  publicProfile: PublicProfile | null
  currentView: 'blog' | 'admin' | 'settings' | 'login' | 'article'
}>()

// 只在 blog 页面显示内容
const shouldShowContent = computed(() => props.currentView === 'blog')

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'switchTab', tab: 'profile' | 'hotposts'): void
}>()

// 抽屉打开时禁止背景滚动
watch(() => props.isOpen, (isOpen) => {
  if (isOpen) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})
</script>

<template>
  <!-- 遮罩层 -->
  <Transition
    enter-active-class="transition-opacity duration-300"
    leave-active-class="transition-opacity duration-300"
    enter-from-class="opacity-0"
    leave-to-class="opacity-0"
    enter-to-class="opacity-100"
    leave-from-class="opacity-100"
  >
    <div
      v-if="isOpen"
      class="fixed inset-0 bg-black/20 backdrop-blur-sm z-40 lg:hidden"
      @click="emit('close')"
    ></div>
  </Transition>

  <!-- 抽屉本体 -->
  <Transition
    enter-active-class="transition-transform duration-300 ease-out"
    leave-active-class="transition-transform duration-300 ease-in"
    enter-from-class="translate-x-full"
    leave-to-class="translate-x-full"
    enter-to-class="translate-x-0"
    leave-from-class="translate-x-0"
  >
    <div
      v-if="isOpen"
      class="fixed right-0 top-8 bottom-0 w-[320px] bg-white/90 dark:bg-[#1e1e1e]/95 backdrop-blur-xl border-l border-white/10 dark:border-white/5 z-50 lg:hidden overflow-hidden flex flex-col"
    >
      <!-- Tab 切换 -->
      <div class="flex border-b border-gray-200 dark:border-white/10 shrink-0">
        <button
          class="flex-1 py-3 text-sm font-medium transition-colors"
          :class="activeTab === 'profile'
            ? 'text-[#007AFF] border-b-2 border-[#007AFF]'
            : 'text-gray-500 dark:text-gray-400'"
          @click="emit('switchTab', 'profile')"
        >
          个人信息
        </button>
        <button
          class="flex-1 py-3 text-sm font-medium transition-colors"
          :class="activeTab === 'hotposts'
            ? 'text-[#007AFF] border-b-2 border-[#007AFF]'
            : 'text-gray-500 dark:text-gray-400'"
          @click="emit('switchTab', 'hotposts')"
        >
          热门文章
        </button>
      </div>

      <!-- 内容区域 -->
      <div class="flex-1 overflow-y-auto p-4 mac-scrollbar">
        <div v-if="shouldShowContent">
          <ProfileOverview
            v-if="activeTab === 'profile'"
            :show-full-features="false"
            :is-authenticated="isAuthenticated"
            :public-profile="publicProfile"
          />
          <HotPostsList v-else />
        </div>
        <div v-else class="flex items-center justify-center h-full text-gray-400 text-sm">
          仅在博客页面可用
        </div>
      </div>

      <!-- 关闭按钮 -->
      <button
        class="absolute top-3 right-3 p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-white/10 transition-colors"
        @click="emit('close')"
      >
        <i class="ph ph-x text-lg"></i>
      </button>
    </div>
  </Transition>
</template>
