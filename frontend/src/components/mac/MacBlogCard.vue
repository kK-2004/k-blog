<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, watchEffect } from 'vue'
import type { Post, AdminMe } from '@/api/types'
import { renderMarkdown } from '@/composables/useMarkdown'
import { useReadStats } from '@/composables/useReadStats'
import { useHashRouter } from '@/composables/useHashRouter'
import { useMessage } from '@/composables/useMessage'
import AvatarCircle from './AvatarCircle.vue'
import ImageLightbox from './ImageLightbox.vue'
import { incrementLikes, incrementViews } from '@/api/posts'
import { createPostComment, createPostCommentReply, likePostComment, listPostComments } from '@/api/comments'

const { success } = useMessage()

// 图片预览
const lightboxImageUrl = ref('')

const handleContentImageClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.tagName === 'IMG') {
    const img = target as HTMLImageElement
    lightboxImageUrl.value = img.src
  }
}

const closeLightbox = () => {
  lightboxImageUrl.value = ''
}

type ReplyItem = {
  id: number
  user: string
  text: string
  likes: number
  hasLiked?: boolean
  toUser?: string
  createdAt: number
}

type CommentItem = ReplyItem & {
  isHot: boolean
  replies: ReplyItem[]
}

const props = defineProps<{
  post: Post
  isAuthenticated: boolean
  adminMe: AdminMe | null
  authorAvatarUrl: string | null
  scrollContainer?: HTMLElement | null
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
const hasCountedView = ref(false)

watch(
    () => props.post.likes,
    (likes) => {
      if (!hasLikedArticle.value) localArticleLikes.value = likes
    },
)

// 监听登录状态变化，自动填充用户名
watch(
    () => props.adminMe,
    (me) => {
      if (me && props.isAuthenticated) {
        draftUser.value = me.username
      }
    },
    { immediate: true }
)

// 路由导航
const router = useHashRouter()
const navigateToArticle = () => {
  router.navigateTo('article', { articleId: props.post.id })
}

const summary = ref('')
const displayedSummary = ref('')
const isGenerating = ref(false)
const isStreamingSummary = ref(false)
const hasReceivedSummaryData = ref(false) // 是否已收到流式响应数据
let summaryRequestId = 0

const geminiGradientId = computed(() => `gemini_grad_${props.post.id}`)

const displayPostTime = computed(() => {
  const ms = props.post.createdAt
  if (typeof ms === 'number' && Number.isFinite(ms)) {
    const diffMs = Date.now() - ms
    if (diffMs < 60_000) return '刚刚'
    const diffMinutes = Math.floor(diffMs / 60_000)
    if (diffMinutes < 60) {
      const rounded = Math.max(5, Math.floor(diffMinutes / 5) * 5)
      return `${rounded}分钟前`
    }
    return new Date(ms).toLocaleString('zh-CN')
  }
  return ''
})

// 格式化评论时间
const formatCommentTime = (createdAt: number) => {
  const diffMs = Date.now() - createdAt
  if (diffMs < 60_000) return '刚刚'
  const diffMinutes = Math.floor(diffMs / 60_000)
  if (diffMinutes < 60) {
    return `${diffMinutes}分钟前`
  }
  const diffHours = Math.floor(diffMs / (60 * 60_000))
  if (diffHours < 24) {
    return `${diffHours}小时前`
  }
  const diffDays = Math.floor(diffMs / (24 * 60 * 60_000))
  if (diffDays < 7) {
    return `${diffDays}天前`
  }
  return new Date(createdAt).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 获取评论者头像URL
const getCommenterAvatarUrl = (username: string) => {
  // 如果是登录用户且用户名匹配，使用登录用户的头像
  if (props.isAuthenticated && props.adminMe && username === props.adminMe.username) {
    return props.adminMe.avatarUrl
  }
  // 如果是作者，使用作者头像
  if (username === props.post.author) {
    return props.authorAvatarUrl
  }
  // 否则返回 null，使用默认头像
  return null
}

// 渲染 AI 摘要为 Markdown
const renderedSummaryHtml = computed(() => {
  // 如果正在流式输出，返回纯文本（不渲染 Markdown，让光标能跟随）
  if (isStreamingSummary.value) {
    return displayedSummary.value
  }
  // 流式输出完成后，直接渲染 Markdown（不等待打字效果）
  return renderMarkdown(displayedSummary.value)
})

// 判断是否应该显示 Markdown
const shouldRenderMarkdown = computed(() => {
  // 只要不正在流式输出且已有内容，就渲染 Markdown
  return !isStreamingSummary.value && displayedSummary.value.length > 0
})

const showExpandBtn = computed(() => {
  const text = props.post.content || ''
  return text.length > 100 || text.includes('```')
})

const shouldShowFloatingButtons = computed(() => {
  const text = props.post.content || ''
  return text.length > 200
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
}

const applyComments = (items: CommentItem[]) => {
  // 排序逻辑：热门评论优先，然后按时间倒序（最新的在前）
  allComments.value = items.sort((a, b) => {
    // 热门评论优先
    if (a.isHot && !b.isHot) return -1
    if (!a.isHot && b.isHot) return 1
    // 同为热门或非热门时，按时间倒序
    return b.createdAt - a.createdAt
  })
  resetCommentPaging()
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

// 折叠回复
const toggleReplies = (commentId: number) => {
  ensureReplyState(commentId)
  const state = repliesState.value[commentId]
  if (state) {
    state.isOpen = !state.isOpen
    // 如果折叠，清空可见回复；如果展开，加载第一页
    if (!state.isOpen) {
      repliesVisible.value[commentId] = []
    } else {
      void loadMoreReplies(commentId)
    }
  }
}

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



watch(
    () => props.post.id,
    () => {
      void (async () => {
        try {
          const data = await listPostComments(props.post.id)
          if (data.length) {
            applyComments(
              data.map((c) => ({
                id: c.id,
                user: c.user,
                text: c.text,
                likes: c.likes,
                hasLiked: false,
                isHot: c.hot,
                createdAt: c.createdAt,
                replies: (c.replies ?? [])
                  .map((r) => ({
                    id: r.id,
                    user: r.user,
                    text: r.text,
                    likes: r.likes,
                    hasLiked: false,
                    toUser: r.toUser ?? undefined,
                    createdAt: r.createdAt,
                  }))
                  .sort((a, b) => b.createdAt - a.createdAt), // 回复也按时间倒序
              })),
            )
            return
          }
        } catch {
          // ignore
        }
        applyComments([])
      })()
    },
    { immediate: true, deep: false },
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

  // 使用 scrollContainer.scrollTop 或 window.scrollY
  const currentScroll = props.scrollContainer?.scrollTop ?? window.scrollY
  const targetY = currentScroll + (elementCenterY - viewportCenterY)

  // 计算最大滚动值
  const scrollContainer = props.scrollContainer ?? document.documentElement
  const maxScrollY = Math.max(0, scrollContainer.scrollHeight - window.innerHeight)

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

    // 获取当前滚动位置
    const scrollContainer = props.scrollContainer
    const current = scrollContainer?.scrollTop ?? window.scrollY

    const remaining = Math.max(0, centerScrollStopAt - now)
    const alpha = remaining < 120 ? 0.28 : 0.18
    const next = current + (target - current) * alpha

    // 使用正确的滚动容器
    if (scrollContainer) {
      scrollContainer.scrollTop = next
    } else {
      window.scrollTo({ top: next, behavior: 'auto' })
    }

    if (now >= centerScrollStopAt || Math.abs(target - next) < 0.5) {
      if (scrollContainer) {
        scrollContainer.scrollTop = target
      } else {
        window.scrollTo({ top: target, behavior: 'auto' })
      }
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
      hasReceivedSummaryData.value = false // 重置数据接收状态
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
  if (!hasCountedView.value) {
    hasCountedView.value = true
    void incrementViews(props.post.id).catch(() => {})
  }
  void scrollCardToCenter({ durationMs: EXPAND_COLLAPSE_MS })
  window.setTimeout(() => {
    if (!isVisuallyExpanded.value) return
    if (centerScrollUserInterrupted) return
    const target = getCardTargetScrollY()
    if (target == null) return
    const current = props.scrollContainer?.scrollTop ?? window.scrollY
    if (Math.abs(current - target) <= 2) return
    void scrollCardToCenter({ durationMs: 180 })
  }, EXPAND_COLLAPSE_MS + 80)

  // 检查是否首次展开，显示折叠提示
  const storageKey = 'mac-blog-card-collapse-hint-shown'
  if (!localStorage.getItem(storageKey)) {
    setTimeout(() => {
      showCollapseHint.value = true
      updateHintPosition()
    }, EXPAND_COLLAPSE_MS + 200)
  }
}

const toggleExpand = () => {
  if (isVisuallyExpanded.value) collapse()
  else expand()
}

const handleArticleLike = () => {
  if (hasLikedArticle.value) return

  localArticleLikes.value += 1
  hasLikedArticle.value = true
  void incrementLikes(props.post.id).catch(() => {
    localArticleLikes.value = Math.max(0, localArticleLikes.value - 1)
    hasLikedArticle.value = false
  })

  likeBouncing.value = true
  window.setTimeout(() => {
    likeBouncing.value = false
  }, 280)
}

const handleShare = async () => {
  const url = window.location.href.split('#')[0] + `#/article/${props.post.id}`
  try {
    await navigator.clipboard.writeText(url)
    success('链接已复制到剪贴板')
  } catch {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = url
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    success('链接已复制到剪贴板')
  }
}

const handleCommentClick = async () => {
  // 如果未展开，先展开
  if (!isVisuallyExpanded.value) {
    expand()
    // 等待展开动画完成
    await new Promise(resolve => setTimeout(resolve, EXPAND_COLLAPSE_MS))
  }

  // 滚动到评论区顶部（保留标题栏可见）
  const container = scrollerRef.value
  if (!container) return

  // 计算滚动位置：评论区距离顶部的偏移量
  const commentsSection = container.querySelector('h4.text-sm.font-bold.text-gray-400.uppercase') as HTMLElement
  if (commentsSection) {
    const containerRect = container.getBoundingClientRect()
    const commentsRect = commentsSection.getBoundingClientRect()

    // 计算需要滚动的距离
    const scrollTop = container.scrollTop + (commentsRect.top - containerRect.top)
    container.scrollTo({ top: scrollTop, behavior: 'smooth' })
  }
}

const handleCommentLike = (item: { id: number; likes: number; hasLiked?: boolean }) => {
  if (item.hasLiked) return
  item.likes += 1
  item.hasLiked = true
  void likePostComment(props.post.id, item.id).catch(() => {
    item.likes = Math.max(0, item.likes - 1)
    item.hasLiked = false
  })
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

  const user = draftUser.value.trim() || 'Guest'
  const text = draftText.value

  draftText.value = ''

  if (replyingTo.value) {
    const rootId = replyingTo.value.rootId
    const parent = allComments.value.find((c) => c.id === rootId)
    if (!parent) return

    const created = await createPostCommentReply(props.post.id, rootId, {
      user,
      text,
      toUser: replyingTo.value.toUser,
    })

    const replyToAdd: ReplyItem = {
      id: created.id,
      user: created.user,
      text: created.text,
      likes: created.likes,
      toUser: created.toUser ?? undefined,
      hasLiked: false,
      createdAt: created.createdAt,
    }

    parent.replies.unshift(replyToAdd)
    ensureReplyState(parent.id)
    repliesState.value[parent.id]!.isOpen = true
    repliesVisible.value[parent.id]!.unshift(replyToAdd)
    repliesState.value[parent.id]!.hasMore = repliesVisible.value[parent.id]!.length < getRepliesTotal(parent.id)
    replyingTo.value = null
    await scrollToCommentById(created.id)
    return
  } else {
    const created = await createPostComment(props.post.id, { user, text })
    const commentToAdd: CommentItem = {
      id: created.id,
      user: created.user,
      text: created.text,
      likes: created.likes,
      hasLiked: false,
      isHot: created.hot,
      replies: [],
      createdAt: created.createdAt,
    }
    allComments.value.unshift(commentToAdd)
    comments.value.unshift(commentToAdd)
    if (comments.value.length > COMMENTS_PAGE_SIZE) comments.value.pop()
    commentsHasMore.value = comments.value.length < allComments.value.length
  }
}

const generateAiSummary = async () => {
  if (isGenerating.value || summary.value) return
  if (!isVisuallyExpanded.value) {
    expand()
    await nextTick()
  }

  const requestId = (summaryRequestId += 1)
  displayedSummary.value = ''
  summary.value = ''
  isGenerating.value = true
  isStreamingSummary.value = true
  hasReceivedSummaryData.value = false // 重置：还未收到数据

  try {
    const res = await fetch('/api/ai/summary/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: props.post.content }),
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
      hasReceivedSummaryData.value = true // 已收到数据
      // 不要在这里设置 isGenerating = false，应该在整个流式输出完成后才设置
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
    hasReceivedSummaryData.value = true // 模拟数据也算收到数据
  } finally {
    if (requestId === summaryRequestId) {
      isGenerating.value = false
      isStreamingSummary.value = false
    }
  }
}

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
  stopCenterScroll()
})

const scrollToCommentInput = async () => {
  if (!isVisuallyExpanded.value) {
    expand()
    await nextTick()
  }
  await nextTick()
  inputRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  inputRef.value?.querySelector('textarea')?.focus()
}

// 首次展开提示相关
const showCollapseHint = ref(false)
const yellowButtonRef = ref<HTMLElement | null>(null)
const hintPosition = ref({ top: 0, left: 0 })
const arrowPath = ref('')
const svgSize = ref({ width: 0, height: 0 })
const arrowTipPosition = ref({ x: 0, y: 0 })

const updateHintPosition = () => {
  if (!yellowButtonRef.value) return

  const targetRect = yellowButtonRef.value.getBoundingClientRect()
  const guideWidth = 260
  const guideHeight = 100
  const horizontalGap = 50  // 增加间距

  // 提示框显示在黄灯按钮左侧
  hintPosition.value = {
    top: targetRect.top + targetRect.height / 2 - guideHeight / 2,
    left: targetRect.left - guideWidth - horizontalGap
  }

  // SVG 箭头从提示框右侧指向黄灯按钮（使用曲线）
  const arrowStartX = hintPosition.value.left + guideWidth
  const arrowStartY = hintPosition.value.top + guideHeight / 2
  const arrowEndX = targetRect.left - 5
  const arrowEndY = targetRect.top + targetRect.height / 2

  // 控制点，形成平滑的 S 形曲线
  const controlX1 = arrowStartX - (arrowStartX - arrowEndX) * 0.3
  const controlY1 = arrowStartY
  const controlX2 = arrowEndX + (arrowStartX - arrowEndX) * 0.3
  const controlY2 = arrowEndY

  arrowTipPosition.value = { x: arrowEndX, y: arrowEndY }
  arrowPath.value = `M ${arrowStartX} ${arrowStartY} C ${controlX1} ${controlY1}, ${controlX2} ${controlY2}, ${arrowEndX} ${arrowEndY}`

  svgSize.value = {
    width: Math.max(hintPosition.value.left + guideWidth + 50, targetRect.right + 100),
    height: Math.max(hintPosition.value.top + guideHeight + 50, targetRect.bottom + 50)
  }
}

const closeCollapseHint = () => {
  localStorage.setItem('mac-blog-card-collapse-hint-shown', 'true')
  showCollapseHint.value = false
}
</script>

<template>
  <div ref="cardRef" class="mb-8 group/card transition-transform duration-300 hover:-translate-y-1 w-full lg:max-w-[490px]">
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
              ref="yellowButtonRef"
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
        <div class="w-10 flex justify-end">
          <svg
              v-if="post.pinned"
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              class="text-red-500"
              title="置顶"
          >
            <path
                d="M16.5 3.5L12 7L7.5 3.5V3.5C7.5 2.67 8.17 2 9 2H15C15.83 2 16.5 2.67 16.5 3.5V3.5Z"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
            />
            <path
                d="M12 7V22"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
            />
            <path
                d="M9 12H15"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
            />
          </svg>
        </div>
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
              <!-- 作者头像：优先使用公开资料头像 -->
              <img
                v-if="authorAvatarUrl"
                :src="authorAvatarUrl"
                :alt="post.author"
                class="w-10 h-10 rounded-full object-cover shadow-sm"
                draggable="false"
              />
              <AvatarCircle v-else :name="post.author" size="lg" />
              <div class="flex-1">
                <div class="text-sm font-bold text-gray-800 dark:text-gray-100">{{ post.author }}</div>
                <div class="text-xs text-gray-400 font-mono">{{ displayPostTime }}</div>
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
                    <!-- 加载动画：只在还未收到任何响应时显示 -->
                    <div v-show="!hasReceivedSummaryData" class="space-y-2">
                      <div class="h-3 bg-gray-200 dark:bg-white/10 rounded w-full animate-pulse"></div>
                      <div class="h-3 bg-gray-200 dark:bg-white/10 rounded w-[80%] animate-pulse"></div>
                    </div>

                    <!-- 内容显示：收到数据后显示 -->
                    <div v-show="hasReceivedSummaryData" class="mac-summary-reveal">
                      <!-- Markdown 渲染：流式输出完成后显示 -->
                      <div v-if="shouldRenderMarkdown" class="markdown-content prose prose-sm dark:prose-invert max-w-none">
                        <div v-html="renderedSummaryHtml"></div>
                      </div>

                      <!-- 纯文本 + 光标：流式输出时显示 -->
                      <div v-else>
                        <p class="whitespace-pre-wrap">
                          {{ displayedSummary }}<span
                            class="inline-block w-1.5 h-4 ml-0.5 bg-blue-400 animate-pulse align-middle rounded-sm"
                        ></span>
                        </p>
                      </div>
                    </div>
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
              @click="handleContentImageClick"
          ></div>

          <div
              v-show="isExpanded"
              class="mt-8 pt-6 border-t border-gray-100 dark:border-white/5 transition-[opacity,transform] duration-300"
              :class="isVisuallyExpanded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-1 pointer-events-none'"
          >
            <h4 class="text-sm font-bold text-gray-400 uppercase tracking-widest mb-4">Comments ({{ allComments.length }})</h4>
            <!-- 评论区 -->
            <!-- 移除 max-h 和 overflow-y-auto -->
            <div class="space-y-6 pr-2">
              <div v-for="comment in comments" :key="comment.id" class="animate-[fadeIn_0.3s_ease-out]" :data-comment-id="comment.id">
                <div class="flex gap-3">
                  <!-- 评论者头像 -->
                  <img
                      v-if="getCommenterAvatarUrl(comment.user)"
                      :src="getCommenterAvatarUrl(comment.user)!"
                      :alt="comment.user"
                      class="w-8 h-8 rounded-full object-cover shadow-sm shrink-0 mt-1"
                      draggable="false"
                  />
                  <AvatarCircle v-else :name="comment.user" size="md" class="shrink-0 mt-1" />

                  <div class="flex flex-col items-start max-w-[85%] flex-1">
                    <div class="flex items-center gap-2 mb-1">
                      <span class="text-sm font-medium text-gray-500 dark:text-gray-400">{{ comment.user }}</span>
                      <span v-if="comment.isHot" class="bg-red-500 text-white text-[9px] px-1 rounded font-bold">HOT</span>
                      <span class="text-[11px] text-gray-400">{{ formatCommentTime(comment.createdAt) }}</span>
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
                      @click="toggleReplies(comment.id)"
                  >
                    {{ isRepliesLoading(comment.id) ? '加载中...' : `查看回复 (${getRepliesTotal(comment.id)})` }}
                  </button>

                  <div v-else class="flex items-center gap-2 mb-2">
                    <button
                        class="text-[11px] font-semibold text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
                        @click="toggleReplies(comment.id)"
                    >
                      <i class="ph ph-caret-up mr-1"></i>
                      收起回复
                    </button>
                  </div>

                  <div v-if="isRepliesOpen(comment.id)" class="space-y-3">
                    <div v-for="reply in getVisibleReplies(comment.id)" :key="reply.id" class="flex gap-2" :data-comment-id="reply.id">
                      <img
                          v-if="getCommenterAvatarUrl(reply.user)"
                          :src="getCommenterAvatarUrl(reply.user)!"
                          :alt="reply.user"
                          class="w-6 h-6 rounded-full object-cover shadow-sm shrink-0 mt-1"
                          draggable="false"
                      />
                      <AvatarCircle v-else :name="reply.user" size="xs" class="shrink-0 mt-1" />
                      <div class="min-w-0 flex-1">
                        <div class="flex items-center gap-2 mb-0.5">
              <span class="text-[12px] font-bold text-gray-500 dark:text-gray-400">
                {{ reply.user }}<span v-if="reply.toUser" class="font-medium opacity-80"> to {{ reply.toUser }}</span>
              </span>
                          <span class="text-[10px] text-gray-400">{{ formatCommentTime(reply.createdAt) }}</span>
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

              <!-- 加载更多按钮/提示 -->
              <div v-if="commentsHasMore || commentsLoading" class="text-center py-4">
                <button
                    v-if="!commentsLoading"
                    @click="loadMoreComments"
                    class="text-xs text-blue-500 hover:text-blue-600 font-semibold px-4 py-2 rounded-lg hover:bg-blue-50 dark:hover:bg-blue-500/10 transition-colors"
                >
                  加载更多评论 <i class="ph ph-caret-down ml-1"></i>
                </button>
                <span v-else class="text-xs text-gray-400 flex items-center justify-center gap-2">
      <i class="ph ph-spinner-gap animate-spin"></i>
      加载中...
    </span>
              </div>
            </div>

            <div
                ref="inputRef"
                class="sticky bottom-0 z-50 pt-3 pb-6 px-6 -mx-6 bg-gradient-to-t from-white via-white/95 to-transparent dark:from-[#1e1e1e] dark:via-[#1e1e1e]/95 backdrop-blur-sm"
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

        <!-- 全屏阅读按钮（展开时显示在右上角） -->
        <button
          v-if="isExpanded"
          @click="navigateToArticle"
          class="absolute top-6 right-6 px-3 py-1.5 text-xs font-medium bg-white/95 dark:bg-white/10 border border-gray-200 dark:border-white/10 rounded-lg hover:border-blue-400 dark:hover:border-blue-500/50 hover:text-blue-600 dark:hover:text-blue-400 transition-all flex items-center gap-1.5 shadow-sm z-30 backdrop-blur-sm"
        >
          <i class="ph ph-arrows-out-simple"></i>
          <span>全屏阅读</span>
        </button>

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
          style="min-height: 52px;"
      >
        <div class="flex gap-4 items-center"> <!-- 添加 items-center -->
          <button class="flex items-center gap-1.5 text-gray-400 hover:text-red-500 transition-colors group/praise" @click="handleArticleLike">
            <!-- 关键：添加固定尺寸的容器 -->
            <span class="inline-flex items-center justify-center w-5 h-5 relative">
        <i
            class="text-xl transition-transform duration-300 absolute"
            :class="[
              hasLikedArticle ? 'ph-fill ph-heart text-red-500' : 'ph ph-heart',
              likeBouncing ? 'mac-like-bounce' : '',
              !hasLikedArticle ? 'group-hover/praise:scale-110' : '',
            ]"
            style="transform-origin: center center;"
        ></i>
      </span>
            <span class="text-xs font-medium leading-none" :class="{ 'text-red-500': hasLikedArticle }">
        {{ localArticleLikes }}
      </span>
          </button>

          <button class="flex items-center gap-1.5 text-gray-400 hover:text-blue-500 transition-colors group/comment" @click="handleCommentClick">
      <span class="inline-flex items-center justify-center w-5 h-5">
        <i
            class="ph ph-chat-circle text-xl group-hover/comment:scale-110 transition-transform"
            style="transform-origin: center center;"
        ></i>
      </span>
            <span class="text-xs font-medium leading-none">{{ allComments.length }}</span>
          </button>
        </div>

        <button class="text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 transition-colors inline-flex items-center justify-center w-5 h-5" title="分享" @click="handleShare">
          <i class="ph ph-share-network text-xl"></i>
        </button>
      </div>

      <!-- 浮动操作按钮 - 优化版 -->
      <div
          v-if="isExpanded && shouldShowFloatingButtons"
          class="fixed right-8 bottom-20 flex flex-col gap-3 z-50 transition-all duration-500"
          :class="{ 'bottom-28': isVisuallyExpanded }"
      >
        <!-- 点赞按钮 -->
        <button
            @click="handleArticleLike"
            class="floating-action-btn group"
            :class="{ 'floating-action-btn--liked': hasLikedArticle }"
            title="点赞"
        >
          <i
              class="ph-fill ph-heart text-xl transition-all duration-300"
              :class="hasLikedArticle ? 'text-red-500 scale-110' : 'text-gray-400 group-hover:text-red-400 group-hover:scale-110'"
          ></i>
        </button>

        <!-- 评论按钮 -->
        <button
            @click="scrollToCommentInput"
            class="floating-action-btn group"
            title="评论"
        >
          <i class="ph ph-chat-circle text-xl text-gray-400 group-hover:text-blue-500 group-hover:scale-110 transition-all duration-300"></i>
        </button>
      </div>

    </div>

    <!-- 首次展开折叠提示 -->
    <Teleport to="body">
      <Transition name="fade-slide">
        <div
            v-if="showCollapseHint && arrowPath"
            class="fixed z-[60] pointer-events-none hidden lg:block"
            :style="{
              width: `${svgSize.width}px`,
              height: `${svgSize.height}px`,
              top: '0',
              left: '0'
            }"
        >
          <svg
              :width="svgSize.width"
              :height="svgSize.height"
              style="overflow: visible;"
          >
            <path
                :d="arrowPath"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-dasharray="6,6"
                class="text-yellow-500 dark:text-yellow-400"
                stroke-linecap="round"
                stroke-linejoin="round"
            />
          </svg>

          <div
              class="absolute pointer-events-auto"
              :style="{ top: `${hintPosition.top}px`, left: `${hintPosition.left}px` }"
          >
            <div class="bg-white dark:bg-[#1c1c1e] border border-gray-200 dark:border-gray-700 rounded-2xl p-5 shadow-xl max-w-[260px]">
              <p class="text-sm text-gray-700 dark:text-gray-300 leading-relaxed mb-4">
                点击这里的 <span class="inline-flex items-center text-yellow-500 font-medium">🟡</span> 按钮可以折叠文章哦～
              </p>

              <button
                  @click="closeCollapseHint"
                  class="w-full py-2.5 px-4 bg-white hover:bg-gray-50 dark:hover:bg-white/5 text-gray-800 dark:text-gray-200 border border-gray-200 dark:border-gray-600 text-sm font-medium rounded-xl transition-all duration-200 hover:shadow-md active:scale-95"
              >
                我知道啦 ✨
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 图片预览 Lightbox -->
    <ImageLightbox :image-url="lightboxImageUrl" :is-open="!!lightboxImageUrl" @close="closeLightbox" />
  </div>
</template>

<style scoped>
/* 优化后的浮动操作按钮 - 纯圆形设计 */
.floating-action-btn {
  @apply w-12 h-12
  rounded-full
  flex items-center justify-center
  bg-white/95 dark:bg-[#2c2c2e]/95
  backdrop-blur-md
  border border-gray-200/60 dark:border-white/10
  shadow-lg shadow-black/5 dark:shadow-black/20
  transition-all duration-300 ease-out
  hover:shadow-xl hover:shadow-black/10 dark:hover:shadow-black/30
  hover:-translate-y-1
  hover:scale-105
  active:translate-y-0
  active:scale-100
  cursor-pointer;
}

.floating-action-btn--liked {
  @apply bg-red-50/95 dark:bg-red-500/10
  border-red-200/60 dark:border-red-500/20
  shadow-red-100/50 dark:shadow-red-500/10;
}

.floating-action-btn:active {
  @apply shadow-md;
}

.mac-summary-shell {
  overflow: hidden;
  transition:
      max-height 0.36s cubic-bezier(0.25, 1, 0.5, 1),
      opacity 0.24s ease,
      margin-top 0.36s cubic-bezier(0.25, 1, 0.5, 1);
}

.mac-summary--open {
  max-height: 430px;
  opacity: 1;
  margin-top: 0.75rem;
  /* 打开时允许垂直滚动 */
  overflow-y: auto;
}

/* 自定义滚动条样式 */
.mac-summary--open::-webkit-scrollbar {
  width: 6px;
}

.mac-summary--open::-webkit-scrollbar-track {
  background: transparent;
}

.mac-summary--open::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  opacity: 0;
  transition: opacity 0.3s ease, background 0.3s ease;
}

.dark .mac-summary--open::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
}

.mac-summary--open:hover::-webkit-scrollbar-thumb {
  opacity: 1;
  background: rgba(0, 0, 0, 0.2);
}

.dark .mac-summary--open:hover::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
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
  /* 不限制最大高度，让内容完整显示 */
  max-height: none;
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

/* 首次展开提示过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease-out;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}
</style>
