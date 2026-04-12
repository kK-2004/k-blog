<script setup lang="ts">
import { computed } from 'vue'
import AvatarCircle from '@/components/mac/AvatarCircle.vue'

const props = defineProps<{
  author: string
  createdAt: number | null
  updatedAt: number | null
  readTime: string
  wordCount: string
  views: number
  tags?: string[]
  authorAvatarUrl: string | null
}>()

// 格式化发布时间
const formattedDate = computed(() => {
  const ms = props.createdAt
  if (typeof ms === 'number' && Number.isFinite(ms)) {
    const diffMs = Date.now() - ms
    if (diffMs < 60_000) return '刚刚'
    const diffMinutes = Math.floor(diffMs / 60_000)
    if (diffMinutes < 10) return `${diffMinutes}分钟前`
    return new Date(ms).toLocaleString('zh-CN')
  }
  return ''
})

// 是否显示更新时间（updatedAt 与 createdAt 相差超过5秒时显示，排除后端初始化的微小差异）
const showUpdatedDate = computed(() => {
  if (!props.updatedAt || !props.createdAt) return false
  if (typeof props.updatedAt !== 'number' || typeof props.createdAt !== 'number') return false
  return props.updatedAt - props.createdAt > 5000
})

// 格式化更新时间
const formattedUpdatedDate = computed(() => {
  const ms = props.updatedAt
  if (typeof ms === 'number' && Number.isFinite(ms)) {
    const diffMs = Date.now() - ms
    if (diffMs < 60_000) return '刚刚'
    const diffMinutes = Math.floor(diffMs / 60_000)
    if (diffMinutes < 10) return `${diffMinutes}分钟前`
    return new Date(ms).toLocaleString('zh-CN')
  }
  return ''
})
</script>

<template>
  <div class="article-meta">
    <!-- 作者信息 -->
    <div class="flex items-center gap-3 mb-3">
      <img
        v-if="authorAvatarUrl"
        :src="authorAvatarUrl"
        :alt="author"
        draggable="false"
        class="w-10 h-10 rounded-full object-cover shadow-sm"
      />
      <AvatarCircle v-else :name="author" size="lg" />

      <div class="flex-1">
        <div class="text-sm font-bold text-gray-800 dark:text-gray-100">{{ author }}</div>
        <div class="text-xs text-gray-400 font-mono">
          <template v-if="showUpdatedDate">更新于 {{ formattedUpdatedDate }}</template>
          <template v-else>{{ formattedDate }}</template>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="pt-2 flex flex-wrap items-center gap-4 text-xs text-gray-500 dark:text-gray-400 font-mono">
      <span class="flex items-center gap-1.5">
        <i class="ph ph-hourglass text-base text-gray-400"></i>
        {{ readTime }}
      </span>
      <span class="flex items-center gap-1.5">
        <i class="ph ph-text-t text-base text-gray-400"></i>
        {{ wordCount }}
      </span>
      <span class="flex items-center gap-1.5">
        <i class="ph ph-eye text-base text-gray-400"></i>
        {{ views }} 次阅读
      </span>
    </div>

    <!-- 标签 -->
    <div v-if="tags && tags.length > 0" class="flex flex-wrap gap-2">
      <span
        v-for="tag in tags"
        :key="tag"
        class="inline-flex items-center px-3 py-1.5 text-xs font-medium bg-gray-100 dark:bg-white/10 text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-200 dark:hover:bg-white/15 transition-colors cursor-default"
      >
        #{{ tag }}
      </span>
    </div>
  </div>
</template>
