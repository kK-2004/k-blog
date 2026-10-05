<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/composables/useMarkdown'
import { useAiSummary } from '@/composables/useAiSummary'
import type { AiSummaryStatus } from '@/api/types'

const props = defineProps<{
  postId: number
  content: string
  aiSummaryStatus?: AiSummaryStatus
}>()

// 摘要由后端发布后异步生成落库；这里读取（Redis → 数据库）并以打字机效果展示
const {
  summary,
  displayedSummary,
  isGenerating,
  isStreaming,
  hasReceivedData,
  isPending,
  reveal: generateSummary,
} = useAiSummary(() => props.postId, () => props.aiSummaryStatus)

// 判断是否应该渲染 Markdown
const shouldRenderMarkdown = computed(() => {
  return !isStreaming.value && displayedSummary.value.length > 0
})

// 渲染后的 HTML
const renderedSummaryHtml = computed(() => {
  if (isStreaming.value) {
    return displayedSummary.value
  }
  return renderMarkdown(displayedSummary.value)
})

// 暴露方法供父组件调用
defineExpose({
  generateSummary
})
</script>

<template>
  <div class="mb-12 relative group/ai">
    <!-- 背景渐变效果 -->
    <div class="absolute -inset-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500 rounded-2xl opacity-20 group-hover/ai:opacity-30 blur transition duration-500"></div>

    <div class="relative bg-white/50 dark:bg-[#1c1c1e]/50 backdrop-blur-xl border border-white/20 dark:border-white/5 rounded-2xl p-6 overflow-hidden">
      <!-- 头部 -->
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-2">
          <!-- 星星图标 -->
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 3L14.5 9.5L21 12L14.5 14.5L12 21L9.5 14.5L3 12L9.5 9.5L12 3Z" fill="url(#ai-gradient)" />
            <defs>
              <linearGradient id="ai-gradient" x1="3" y1="3" x2="21" y2="21" gradientUnits="userSpaceOnUse">
                <stop stop-color="#4E8AFF" />
                <stop offset="0.5" stop-color="#A855F7" />
                <stop offset="1" stop-color="#FF5D9E" />
              </linearGradient>
            </defs>
          </svg>
          <span class="text-xs font-bold tracking-[0.2em] uppercase text-transparent bg-clip-text bg-gradient-to-r from-blue-500 to-purple-500">
            AI Summary
          </span>
        </div>

        <!-- 生成按钮 -->
        <button
          v-if="!summary && !isGenerating"
          @click="generateSummary"
          :disabled="isPending"
          class="text-xs bg-white dark:bg-white/10 px-4 py-2 rounded-full transition shadow-sm border border-gray-200 dark:border-white/10 flex items-center gap-1.5"
          :class="isPending ? 'opacity-60 cursor-not-allowed' : 'hover:scale-105'"
        >
          <i v-if="isPending" class="ph ph-circle-notch animate-spin text-purple-500"></i>
          <i v-else class="ph-fill ph-sparkle text-purple-500"></i>
          <span>{{ isPending ? '生成中' : '生成摘要' }}</span>
        </button>
      </div>

      <!-- 内容区域 -->
      <div class="text-sm leading-relaxed text-gray-700 dark:text-gray-300">
        <!-- 加载动画 -->
        <div v-if="isGenerating && !hasReceivedData" class="space-y-2">
          <div class="h-3 bg-gray-200 dark:bg-white/10 rounded w-full animate-pulse"></div>
          <div class="h-3 bg-gray-200 dark:bg-white/10 rounded w-[80%] animate-pulse"></div>
          <div class="h-3 bg-gray-200 dark:bg-white/10 rounded w-[60%] animate-pulse"></div>
        </div>

        <!-- 内容显示 -->
        <div v-else-if="hasReceivedData || summary">
          <!-- Markdown 渲染：流式输出完成后显示 -->
          <div v-if="shouldRenderMarkdown" class="prose prose-sm dark:prose-invert max-w-none">
            <div v-html="renderedSummaryHtml"></div>
          </div>

          <!-- 纯文本 + 光标：流式输出时显示 -->
          <div v-else>
            <p class="whitespace-pre-wrap">
              {{ displayedSummary
              }}<span v-if="isStreaming" class="inline-block w-1.5 h-4 ml-0.5 bg-purple-500 animate-pulse align-middle rounded-sm"></span>
            </p>
          </div>
        </div>

        <!-- 初始提示 -->
        <div v-else class="text-gray-400 italic">
          {{ isPending ? 'AI 正在为本文生成摘要，请稍候...' : '点击右上角按钮生成本文的智能摘要...' }}
        </div>
      </div>

      <!-- 底部提示 -->
      <div v-if="summary" class="mt-4 pt-3 border-t border-gray-100 dark:border-white/5 flex justify-end">
        <span class="text-[10px] text-gray-400 dark:text-gray-500">AI的回答未必正确无误，请注意核查</span>
      </div>
    </div>
  </div>
</template>
