<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, watchEffect } from 'vue'
import type { Post } from '@/data/initialPosts'
import { renderMarkdown } from '@/composables/useMarkdown'
import { useReadStats } from '@/composables/useReadStats'
import AvatarCircle from './AvatarCircle.vue'

type ReplyItem = {
  id: number
  user: string
  text: string
  likes: number
  hasLiked?: boolean
  toUser?: string
}

type CommentItem = ReplyItem & {
  isHot: boolean
  replies: ReplyItem[]
}

const props = defineProps<{
  post: Post
}>()

const COLLAPSED_MAX_HEIGHT = 600
const EXPANDED_MAX_HEIGHT = 770
const EXPAND_COLLAPSE_MS = 520
const SUMMARY_COLLAPSE_MS = 360

const COLLAPSED_INNER_MAX_HEIGHT = 400

const isExpanded = ref(false)
const isVisuallyExpanded = ref(false)
const replyingTo = ref<{ rootId: number; toUser?: string } | null>(null)
const inputRef = ref<HTMLElement | null>(null)
const cardRef = ref<HTMLElement | null>(null)
const scrollerRef = ref<HTMLElement | null>(null)
let centerScrollRaf = 0
let centerScrollStopAt = 0
let isCenteringScroll = false
let ignoreAutoCollapseUntil = 0
let centerScrollUserInterrupted = false

const interruptCenterScroll = () => {
  centerScrollUserInterrupted = true
  stopCenterScroll()
}

const draftUser = ref('')
const draftText = ref('')
const allComments = ref<CommentItem[]>([])
const comments = ref<CommentItem[]>([])
const COMMENTS_PAGE_SIZE = 5
const commentsLoading = ref(false)
const commentsHasMore = ref(false)
let innerScrollRaf = 0
let lastInnerScrollTop = 0
const hasInnerUserScrolled = ref(false)

type ReplyPagingState = {
  isOpen: boolean
  loading: boolean
  hasMore: boolean
}

const repliesVisible = ref<Record<number, ReplyItem[]>>({})
const repliesState = ref<Record<number, ReplyPagingState>>({})

const localArticleLikes = ref(props.post.likes)
const hasLikedArticle = ref(false)
const likeBouncing = ref(false)

const summary = ref('')
const displayedSummary = ref('')
const isGenerating = ref(false)
let summaryRequestId = 0
let typingTimer: number | undefined

const geminiGradientId = computed(() => `gemini_grad_${props.post.id}`)

const showExpandBtn = computed(() => {
  const text = props.post.content || ''
  return text.length > 100 || text.includes('```') || Boolean(props.post.hotComment)
})

const readStats = useReadStats(computed(() => props.post.content))

const EXPAND_EVENT = 'mac-blog-card:expand'

const resetCommentPaging = () => {
  comments.value = []
  commentsHasMore.value = false
  commentsLoading.value = false
  repliesVisible.value = {}
  repliesState.value = {}
  lastInnerScrollTop = 0
  hasInnerUserScrolled.value = false
  void loadMoreComments()
}

const fetchCommentsPage = async (offset: number, limit: number) => {
  return allComments.value.slice(offset, offset + limit)
}

const loadMoreComments = async () => {
  if (commentsLoading.value) return
  const offset = comments.value.length
  if (offset >= allComments.value.length) {
    commentsHasMore.value = false
    return
  }

  commentsLoading.value = true
  try {
    const items = await fetchCommentsPage(offset, COMMENTS_PAGE_SIZE)
    comments.value.push(...items)
  } finally {
    commentsLoading.value = false
    commentsHasMore.value = comments.value.length < allComments.value.length
  }
}

const getRepliesTotal = (commentId: number) => {
  const c = allComments.value.find((x) => x.id === commentId)
  return c?.replies.length ?? 0
}

const ensureReplyState = (commentId: number) => {
  if (!repliesState.value[commentId]) {
    repliesState.value[commentId] = { isOpen: false, loading: false, hasMore: getRepliesTotal(commentId) > 0 }
  }
  if (!repliesVisible.value[commentId]) repliesVisible.value[commentId] = []
}

