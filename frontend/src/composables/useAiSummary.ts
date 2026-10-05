import { computed, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiSummary, getAiSummaryStatuses } from '@/api/posts'
import type { AiSummaryStatus } from '@/api/types'

// ==================== 共享轮询：所有生成中的文章合并成一个请求 ====================
// 后端有每分钟 100 次的限流黑名单，因此不按卡片各自轮询

const POLL_INTERVAL_MS = 8000
const listeners = new Map<number, Set<(status: AiSummaryStatus) => void>>()
let pollTimer: ReturnType<typeof setTimeout> | null = null

function schedulePoll() {
  if (pollTimer || listeners.size === 0) return
  pollTimer = setTimeout(async () => {
    pollTimer = null
    const ids = [...listeners.keys()].slice(0, 50)
    if (ids.length === 0) return
    try {
      const results = await getAiSummaryStatuses(ids)
      for (const r of results) {
        listeners.get(r.postId)?.forEach((cb) => cb(r.status))
      }
    } catch {
      // 静默失败，下一轮重试
    }
    schedulePoll()
  }, POLL_INTERVAL_MS)
}

function subscribe(postId: number, cb: (status: AiSummaryStatus) => void) {
  let set = listeners.get(postId)
  if (!set) {
    set = new Set()
    listeners.set(postId, set)
  }
  set.add(cb)
  schedulePoll()
  return () => {
    set!.delete(cb)
    if (set!.size === 0) listeners.delete(postId)
  }
}

// ==================== 单篇文章摘要 ====================

const TYPE_TICK_MS = 30
const TYPE_TOTAL_TICKS = 100 // 约 3 秒打完，长摘要每次多输出几个字

/**
 * 文章 AI 摘要：摘要由后端在发布后异步生成落库，前端读取后以打字机效果展示
 * - isPending：后端仍在生成，按钮不可点，显示“生成中”
 * - isGenerating：正在请求或打字中
 * - isStreaming：打字中（显示纯文本 + 光标，结束后渲染 Markdown）
 */
export function useAiSummary(postId: () => number, initialStatus: () => AiSummaryStatus | undefined) {
  const status = ref<AiSummaryStatus>(initialStatus() ?? 'GENERATING')
  const summary = ref('')
  const displayedSummary = ref('')
  const isGenerating = ref(false)
  const isStreaming = ref(false)
  const hasReceivedData = computed(() => displayedSummary.value.length > 0)
  const isPending = computed(() => status.value === 'GENERATING')

  let unsubscribe: (() => void) | null = null
  let typeTimer: ReturnType<typeof setInterval> | null = null
  let requestId = 0

  const stopWatching = () => {
    unsubscribe?.()
    unsubscribe = null
  }

  const syncWatching = () => {
    if (status.value === 'GENERATING') {
      if (!unsubscribe) {
        unsubscribe = subscribe(postId(), (s) => {
          status.value = s
          if (s !== 'GENERATING') stopWatching()
        })
      }
    } else {
      stopWatching()
    }
  }

  const stopTyping = () => {
    if (typeTimer) clearInterval(typeTimer)
    typeTimer = null
  }

  const reset = () => {
    requestId += 1
    stopTyping()
    stopWatching()
    summary.value = ''
    displayedSummary.value = ''
    isGenerating.value = false
    isStreaming.value = false
    status.value = initialStatus() ?? 'GENERATING'
    syncWatching()
  }

  const typewrite = (text: string, id: number) =>
    new Promise<void>((resolve) => {
      const step = Math.max(1, Math.ceil(text.length / TYPE_TOTAL_TICKS))
      let index = 0
      isStreaming.value = true
      typeTimer = setInterval(() => {
        if (id !== requestId) {
          stopTyping()
          resolve()
          return
        }
        index = Math.min(text.length, index + step)
        displayedSummary.value = text.slice(0, index)
        if (index >= text.length) {
          stopTyping()
          resolve()
        }
      }, TYPE_TICK_MS)
    })

  /** 读取已生成的摘要并以打字机效果展示 */
  const reveal = async () => {
    if (isGenerating.value || summary.value || isPending.value) return

    const id = (requestId += 1)
    isGenerating.value = true
    displayedSummary.value = ''
    try {
      const res = await getAiSummary(postId())
      if (id !== requestId) return
      status.value = res.status
      syncWatching()
      if (res.status !== 'READY' || !res.summary) {
        if (res.status === 'FAILED') ElMessage.warning('AI 摘要暂时生成失败，请稍后再试')
        return
      }
      await typewrite(res.summary, id)
      if (id !== requestId) return
      summary.value = res.summary
    } catch {
      // apiFetch 已统一提示错误
    } finally {
      if (id === requestId) {
        isGenerating.value = false
        isStreaming.value = false
      }
    }
  }

  watch(postId, reset)
  // 文章列表刷新带来新状态时同步（尚未展示摘要时才更新）
  watch(initialStatus, (s) => {
    if (s && !summary.value && !isGenerating.value) {
      status.value = s
      syncWatching()
    }
  })

  syncWatching()
  onUnmounted(() => {
    requestId += 1
    stopTyping()
    stopWatching()
  })

  return {
    status,
    summary,
    displayedSummary,
    isGenerating,
    isStreaming,
    hasReceivedData,
    isPending,
    reveal,
    /** 清空已展示的摘要（如卡片收起时） */
    clear: () => {
      requestId += 1
      stopTyping()
      summary.value = ''
      displayedSummary.value = ''
      isGenerating.value = false
      isStreaming.value = false
    },
  }
}
