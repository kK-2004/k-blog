<script setup lang="ts">
import { computed } from 'vue'
import AvatarCircle from '@/components/mac/AvatarCircle.vue'

const props = defineProps<{
  author: string
  createdAt: number | null
  readTime: string
  wordCount: string
  views: number
  tags?: string[]
  authorAvatarUrl: string | null
}>()

// 格式化发布时间
const formattedDate = computed(() => {
  if (!props.createdAt) return ''
  return new Date(props.createdAt).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})
</script>

<template>
  <div class="article-meta mb-10">
    <!-- 作者信息 -->
    <div class="flex items-center gap-4 mb-6">
      <img
        v-if="authorAvatarUrl"
        :src="authorAvatarUrl"
        :alt="author"
        class="w-14 h-14 rounded-full object-cover ring-2 ring-white dark:ring-white/10 shadow-lg"
      />
      <AvatarCircle v-else :name="author" size="xl" />

      <div>
        <div class="font-bold text-gray-900 dark:text-gray-100 text-base">{{ author }}</div>
        <div class="text-sm text-gray-500 dark:text-gray-400">{{ formattedDate }}</div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="flex flex-wrap items-center gap-5 text-sm text-gray-600 dark:text-gray-400 mb-6">
      <span class="flex items-center gap-1.5">
        <i class="ph ph-clock text-lg"></i>
        {{ readTime }}
      </span>
      <span class="flex items-center gap-1.5">
        <i class="ph ph-text-aa text-lg"></i>
        {{ wordCount }}
      </span>
      <span class="flex items-center gap-1.5">
        <i class="ph ph-eye text-lg"></i>
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
