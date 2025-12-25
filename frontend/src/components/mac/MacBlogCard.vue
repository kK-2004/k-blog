<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import type { Post } from '@/data/initialPosts'
import { renderMarkdown } from '@/composables/useMarkdown'
import { useReadStats } from '@/composables/useReadStats'
import AvatarCircle from './AvatarCircle.vue'

type CommentItem = {
  user: string
  text: string
  isHot: boolean
}

const props = defineProps<{
  post: Post
}>()

const COLLAPSED_HEIGHT = 160
const EXPANDED_MAX_HEIGHT = 600

const isExpanded = ref(false)
const draftUser = ref('')
const draftText = ref('')
const comments = ref<CommentItem[]>([])

const summary = ref('')
const isGenerating = ref(false)

const showExpandBtn = computed(() => {
  const text = props.post.content || ''
  return text.length > 100 || text.includes('```') || Boolean(props.post.hotComment)
})

watchEffect(() => {
  if (!showExpandBtn.value) isExpanded.value = true
})

watchEffect(() => {
  comments.value = []
  if (props.post.hotComment) comments.value.push({ ...props.post.hotComment, isHot: true })
})

const readStats = useReadStats(computed(() => props.post.content))

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

const submitComment = () => {
  if (!draftText.value.trim()) return
  comments.value.push({
    user: draftUser.value.trim() || 'Guest',
    text: draftText.value,
    isHot: false,
  })
  draftText.value = ''
}

const generateAiSummary = async () => {
  if (isGenerating.value || summary.value) return
  isGenerating.value = true

  try {
    const res = await fetch('/api/ai/summary', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: props.post.content }),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const data = (await res.json()) as { summary?: string }
    summary.value = data.summary || '无法生成摘要'
  } catch {
    await new Promise((r) => setTimeout(r, 1200))
    summary.value =
      '（演示模式：本地 /api/ai/summary 不可用）\n这是一段模拟摘要。你可以把摘要服务替换成真实的 Gemini / OpenAI 接口。'
  } finally {
    isGenerating.value = false
  }
}
</script>