const fetchRepliesPage = async (commentId: number, offset: number, limit: number) => {
  const c = allComments.value.find((x) => x.id === commentId)
  return (c?.replies ?? []).slice(offset, offset + limit)
}

const loadMoreReplies = async (commentId: number) => {
  ensureReplyState(commentId)
  const state = repliesState.value[commentId]!
  const visible = repliesVisible.value[commentId]!
  if (state.loading) return
  state.loading = true
  state.isOpen = true

  try {
    const offset = visible.length
    const items = await fetchRepliesPage(commentId, offset, COMMENTS_PAGE_SIZE)
    visible.push(...items)
  } finally {
    state.loading = false
    state.hasMore = visible.length < getRepliesTotal(commentId)
  }
}

const isRepliesOpen = (commentId: number) => repliesState.value[commentId]?.isOpen ?? false
const isRepliesLoading = (commentId: number) => repliesState.value[commentId]?.loading ?? false
const repliesHasMore = (commentId: number) => repliesState.value[commentId]?.hasMore ?? getRepliesTotal(commentId) > 0
const getVisibleReplies = (commentId: number) => repliesVisible.value[commentId] ?? []

const maybeLoadMoreOnScroll = () => {
  const el = scrollerRef.value
  if (!el) return
  if (!isExpanded.value) return
  if (!commentsHasMore.value || commentsLoading.value) return
  const currentTop = el.scrollTop
  if (currentTop > 0) hasInnerUserScrolled.value = true
  const scrolledDown = currentTop > lastInnerScrollTop + 1
  lastInnerScrollTop = currentTop
  if (!hasInnerUserScrolled.value) return
  if (!scrolledDown) return
  if (currentTop + el.clientHeight >= el.scrollHeight - 90) void loadMoreComments()
}

const onInnerScroll = () => {
  if (innerScrollRaf) return
  innerScrollRaf = window.requestAnimationFrame(() => {
    innerScrollRaf = 0
    maybeLoadMoreOnScroll()
  })
}

watchEffect(() => {
  if (!showExpandBtn.value) {
    isExpanded.value = true
    isVisuallyExpanded.value = true
  }
})

watch(
  () => props.post.hotComment,
  (hc) => {
    allComments.value = []
    if (hc) {
      allComments.value.push({
        id: hc.id ?? props.post.id * 1000 + 1,
        user: hc.user,
        text: hc.text,
        likes: hc.likes ?? Math.max(1, Math.round(props.post.likes / 4)),
        replies: (hc.replies ?? []).map((r) => ({
          id: r.id,
          user: r.user,
          text: r.text,
          likes: r.likes ?? 0,
        })),
        isHot: true,
      })
    }
    resetCommentPaging()
  },
  { immediate: true },
)

watchEffect((onCleanup) => {
  const el = scrollerRef.value
  if (!el) return
  el.addEventListener('scroll', onInnerScroll, { passive: true })
  onCleanup(() => el.removeEventListener('scroll', onInnerScroll))
})

const getFixedHeaderHeight = () => {
  const header = document.querySelector('header')
  if (!header) return 0
  const rect = header.getBoundingClientRect()
  const isFixed = window.getComputedStyle(header).position === 'fixed'
  return isFixed ? rect.height : 0
}

const getCardTargetScrollY = () => {
  const el = cardRef.value
  if (!el) return null

  const headerHeight = getFixedHeaderHeight()
  const rect = el.getBoundingClientRect()
  const viewportCenterY = headerHeight + (window.innerHeight - headerHeight) / 2
  const elementCenterY = rect.top + rect.height / 2
  const targetY = window.scrollY + (elementCenterY - viewportCenterY)
  const maxScrollY = Math.max(0, document.documentElement.scrollHeight - window.innerHeight)
  return Math.min(maxScrollY, Math.max(0, targetY))
}

const stopCenterScroll = () => {
  if (centerScrollRaf) window.cancelAnimationFrame(centerScrollRaf)
  centerScrollRaf = 0
  centerScrollStopAt = 0
  isCenteringScroll = false
  ignoreAutoCollapseUntil = Math.max(ignoreAutoCollapseUntil, performance.now() + 140)
  window.removeEventListener('wheel', interruptCenterScroll)
  window.removeEventListener('touchstart', interruptCenterScroll)
  window.removeEventListener('keydown', interruptCenterScroll)
}

