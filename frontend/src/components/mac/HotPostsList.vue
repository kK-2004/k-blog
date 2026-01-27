<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { getHotPosts } from '@/api/posts'
import type { HotPost } from '@/api/types'

const hotPosts = ref<HotPost[]>([])
const loading = ref(false)

const loadHotPosts = async () => {
  loading.value = true
  try {
    hotPosts.value = await getHotPosts()
  } catch (e) {
    console.error('加载热门文章失败:', e)
    hotPosts.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadHotPosts)

// 每30秒刷新一次
const intervalId = setInterval(loadHotPosts, 30000)

onBeforeUnmount(() => clearInterval(intervalId))

const goToArticle = (id: number) => {
  window.location.hash = `#/article/${id}`
}

/**
 * 格式化时间显示
 * @param lastViewTime 过期时间戳
 * @returns 格式化后的时间字符串
 */
const formatTime = (lastViewTime: number): string => {
  const now = Date.now()
  const expireTime = lastViewTime
  // lastViewTime 是过期时间戳，需要减去窗口时间来估算访问时间
  // 窗口是10分钟，所以访问时间大约是 过期时间 - 10分钟
  const windowMs = 10 * 60 * 1000
  const viewTime = expireTime - windowMs
  const diffMs = now - viewTime

  if (diffMs < 60 * 1000) {
    return '刚刚'
  } else if (diffMs < 60 * 60 * 1000) {
    const minutes = Math.floor(diffMs / (60 * 1000))
    return `${minutes}分钟前`
  } else {
    return '较早'
  }
}
</script>

<template>
  <div class="bg-white/90 dark:bg-[#1e1e1e]/90 backdrop-blur-xl rounded-2xl shadow-lg border border-gray-200/50 dark:border-white/5 overflow-hidden">
    <div class="p-4 border-b border-gray-100 dark:border-white/5">
      <div class="flex items-center gap-2">
        <i class="ph-fill ph-fire text-orange-500 text-lg"></i>
        <h3 class="text-sm font-semibold text-gray-800 dark:text-gray-200">最新阅读</h3>
      </div>
    </div>

    <div v-if="loading && hotPosts.length === 0" class="p-4 text-center text-sm text-gray-500">
      加载中...
    </div>

    <div v-else-if="hotPosts.length === 0" class="p-4 text-center text-sm text-gray-400">
      最近10min还没有人阅读过~
    </div>

    <div v-else class="divide-y divide-gray-100 dark:divide-white/5">
      <button
        v-for="(post, index) in hotPosts"
        :key="post.id"
        @click="goToArticle(post.id)"
        class="w-full flex items-start gap-3 p-3 hover:bg-gray-50 dark:hover:bg-white/5 transition-colors text-left group"
      >
        <!-- 排名图标 -->
        <div class="flex-shrink-0 w-5 h-5 flex items-center justify-center">
          <span v-if="index === 0" class="text-orange-500">
            <i class="ph-fill ph-medal-military text-lg"></i>
          </span>
          <span v-else-if="index === 1" class="text-gray-400">
            <i class="ph-fill ph-medal text-lg"></i>
          </span>
          <span v-else-if="index === 2" class="text-amber-600">
            <i class="ph-fill ph-award text-lg"></i>
          </span>
          <span v-else class="text-xs font-medium text-gray-400">
            {{ index + 1 }}
          </span>
        </div>

        <!-- 文章标题 -->
        <div class="min-w-0 flex-1">
          <div class="text-sm text-gray-700 dark:text-gray-300 group-hover:text-blue-600 dark:group-hover:text-blue-400 line-clamp-2 transition-colors">
            {{ post.title }}
          </div>
        </div>

        <!-- 时间 -->
        <div class="flex-shrink-0 text-xs text-gray-400">
          {{ formatTime(post.lastViewTime) }}
        </div>
      </button>
    </div>
  </div>
</template>