<template>
  <div class="mb-8 group transition-transform duration-300 hover:-translate-y-1">
    <div
      class="bg-white/90 dark:bg-[#1e1e1e]/90 backdrop-blur-xl rounded-xl shadow-lg border border-white/20 dark:border-white/10 overflow-hidden relative flex flex-col transition-all duration-500 ease-[cubic-bezier(0.25,1,0.5,1)]"
      :style="{ height: isExpanded ? `${EXPANDED_MAX_HEIGHT}px` : `${COLLAPSED_HEIGHT}px` }"
    >
      <div
        class="h-8 shrink-0 bg-gradient-to-b from-gray-100 to-gray-200 dark:from-[#3a3a3a] dark:to-[#2b2b2b] border-b border-gray-300 dark:border-black flex items-center px-4 justify-between z-20"
      >
        <div class="flex gap-2">
          <div class="w-3 h-3 rounded-full bg-[#FF5F56] border border-black/10"></div>
          <div class="w-3 h-3 rounded-full bg-[#FFBD2E] border border-black/10"></div>
          <div class="w-3 h-3 rounded-full bg-[#27C93F] border border-black/10"></div>
        </div>
        <div class="text-[11px] font-semibold text-gray-500 dark:text-gray-400 flex items-center gap-1 opacity-80">
          <i class="ph ph-file-text"></i>
          {{ post.title }}
        </div>
        <div class="w-10"></div>
      </div>

      <div class="flex-1 p-6 mac-scrollbar transition-all duration-300" :class="isExpanded ? 'overflow-y-auto' : 'overflow-hidden'">
        <div class="mb-5">
          <div class="flex items-center gap-3 mb-3">
            <AvatarCircle :name="post.author" size="lg" />
            <div>
              <div class="text-sm font-bold text-gray-800 dark:text-gray-100">{{ post.author }}</div>
              <div class="text-xs text-gray-400 font-mono">{{ post.time }}</div>
            </div>
          </div>

          <div
            class="flex items-center justify-between bg-gray-50/80 dark:bg-white/5 rounded-lg p-2 px-3 border border-gray-100 dark:border-white/5 mb-3 backdrop-blur-sm"
          >
            <div class="flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400 font-mono">
              <div class="flex items-center gap-1.5" title="预估字数">
                <i class="ph ph-text-t text-base text-gray-400"></i>
                <span>{{ readStats.count }}</span>
              </div>
              <div class="flex items-center gap-1.5" title="预估阅读时间">
                <i class="ph ph-hourglass text-base text-gray-400"></i>
                <span>{{ readStats.time }}</span>
              </div>
            </div>

            <button
              v-if="!summary && !isGenerating"
              class="text-xs flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-white dark:bg-white/10 border border-gray-200 dark:border-white/10 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 hover:border-blue-200 dark:hover:border-blue-500/50 transition-all shadow-sm active:scale-95 group/btn"
              @click="generateAiSummary"
            >
              <i class="ph ph-sparkle text-purple-500 group-hover/btn:scale-110 transition-transform"></i>
              <span>AI 摘要</span>
            </button>
          </div>

          <div v-if="isGenerating || summary" class="relative group/ai animate-[fadeIn_0.3s_ease-out]">
            <div class="absolute -inset-0.5 bg-gradient-to-r from-pink-500 via-purple-500 to-blue-500 rounded-lg opacity-20 blur group-hover/ai:opacity-30 transition duration-1000"></div>
            <div
              class="relative bg-gray-50/90 dark:bg-[#252527]/90 backdrop-blur-md p-3 rounded-lg border border-white/50 dark:border-white/10 text-xs text-gray-600 dark:text-gray-300 leading-relaxed shadow-sm"
            >
              <div v-if="isGenerating" class="flex items-center gap-2 text-gray-500">
                <i class="ph ph-circle-notch animate-spin text-blue-500"></i>
                <span class="animate-pulse">正在生成摘要...</span>
              </div>
              <div v-else class="flex gap-2">
                <i class="ph ph-sparkle-fill text-purple-500 shrink-0 mt-0.5"></i>
                <div class="whitespace-pre-wrap">{{ summary }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="prose dark:prose-invert text-[15px] leading-relaxed font-sans max-w-none" v-html="renderMarkdown(post.content)"></div>

        <div class="mt-8 pt-6 border-t border-gray-100 dark:border-white/5 space-y-4">
          <h4 class="text-xs font-bold text-gray-400 uppercase tracking-widest mb-4">Comments</h4>

          <div v-for="(comment, index) in comments" :key="index" class="flex gap-3 animate-[fadeIn_0.3s_ease-out]">
            <AvatarCircle :name="comment.user" size="md" class="shrink-0 mt-1" />

            <div class="flex flex-col items-start max-w-[85%]">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-xs font-medium text-gray-500 dark:text-gray-400">{{ comment.user }}</span>
                <span v-if="comment.isHot" class="bg-red-500 text-white text-[9px] px-1 rounded font-bold">HOT</span>
              </div>
              <div class="bg-[#e9e9eb] dark:bg-[#3a3a3c] px-3 py-2 rounded-2xl rounded-tl-sm text-[13px] text-gray-800 dark:text-gray-100 leading-normal">
                {{ comment.text }}
              </div>
            </div>
          </div>

          <div class="flex items-end gap-2 pt-4">
            <div
              class="flex-1 bg-gray-100 dark:bg-[#2c2c2e] p-1 rounded-[20px] border border-transparent focus-within:border-blue-500/30 transition-all flex flex-col"
            >
              <input
                v-model="draftUser"
                placeholder="Nickname..."
                class="bg-transparent text-[10px] text-gray-500 dark:text-gray-400 px-3 py-1 outline-none w-full border-b border-gray-200 dark:border-white/5 mb-1 placeholder-gray-400/70"
              />
              <textarea
                v-model="draftText"
                placeholder="iMessage..."
                rows="1"
                class="bg-transparent text-sm px-3 py-1 outline-none w-full resize-none text-gray-800 dark:text-gray-100 placeholder-gray-400"
                style="min-height: 24px"
              ></textarea>
            </div>
            <button
              class="w-8 h-8 rounded-full bg-[#007AFF] hover:bg-[#0062cc] disabled:bg-gray-300 dark:disabled:bg-gray-600 flex items-center justify-center text-white transition-all shadow-md active:scale-95 mb-0.5"
              :disabled="!draftText.trim()"
              @click="submitComment"
            >
              <i class="ph ph-arrow-up font-bold"></i>
            </button>
          </div>
          <div class="h-12 w-full"></div>
        </div>
      </div>

      <div
        v-if="!isExpanded && showExpandBtn"
        class="absolute bottom-0 left-0 w-full h-32 bg-gradient-to-t from-white dark:from-[#1e1e1e] via-white/80 dark:via-[#1e1e1e]/80 to-transparent flex items-end justify-center pb-4 z-30 pointer-events-none"
      >
        <button
          class="pointer-events-auto px-6 py-2 rounded-full text-xs font-semibold bg-white dark:bg-[#3a3a3c] text-gray-600 dark:text-gray-200 border border-gray-200 dark:border-white/10 shadow-lg hover:scale-105 active:scale-95 transition-all flex items-center gap-2"
          @click="toggleExpand"
        >
          <span>展开阅读</span>
          <i class="ph ph-caret-down"></i>
        </button>
      </div>

      <div v-if="isExpanded && showExpandBtn" class="absolute bottom-4 right-6 z-30">
        <button
          class="w-10 h-10 rounded-full bg-gray-200/80 dark:bg-gray-700/80 backdrop-blur text-gray-600 dark:text-gray-200 hover:bg-gray-300 dark:hover:bg-gray-600 shadow-lg flex items-center justify-center transition-all hover:scale-110 active:scale-90"
          title="收起"
          @click="toggleExpand"
        >
          <i class="ph ph-caret-up text-lg"></i>
        </button>
      </div>
    </div>
  </div>
</template>