const scrollCardToCenter = async (opts?: { durationMs?: number }) => {
  await nextTick()
  stopCenterScroll()
  isCenteringScroll = true
  centerScrollUserInterrupted = false

  const durationMs = opts?.durationMs ?? EXPAND_COLLAPSE_MS
  ignoreAutoCollapseUntil = performance.now() + durationMs + 180
  centerScrollStopAt = performance.now() + durationMs
  window.addEventListener('wheel', interruptCenterScroll, { passive: true })
  window.addEventListener('touchstart', interruptCenterScroll, { passive: true })
  window.addEventListener('keydown', interruptCenterScroll)

  const tick = () => {
    const now = performance.now()
    const target = getCardTargetScrollY()
    if (target == null) return stopCenterScroll()

    const current = window.scrollY
    const remaining = Math.max(0, centerScrollStopAt - now)
    const alpha = remaining < 120 ? 0.28 : 0.18
    const next = current + (target - current) * alpha
    window.scrollTo({ top: next, behavior: 'auto' })

    if (now >= centerScrollStopAt || Math.abs(target - next) < 0.5) {
      window.scrollTo({ top: target, behavior: 'auto' })
      return stopCenterScroll()
    }
    centerScrollRaf = window.requestAnimationFrame(tick)
  }

  centerScrollRaf = window.requestAnimationFrame(tick)
}

const resetInnerScroll = () => {
  const el = scrollerRef.value
  if (!el) return
  el.scrollTo({ top: 0, behavior: 'auto' })
  lastInnerScrollTop = 0
  hasInnerUserScrolled.value = false
}

const collapse = () => {
  if (!isExpanded.value && !isVisuallyExpanded.value) return
  isVisuallyExpanded.value = false
  replyingTo.value = null
  summaryRequestId += 1
  isGenerating.value = false
  window.setTimeout(resetInnerScroll, 0)
  window.setTimeout(() => {
    if (!isVisuallyExpanded.value) {
      summary.value = ''
      displayedSummary.value = ''
    }
  }, SUMMARY_COLLAPSE_MS)
  window.setTimeout(() => {
    if (!isVisuallyExpanded.value) isExpanded.value = false
  }, EXPAND_COLLAPSE_MS)
}

const expand = () => {
  if (!isExpanded.value) isExpanded.value = true
  isVisuallyExpanded.value = true
  window.dispatchEvent(new CustomEvent(EXPAND_EVENT, { detail: { id: props.post.id } }))
  void scrollCardToCenter({ durationMs: EXPAND_COLLAPSE_MS })
  window.setTimeout(() => {
    if (!isVisuallyExpanded.value) return
    if (centerScrollUserInterrupted) return
    const target = getCardTargetScrollY()
    if (target == null) return
    if (Math.abs(window.scrollY - target) <= 2) return
    void scrollCardToCenter({ durationMs: 180 })
  }, EXPAND_COLLAPSE_MS + 80)
}

const toggleExpand = () => {
  if (isVisuallyExpanded.value) collapse()
  else expand()
}

const handleArticleLike = () => {
  if (!hasLikedArticle.value) {
    localArticleLikes.value += 1
    hasLikedArticle.value = true
  } else {
    localArticleLikes.value -= 1
    hasLikedArticle.value = false
  }

  likeBouncing.value = true
  window.setTimeout(() => {
    likeBouncing.value = false
  }, 280)
}

const handleCommentLike = (item: { likes: number; hasLiked?: boolean }) => {
  if (!item.hasLiked) {
    item.likes += 1
    item.hasLiked = true
  } else {
    item.likes = Math.max(0, item.likes - 1)
    item.hasLiked = false
  }
}

