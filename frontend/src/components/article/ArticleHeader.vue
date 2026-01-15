<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { Post } from '@/api/types'
import { useHashRouter } from '@/composables/useHashRouter'

const props = defineProps<{
  post: Post
  scrollProgress: number
  showTitle: boolean
  isSidebarOpen: boolean
}>()

// 标记组件是否挂载，防止服务端渲染(SSR)时的 Hydration 不匹配
const isMounted = ref(false)
const router = useHashRouter()

const trafficLightContainer = ref<HTMLElement | null>(null)

const handleBack = () => {
  router.navigateTo('blog')
}

onMounted(() => {
  isMounted.value = true
})

defineExpose({
  trafficLightContainer
})
</script>

<template>
  <Teleport to="body" v-if="isMounted">
    <header
      class="fixed w-full z-40 transition-all duration-300 border-b border-transparent"
      :class="[
        /* top-16 (64px): 为了避开顶部的 'KK Blog' 全局导航栏 */
        'top-0 md:top-8',

        /* 根据侧边栏状态调整位置 */
        isSidebarOpen ? 'left-64' : 'left-0',

        showTitle
          ? 'bg-white/80 dark:bg-[#1c1c1e]/80 backdrop-blur-md border-gray-200 dark:border-white/10 shadow-sm h-14'
          : 'bg-transparent h-16 pointer-events-none' // pointer-events-none 让透明时鼠标能穿透点击下面的内容
      ]"
    >
      <!-- 滚动进度条 -->
      <div class="absolute top-0 left-0 w-full h-[2px] bg-gray-200 dark:bg-white/10 overflow-hidden">
        <div
          class="h-full bg-gradient-to-r from-blue-500 to-purple-500 transition-all duration-150 ease-out"
          :style="{ width: `${scrollProgress}%` }"
        ></div>
      </div>

      <!-- 头部内容 -->
      <div class="max-w-7xl mx-auto px-4 h-full flex items-center justify-between pointer-events-auto">
        <!-- 左侧：返回按钮（macOS 风格红绿灯） -->
        <div
          ref="trafficLightContainer"
          class="flex gap-2 group cursor-pointer p-2 -ml-2 rounded-lg hover:bg-gray-100 dark:hover:bg-white/5 transition"
          @click="handleBack"
          title="返回列表"
        >
          <div class="relative w-3 h-3 rounded-full bg-[#FF5F56] border border-black/10 flex items-center justify-center overflow-hidden">
            <i class="ph-bold ph-x text-[8px] text-black/50 opacity-0 group-hover:opacity-100 absolute"></i>
          </div>
          <div class="relative w-3 h-3 rounded-full bg-[#FFBD2E] border border-black/10 flex items-center justify-center overflow-hidden">
            <i class="ph-bold ph-minus text-[8px] text-black/50 opacity-0 group-hover:opacity-100 absolute"></i>
          </div>
          <div class="relative w-3 h-3 rounded-full bg-[#27C93F] border border-black/10 flex items-center justify-center overflow-hidden">
            <i class="ph-bold ph-arrows-out-simple text-[8px] text-black/50 opacity-0 group-hover:opacity-100 absolute"></i>
          </div>
        </div>

        <!-- 中间：文章标题（滚动时显示） -->
        <div
          class="absolute left-1/2 -translate-x-1/2 text-sm font-semibold text-gray-800 dark:text-gray-200 transition-all duration-500 max-w-[50%] truncate text-center"
          :class="showTitle ? 'opacity-100 translate-y-0 blur-0' : 'opacity-0 translate-y-2 blur-sm'"
        >
          {{ post.title }}
        </div>

        <!-- 右侧：占位（保持布局平衡） -->
        <div class="flex items-center gap-2">
          <div class="w-8"></div>
        </div>
      </div>
    </header>
  </Teleport>
</template>
