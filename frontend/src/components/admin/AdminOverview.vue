<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { AdminMe, Post, QuickAction } from '@/api/types'
import { listVisibleQuickActions } from '@/api/site'
import { formatCount, formatDateTime, formatRelativeTime, postTime } from '@/utils/adminFormat'

const props = defineProps<{
  posts: Post[]
  loading: boolean
  adminMe: AdminMe | null
}>()

const emit = defineEmits<{
  (e: 'create'): void
  (e: 'edit', post: Post): void
}>()

const quickActions = ref<QuickAction[]>([])

const sum = (key: 'views' | 'likes' | 'comments') => props.posts.reduce((total, p) => total + (p[key] ?? 0), 0)

const stats = computed(() => [
  { label: '文章', value: formatCount(props.posts.length), icon: 'ph-article' },
  { label: '阅读', value: formatCount(sum('views')), icon: 'ph-eye' },
  { label: '点赞', value: formatCount(sum('likes')), icon: 'ph-heart' },
  { label: '评论', value: formatCount(sum('comments')), icon: 'ph-chat-circle' },
])

const recentPosts = computed(() => [...props.posts].sort((a, b) => postTime(b) - postTime(a)).slice(0, 5))
const topPosts = computed(() => [...props.posts].sort((a, b) => b.views - a.views).slice(0, 5))

const lastLoginText = computed(() => {
  const time = formatDateTime(props.adminMe?.lastLoginAt ?? null)
  if (!time) return ''
  const location = props.adminMe?.lastLoginLocation
  return location ? `最近登录 ${time} · ${location}` : `最近登录 ${time}`
})

const openQuickAction = (item: QuickAction) => {
  if (item.targetType === 'internal') {
    window.location.hash = item.target.startsWith('#') ? item.target : `#${item.target}`
    return
  }
  window.open(item.target, '_blank', 'noopener,noreferrer')
}

onMounted(async () => {
  try {
    quickActions.value = await listVisibleQuickActions()
  } catch {
    quickActions.value = []
  }
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between gap-3">
      <div class="min-w-0">
        <h1 class="text-xl font-semibold text-gray-900 dark:text-gray-100">概览</h1>
        <p v-if="lastLoginText" class="mt-0.5 text-xs text-gray-500 dark:text-gray-400 truncate">{{ lastLoginText }}</p>
      </div>
      <button
        class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-lg bg-[#007AFF] text-white hover:bg-[#0062cc] transition-colors shrink-0"
        @click="emit('create')"
      >
        <i class="ph ph-plus"></i>
        写文章
      </button>
    </div>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <div
        v-for="item in stats"
        :key="item.label"
        class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-sm border border-black/5 dark:border-white/10 px-4 py-3.5"
      >
        <div class="flex items-center gap-1.5 text-xs text-gray-500 dark:text-gray-400">
          <i :class="['ph', item.icon]"></i>
          {{ item.label }}
        </div>
        <div class="mt-1 text-2xl font-semibold text-gray-900 dark:text-gray-100 tabular-nums">
          {{ loading ? '–' : item.value }}
        </div>
      </div>
    </div>

    <div
      v-if="!loading && posts.length === 0"
      class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-sm border border-black/5 dark:border-white/10 py-16 text-center"
    >
      <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">写下第一篇文章</p>
      <button
        class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-lg bg-[#007AFF] text-white hover:bg-[#0062cc] transition-colors"
        @click="emit('create')"
      >
        <i class="ph ph-plus"></i>
        写文章
      </button>
    </div>

    <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-3">
      <section class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-sm border border-black/5 dark:border-white/10 overflow-hidden">
        <h2 class="px-5 pt-4 pb-2 text-sm font-semibold text-gray-700 dark:text-gray-200">最近更新</h2>
        <div v-if="loading" class="px-5 pb-4 text-sm text-gray-500 dark:text-gray-400">加载中…</div>
        <template v-else>
        <button
          v-for="post in recentPosts"
          :key="post.id"
          class="w-full flex items-center justify-between gap-4 px-5 py-2.5 text-left border-t border-gray-100 dark:border-white/5 hover:bg-black/[0.03] dark:hover:bg-white/5 transition-colors"
          @click="emit('edit', post)"
        >
          <span class="truncate text-sm text-gray-800 dark:text-gray-200">{{ post.title }}</span>
          <span class="shrink-0 text-xs text-gray-500 dark:text-gray-400">{{ formatRelativeTime(postTime(post)) }}</span>
        </button>
        </template>
      </section>

      <section class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-sm border border-black/5 dark:border-white/10 overflow-hidden">
        <h2 class="px-5 pt-4 pb-2 text-sm font-semibold text-gray-700 dark:text-gray-200">阅读最多</h2>
        <div v-if="loading" class="px-5 pb-4 text-sm text-gray-500 dark:text-gray-400">加载中…</div>
        <template v-else>
        <button
          v-for="post in topPosts"
          :key="post.id"
          class="w-full flex items-center justify-between gap-4 px-5 py-2.5 text-left border-t border-gray-100 dark:border-white/5 hover:bg-black/[0.03] dark:hover:bg-white/5 transition-colors"
          @click="emit('edit', post)"
        >
          <span class="truncate text-sm text-gray-800 dark:text-gray-200">{{ post.title }}</span>
          <span class="shrink-0 text-xs text-gray-500 dark:text-gray-400 tabular-nums">{{ formatCount(post.views) }}</span>
        </button>
        </template>
      </section>
    </div>

    <section
      v-if="quickActions.length"
      class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-sm border border-black/5 dark:border-white/10 px-5 py-4"
    >
      <h2 class="mb-3 text-sm font-semibold text-gray-700 dark:text-gray-200">快速操作</h2>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="item in quickActions"
          :key="item.id"
          class="inline-flex items-center gap-2 px-3 py-1.5 text-sm rounded-lg border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-200 hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
          :title="item.description || item.title"
          @click="openQuickAction(item)"
        >
          <i :class="['ph', item.icon, 'text-[#007AFF]']"></i>
          {{ item.title }}
        </button>
      </div>
    </section>
  </div>
</template>