const scrollToCommentById = async (id: number) => {
  await nextTick()
  const container = scrollerRef.value
  if (!container) return
  const el = container.querySelector(`[data-comment-id="${id}"]`) as HTMLElement | null
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const initReply = async (comment: CommentItem) => {
  replyingTo.value = { rootId: comment.id }
  if (!isVisuallyExpanded.value) expand()
  await nextTick()
  inputRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  inputRef.value?.querySelector('textarea')?.focus()
}

const initReplyToReply = async (comment: CommentItem, reply: ReplyItem) => {
  replyingTo.value = { rootId: comment.id, toUser: reply.user }
  if (!isVisuallyExpanded.value) expand()
  await nextTick()
  inputRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  inputRef.value?.querySelector('textarea')?.focus()
}

const submitComment = async () => {
  if (!draftText.value.trim()) return

  const newId = Date.now()
  const newItem: ReplyItem = {
    id: newId,
    user: draftUser.value.trim() || 'Guest',
    text: draftText.value,
    likes: 0,
  }

  if (replyingTo.value) {
    const parent = allComments.value.find((c) => c.id === replyingTo.value?.rootId)
    if (parent) {
      const replyToAdd = { ...newItem, toUser: replyingTo.value.toUser }
      parent.replies.push(replyToAdd)
      ensureReplyState(parent.id)
      repliesState.value[parent.id]!.isOpen = true
      repliesVisible.value[parent.id]!.push(replyToAdd)
      repliesState.value[parent.id]!.hasMore = repliesVisible.value[parent.id]!.length < getRepliesTotal(parent.id)
      replyingTo.value = null
      draftText.value = ''
      await scrollToCommentById(newId)
      return
    }
  } else {
    const commentToAdd: CommentItem = { ...newItem, isHot: false, replies: [] }
    allComments.value.unshift(commentToAdd)
    comments.value.unshift(commentToAdd)
    if (comments.value.length > COMMENTS_PAGE_SIZE) comments.value.pop()
    commentsHasMore.value = comments.value.length < allComments.value.length
  }

  draftText.value = ''
  await scrollToCommentById(newId)
}

const generateAiSummary = async () => {
  if (isGenerating.value || summary.value) return
  if (!isVisuallyExpanded.value) {
    expand()
    await nextTick()
  }

  const requestId = (summaryRequestId += 1)
  displayedSummary.value = ''
  isGenerating.value = true

  try {
    const res = await fetch('/api/ai/summary', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: props.post.content }),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const data = (await res.json()) as { summary?: string }
    if (requestId !== summaryRequestId) return
    summary.value = data.summary || '无法生成摘要'
  } catch {
    if (requestId !== summaryRequestId) return
    await new Promise((r) => setTimeout(r, 1200))
    summary.value =
      '（演示模式：本地 /api/ai/summary 不可用）\n这是一段模拟摘要。你可以把摘要服务替换成真实的 Gemini / OpenAI 接口。'
  } finally {
    if (requestId === summaryRequestId) isGenerating.value = false
  }
}

watch(
  summary,
  (val) => {
    if (typingTimer) window.clearTimeout(typingTimer)
    displayedSummary.value = ''
    if (!val) return

    const tick = () => {
      if (summary.value !== val) return
      if (displayedSummary.value.length >= val.length) return
      displayedSummary.value = val.slice(0, displayedSummary.value.length + 1)
      typingTimer = window.setTimeout(tick, 15)
    }

    typingTimer = window.setTimeout(tick, 60)
  },
  { flush: 'post' },
)

let autoCollapseRaf = 0
const checkAutoCollapse = () => {
  if (!isVisuallyExpanded.value) return
  if (isCenteringScroll) return
  if (performance.now() < ignoreAutoCollapseUntil) return
  const el = cardRef.value
  if (!el) return

  const headerHeight = getFixedHeaderHeight()
  const thresholdY = headerHeight + (window.innerHeight - headerHeight) * 0.5
  const bottom = el.getBoundingClientRect().bottom
  if (bottom < thresholdY) collapse()
}

const onWindowScroll = () => {
  if (autoCollapseRaf) return
  autoCollapseRaf = window.requestAnimationFrame(() => {
    autoCollapseRaf = 0
    checkAutoCollapse()
  })
}

const onOtherCardExpand = (e: Event) => {
  const ce = e as CustomEvent<{ id?: number }>
  if (ce.detail?.id === props.post.id) return
  collapse()
}

watch(
  isExpanded,
  (expanded) => {
    if (expanded) window.addEventListener('scroll', onWindowScroll, { passive: true })
    else window.removeEventListener('scroll', onWindowScroll)
  },
  { flush: 'post' },
)

