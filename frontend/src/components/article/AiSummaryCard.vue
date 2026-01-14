<script setup lang="ts">
import { ref, computed } from 'vue'
import { renderMarkdown } from '@/composables/useMarkdown'

const props = defineProps<{
  postId: number
  content: string
}>()

const summary = ref('')
const displayedSummary = ref('')
const isGenerating = ref(false)
const isStreaming = ref(false)
const hasReceivedData = ref(false)
let summaryRequestId = 0

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

// 生成摘要
const generateSummary = async () => {
  if (isGenerating.value || summary.value) return

  const requestId = (summaryRequestId += 1)
  displayedSummary.value = ''
  summary.value = ''
  isGenerating.value = true
  isStreaming.value = true
  hasReceivedData.value = false

  try {
    const res = await fetch('/api/ai/summary/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: props.content }),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    if (!res.body) throw new Error('No response body')

    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let acc = ''

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      if (requestId !== summaryRequestId) {
        try {
          await reader.cancel()
        } catch {
          // ignore
        }
        return
      }

      const chunk = decoder.decode(value, { stream: true })
      if (!chunk) continue
      acc += chunk
      displayedSummary.value = acc
      hasReceivedData.value = true
    }

    if (requestId !== summaryRequestId) return
    const finalText = acc.trim() || '无法生成摘要'
    displayedSummary.value = finalText
    summary.value = finalText
  } catch {
    if (requestId !== summaryRequestId) return
    await new Promise((r) => setTimeout(r, 1200))
    summary.value =
      '（演示模式：本地 /api/ai/summary/stream 不可用）\n这是一段模拟摘要。你可以把摘要服务替换成真实的 GLM / OpenAI 接口。'
    displayedSummary.value = summary.value
    hasReceivedData.value = true
  } finally {
    if (requestId === summaryRequestId) {
      isGenerating.value = false
      isStreaming.value = false
    }
  }
}

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
          class="text-xs bg-white dark:bg-white/10 px-4 py-2 rounded-full hover:scale-105 transition shadow-sm border border-gray-200 dark:border-white/10 flex items-center gap-1.5"
        >
          <i class="ph-fill ph-sparkle text-purple-500"></i>
          <span>生成摘要</span>
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
          点击右上角按钮生成本文的智能摘要...
        </div>
      </div>

      <!-- 底部提示 -->
      <div v-if="summary" class="mt-4 pt-3 border-t border-gray-100 dark:border-white/5 flex justify-end">
        <span class="text-[10px] text-gray-400 dark:text-gray-500">AI的回答未必正确无误，请注意核查</span>
      </div>
    </div>
  </div>
</template>