onMounted(() => {
  window.addEventListener(EXPAND_EVENT, onOtherCardExpand as EventListener)
})

onUnmounted(() => {
  window.removeEventListener(EXPAND_EVENT, onOtherCardExpand as EventListener)
  window.removeEventListener('scroll', onWindowScroll)
  if (autoCollapseRaf) window.cancelAnimationFrame(autoCollapseRaf)
  if (innerScrollRaf) window.cancelAnimationFrame(innerScrollRaf)
  if (typingTimer) window.clearTimeout(typingTimer)
  stopCenterScroll()
})
</script>

<template>
  <div ref="cardRef" class="mb-8 group transition-transform duration-300 hover:-translate-y-1">
    <div
      class="bg-white/90 dark:bg-[#1e1e1e]/90 backdrop-blur-xl rounded-xl shadow-lg border border-white/20 dark:border-white/10 overflow-hidden relative flex flex-col transition-[max-height] duration-[520ms] ease-[cubic-bezier(0.25,1,0.5,1)]"
      :style="{ maxHeight: isVisuallyExpanded ? `${EXPANDED_MAX_HEIGHT}px` : `${COLLAPSED_MAX_HEIGHT}px` }"
    >
      <div
        class="h-8 shrink-0 bg-gradient-to-b from-gray-100 to-gray-200 dark:from-[#3a3a3a] dark:to-[#2b2b2b] border-b border-gray-300 dark:border-black flex items-center px-4 justify-between z-20"
      >
        <div class="flex gap-2">
          <div class="w-3 h-3 rounded-full bg-[#FF5F56] border border-black/10"></div>
          <button
            type="button"
            class="w-3 h-3 rounded-full bg-[#FFBD2E] border border-black/10 hover:brightness-95 active:brightness-90 transition"
            title="展开/折叠"
            @click="toggleExpand"
          ></button>
          <div class="w-3 h-3 rounded-full bg-[#27C93F] border border-black/10"></div>
        </div>
        <div class="text-[11px] font-semibold text-gray-500 dark:text-gray-400 flex items-center gap-1 opacity-80">
          <i class="ph ph-file-text"></i>
          {{ post.title }}
        </div>
        <div class="w-10"></div>
      </div>

      <div class="relative flex-1">
        <div
          class="h-full p-6 mac-scrollbar transition-all duration-300"
          :style="{ maxHeight: isVisuallyExpanded ? `${EXPANDED_MAX_HEIGHT}px` : `${COLLAPSED_INNER_MAX_HEIGHT}px` }"
          :class="[
            isVisuallyExpanded ? 'overflow-y-auto' : 'overflow-hidden',
            !isVisuallyExpanded && showExpandBtn ? 'pb-20' : '',
          ]"
          ref="scrollerRef"
        >
          <div class="mb-2">
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

            <div
              v-show="isGenerating || summary"
              class="mac-summary relative mb-6 group/summary"
            >
              <div
                class="absolute -inset-0.5 rounded-xl bg-gradient-to-r from-[#4E8AFF] via-[#A855F7] to-[#FF5D9E] blur-sm transition-opacity duration-500"
                :class="isGenerating ? 'animate-pulse opacity-40' : 'opacity-20 group-hover/summary:opacity-30'"
              ></div>

              <div
                class="mac-summary-shell relative rounded-xl"
                :class="isVisuallyExpanded ? 'mac-summary--open' : 'mac-summary--closed'"
              >
                <div
                class="relative bg-white/90 dark:bg-[#252527]/90 backdrop-blur-xl border border-white/50 dark:border-white/10 p-4 rounded-xl shadow-lg"
              >
                <div class="flex items-center gap-2 mb-3">
                  <svg
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                    class="animate-pulse"
                  >
                    <path
                      d="M12 3L14.5 9.5L21 12L14.5 14.5L12 21L9.5 14.5L3 12L9.5 9.5L12 3Z"
                      :fill="`url(#${geminiGradientId})`"
                    />
                    <defs>
                      <linearGradient :id="geminiGradientId" x1="3" y1="3" x2="21" y2="21" gradientUnits="userSpaceOnUse">
                        <stop stop-color="#4E8AFF" />
                        <stop offset="0.5" stop-color="#A855F7" />
                        <stop offset="1" stop-color="#FF5D9E" />
                      </linearGradient>
                    </defs>
                  </svg>

                  <span
                    class="text-[10px] font-bold tracking-[0.2em] uppercase text-transparent bg-clip-text bg-gradient-to-r from-[#4E8AFF] to-[#FF5D9E]"
                  >
                    AI Summary
                  </span>
                </div>

                <div class="text-[14px] leading-relaxed text-gray-700 dark:text-gray-300 font-medium">
                  <div v-if="isGenerating" class="space-y-2">
                    <div class="h-3 bg-gray-200 dark:bg-white/10 rounded w-full animate-pulse"></div>
                    <div class="h-3 bg-gray-200 dark:bg-white/10 rounded w-[80%] animate-pulse"></div>
                  </div>
                  <p v-else class="whitespace-pre-wrap mac-summary-reveal">
                    {{ displayedSummary }}
                    <span
                      v-if="displayedSummary.length < summary.length"
                      class="inline-block w-1.5 h-4 ml-1 bg-blue-400 animate-pulse align-middle rounded-sm"
                    ></span>
                  </p>
                </div>

                <div class="mt-4 pt-3 border-t border-gray-100 dark:border-white/5 flex justify-end">
                  <span class="text-[9px] text-gray-400 dark:text-gray-500">Ai的回答未必正确无误，请注意核查</span>
                </div>
              </div>
              </div>
            </div>
          </div>

          <div
            class="prose dark:prose-invert text-[15px] leading-relaxed font-sans max-w-none transition-all duration-300"
            :class="[!isVisuallyExpanded && showExpandBtn ? 'mac-collapsed-preview' : 'mac-expanded-view']"
            v-html="renderMarkdown(post.content)"
          ></div>

	          <div
	            v-show="isExpanded"
	            class="mt-8 pt-6 pb-10 border-t border-gray-100 dark:border-white/5 space-y-6 transition-[opacity,transform] duration-300"
	            :class="isVisuallyExpanded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-1 pointer-events-none'"
	          >
		            <h4 class="text-sm font-bold text-gray-400 uppercase tracking-widest mb-4">Comments ({{ allComments.length }})</h4>

	            <div v-for="comment in comments" :key="comment.id" class="animate-[fadeIn_0.3s_ease-out]" :data-comment-id="comment.id">
	              <div class="flex gap-3">
	                <AvatarCircle :name="comment.user" size="md" class="shrink-0 mt-1" />

	                <div class="flex flex-col items-start max-w-[85%] flex-1">
	                  <div class="flex items-center gap-2 mb-1">
	                    <span class="text-sm font-medium text-gray-500 dark:text-gray-400">{{ comment.user }}</span>
	                    <span v-if="comment.isHot" class="bg-red-500 text-white text-[9px] px-1 rounded font-bold">HOT</span>
	                  </div>
	                  <div
	                    class="bg-[#e9e9eb] dark:bg-[#3a3a3c] px-3 py-2 rounded-2xl rounded-tl-sm text-[14px] text-gray-800 dark:text-gray-100 leading-relaxed mb-1"
	                  >
	                    {{ comment.text }}
	                  </div>
	                  <div class="flex gap-4 px-1">
	                    <button
	                      class="flex items-center gap-1 text-[11px] font-medium text-gray-400 hover:text-red-500 transition-colors"
	                      :class="{ 'text-red-500': comment.hasLiked }"
	                      @click="handleCommentLike(comment)"
	                    >
	                      <i :class="comment.hasLiked ? 'ph-fill ph-heart' : 'ph ph-heart'"></i>
	                      {{ comment.likes }}
	                    </button>
	                    <button class="text-[11px] font-medium text-gray-400 hover:text-blue-500 transition-colors" @click="initReply(comment)">
	                      回复
	                    </button>
	                  </div>
	                </div>
	              </div>

		              <div v-if="getRepliesTotal(comment.id)" class="ml-11 mt-3 pl-3 border-l-2 border-gray-100 dark:border-white/5">
		                <button
		                  v-if="!isRepliesOpen(comment.id)"
		                  class="text-[11px] font-semibold text-blue-500 hover:text-blue-600 transition-colors"
		                  :disabled="isRepliesLoading(comment.id)"
		                  @click="loadMoreReplies(comment.id)"
		                >
		                  {{ isRepliesLoading(comment.id) ? '加载中...' : `查看更多回复 (${getRepliesTotal(comment.id)})` }}
		                </button>

		                <div v-else class="space-y-3">
		                  <div v-for="reply in getVisibleReplies(comment.id)" :key="reply.id" class="flex gap-2" :data-comment-id="reply.id">
		                    <AvatarCircle :name="reply.user" size="xs" class="shrink-0 mt-1" />
		                    <div class="min-w-0 flex-1">
		                      <div class="flex items-center gap-2 mb-0.5">
		                        <span class="text-[12px] font-bold text-gray-500 dark:text-gray-400">
		                          {{ reply.user }}<span v-if="reply.toUser" class="font-medium opacity-80"> to {{ reply.toUser }}</span>
		                        </span>
		                      </div>
		                      <div
		                        class="text-[13px] text-gray-700 dark:text-gray-300 leading-relaxed bg-gray-50 dark:bg-white/5 px-2.5 py-2 rounded-lg rounded-tl-none inline-block"
		                      >
		                        {{ reply.text }}
		                      </div>
		                      <div class="flex gap-3 px-1 mt-0.5">
		                        <button
		                          class="flex items-center gap-1 text-[11px] text-gray-400 hover:text-red-500 transition-colors"
		                          :class="{ 'text-red-500': reply.hasLiked }"
		                          @click="handleCommentLike(reply)"
		                        >
		                          <i :class="reply.hasLiked ? 'ph-fill ph-heart' : 'ph ph-heart'"></i>
		                          {{ reply.likes }}
		                        </button>
		                        <button
		                          class="text-[11px] font-medium text-gray-400 hover:text-blue-500 transition-colors"
		                          @click="initReplyToReply(comment, reply)"
		                        >
		                          回复
		                        </button>
		                      </div>
		                    </div>
		                  </div>

		                  <button
		                    v-if="repliesHasMore(comment.id)"
		                    class="text-[11px] font-semibold text-blue-500 hover:text-blue-600 transition-colors"
		                    :disabled="isRepliesLoading(comment.id)"
		                    @click="loadMoreReplies(comment.id)"
		                  >
		                    {{ isRepliesLoading(comment.id) ? '加载中...' : '查看更多' }}
		                  </button>
		                </div>
		              </div>
		            </div>

		            <div v-if="commentsLoading || commentsHasMore" class="text-center text-[11px] text-gray-400">
		              <span v-if="commentsLoading">加载更多评论中...</span>
		              <span v-else>滑动到底加载更多评论</span>
		            </div>

            <div
                ref="inputRef"
                class="sticky bottom-3 z-50 pt-3 pb-3 px-6 -mx-6 bg-gradient-to-t from-white via-white/95 to-transparent dark:from-[#1e1e1e] dark:via-[#1e1e1e]/95 backdrop-blur-sm"
            >
              <div class="flex items-end gap-2">
                <div
                    class="flex-1 bg-gray-100 dark:bg-[#2c2c2e] p-1 rounded-[20px] border border-transparent focus-within:border-blue-500/30 transition-all flex flex-col shadow-inner"
                >
                  <div
                      v-if="replyingTo"
	                      class="px-3 py-1 text-[11px] text-blue-500 flex justify-between items-center bg-blue-50 dark:bg-blue-900/20 rounded-t-[16px] mb-1"
	                  >
                    <span>
                      回复<span v-if="replyingTo.toUser" class="font-bold"> @{{ replyingTo.toUser }}</span
                      ><span v-else class="font-bold"> #{{ replyingTo.rootId }}</span
                      >...
                    </span>
                    <button class="hover:text-red-500" @click="replyingTo = null">
                      <i class="ph ph-x"></i>
                    </button>
                  </div>
                  <input
                      v-model="draftUser"
                      placeholder="Nickname..."
                      class="bg-transparent text-[10px] text-gray-500 dark:text-gray-400 px-3 py-1 outline-none w-full border-b border-gray-200 dark:border-white/5 mb-1 placeholder-gray-400/70"
                  />
                  <textarea
                      v-model="draftText"
                      :placeholder="replyingTo ? (replyingTo.toUser ? `回复 @${replyingTo.toUser}...` : `回复 #${replyingTo.rootId}...`) : '发表评论...'"
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
                  <i class="ph ph-paper-plane-right font-bold"></i>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div
          v-show="showExpandBtn"
          class="absolute bottom-0 left-0 w-full h-24 bg-gradient-to-t from-white dark:from-[#1e1e1e] via-white/90 dark:via-[#1e1e1e]/90 to-transparent flex items-end justify-center pb-4 z-30 pointer-events-none transition-opacity duration-[520ms]"
          :class="isVisuallyExpanded ? 'opacity-0' : 'opacity-100'"
        >
          <button
            class="px-6 py-2 rounded-full text-xs font-semibold bg-white dark:bg-[#3a3a3c] text-gray-600 dark:text-gray-200 border border-gray-200 dark:border-white/10 shadow-lg hover:scale-105 active:scale-95 transition-all flex items-center gap-2 mb-2"
            :class="isVisuallyExpanded ? 'pointer-events-none' : 'pointer-events-auto'"
            @click="toggleExpand"
          >
            <span>展开阅读</span>
            <i class="ph ph-caret-down"></i>
          </button>
        </div>
      </div>

      <div
        class="bg-gray-50/50 dark:bg-white/5 px-6 py-3 border-t border-gray-100 dark:border-white/10 flex justify-between items-center backdrop-blur-sm z-40 relative"
      >
        <div class="flex gap-4">
          <button class="flex items-center gap-1.5 text-gray-400 hover:text-red-500 transition-colors group" @click="handleArticleLike">
            <div class="relative">
              <i
                class="ph-heart text-xl transition-transform duration-300"
                :class="[
                  hasLikedArticle ? 'ph-fill text-red-500 scale-110' : 'ph group-hover:scale-110',
                  likeBouncing ? 'mac-like-bounce' : '',
                ]"
              ></i>
              <div v-if="hasLikedArticle" class="absolute inset-0 animate-ping opacity-75 rounded-full bg-red-400 scale-150"></div>
            </div>
            <span class="text-xs font-medium" :class="{ 'text-red-500': hasLikedArticle }">{{ localArticleLikes }}</span>
          </button>
          <button class="flex items-center gap-1.5 text-gray-400 hover:text-blue-500 transition-colors group" @click="toggleExpand">
            <i class="ph ph-chat-circle text-xl group-hover:scale-110 transition-transform"></i>
            <span class="text-xs font-medium">{{ allComments.length }}</span>
          </button>
        </div>
        <button class="text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 transition-colors" title="分享">
          <i class="ph ph-share-network text-xl"></i>
        </button>
      </div>

    </div>
  </div>
</template>

<style scoped>
.mac-summary-shell {
  overflow: hidden;
  transition:
    max-height 0.36s cubic-bezier(0.25, 1, 0.5, 1),
    opacity 0.24s ease,
    margin-top 0.36s cubic-bezier(0.25, 1, 0.5, 1);
}

.mac-summary--open {
  max-height: 360px;
  opacity: 1;
  margin-top: 0.75rem;
}

.mac-summary--closed {
  max-height: 0;
  opacity: 0;
  margin-top: 0;
}

.mac-like-bounce {
  animation: likeBounce 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.mac-summary-reveal {
  animation: summaryReveal 0.7s cubic-bezier(0.25, 1, 0.5, 1) both;
}

@keyframes likeBounce {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
}

@keyframes summaryReveal {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.mac-collapsed-preview {
  position: relative;
  max-height: 240px;
  overflow: hidden;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 1) calc(100% - 80px), rgba(0, 0, 0, 0));
  -webkit-mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 1) calc(100% - 80px), rgba(0, 0, 0, 0));
  transition: max-height 0.52s cubic-bezier(0.25, 1, 0.5, 1);
}

.mac-expanded-view {
  max-height: 3000px;
  mask-image: none;
  -webkit-mask-image: none;
}

.mac-line-clamp-2 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mac-line-clamp-2 {
  -webkit-line-clamp: 2;
}
</style>
