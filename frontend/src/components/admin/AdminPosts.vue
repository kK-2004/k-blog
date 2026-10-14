<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { retryAiSummary } from '@/api/posts'
import type { Post } from '@/api/types'
import { useHashRouter } from '@/composables/useHashRouter'
import { formatCount, formatDateTime, formatRelativeTime, postTime } from '@/utils/adminFormat'

type SortKey = 'updated' | 'created' | 'views'

const props = defineProps<{
  posts: Post[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'create'): void
  (e: 'edit', post: Post): void
  (e: 'toggle-pin', post: Post): void
  (e: 'delete', post: Post): void
  (e: 'refresh'): void
}>()

const { navigateTo } = useHashRouter()

const keyword = ref('')
const sortKey = ref<SortKey>('updated')

const compare = (a: Post, b: Post): number => {
  if (sortKey.value === 'created') return (a.createdAt ?? 0) - (b.createdAt ?? 0)
  if (sortKey.value === 'views') return b.views - a.views
  return postTime(b) - postTime(a)
}

const visiblePosts = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  const matched = q
    ? props.posts.filter((p) => p.title.toLowerCase().includes(q) || p.content.toLowerCase().includes(q))
    : [...props.posts]
  // 置顶文章始终在前，其余按所选方式排序
  return matched.sort((a, b) => Number(b.pinned) - Number(a.pinned) || compare(a, b))
})

const viewPost = (post: Post) => {
  navigateTo('article', { articleId: post.id })
}

// ==================== AI 摘要状态 ====================

// 本地覆盖：重试成功后先乐观显示 GENERATING，等待列表刷新
const summaryStatusOverride = ref<Record<number, Post['aiSummaryStatus']>>({})
const retryingIds = ref<number[]>([])

const getSummaryStatus = (post: Post): Post['aiSummaryStatus'] =>
  summaryStatusOverride.value[post.id] ?? post.aiSummaryStatus

const SUMMARY_META: Record<string, { label: string; cls: string; icon: string }> = {
  READY: {
    label: '已生成',
    cls: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-400',
    icon: 'ph-check-circle',
  },
  GENERATING: {
    label: '生成中',
    cls: 'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400',
    icon: 'ph-circle-notch animate-spin',
  },
  FAILED: {
    label: '失败',
    cls: 'bg-red-50 text-red-600 dark:bg-red-500/15 dark:text-red-400',
    icon: 'ph-warning-circle',
  },
}

const summaryMeta = (status: Post['aiSummaryStatus']) =>
  SUMMARY_META[status ?? ''] ?? {
    label: '未生成',
    cls: 'bg-gray-100 text-gray-500 dark:bg-white/10 dark:text-gray-400',
    icon: 'ph-minus-circle',
  }

const canRetry = (post: Post) => {
  const status = getSummaryStatus(post)
  return status === 'FAILED' || status === 'GENERATING'
}

const isRetrying = (post: Post) => retryingIds.value.includes(post.id)

const retrySummary = async (post: Post) => {
  if (isRetrying(post)) return
  retryingIds.value.push(post.id)
  try {
    const res = await retryAiSummary(post.id)
    summaryStatusOverride.value[post.id] = res.status
    ElMessage.success(res.status === 'GENERATING' ? '已重新提交生成' : '摘要无需重新生成')
  } catch {
    // 错误提示已由 apiFetch 统一显示
    delete summaryStatusOverride.value[post.id]
  } finally {
    retryingIds.value = retryingIds.value.filter((id) => id !== post.id)
  }
}

// 列表刷新后清除本地覆盖，以服务端状态为准
const clearOverrides = () => {
  summaryStatusOverride.value = {}
}

let pollTimer: ReturnType<typeof setTimeout> | null = null

const pendingPosts = computed(() => props.posts.filter((p) => getSummaryStatus(p) === 'GENERATING'))

const schedulePoll = () => {
  if (pollTimer) return
  pollTimer = setTimeout(() => {
    pollTimer = null
    if (pendingPosts.value.length > 0) emit('refresh')
  }, 6000)
}

const onPostStatusChange = (post: Post | null, status: Post['aiSummaryStatus']) => {
  if (!post || !status || summaryStatusOverride.value[post.id] === status) return
  summaryStatusOverride.value[post.id] = status
  if (status !== 'GENERATING') delete summaryStatusOverride.value[post.id]
}

// 轮询期间有生成中的文章时定期刷新列表，直到状态落定
const schedulePollIfNeeded = () => {
  if (pendingPosts.value.length > 0) schedulePoll()
}

onUnmounted(() => {
  if (pollTimer) clearTimeout(pollTimer)
})

defineExpose({ clearOverrides, onPostStatusChange, schedulePollIfNeeded })
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between gap-3">
      <div class="flex items-baseline gap-3 min-w-0">
        <h1 class="text-xl font-semibold text-gray-900 dark:text-gray-100">文章</h1>
        <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ posts.length }} 篇</span>
      </div>
      <button
        class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-lg bg-[#007AFF] text-white hover:bg-[#0062cc] transition-colors shrink-0"
        @click="emit('create')"
      >
        <i class="ph ph-plus"></i>
        写文章
      </button>
    </div>

    <div class="flex flex-col sm:flex-row gap-2">
      <div class="relative flex-1 min-w-0">
        <i class="ph ph-magnifying-glass absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"></i>
        <input
          v-model="keyword"
          type="search"
          placeholder="搜索标题或内容"
          class="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-gray-200 dark:border-white/10 bg-white/80 dark:bg-[#1e1e1e]/80 text-gray-800 dark:text-gray-200 outline-none focus:ring-2 focus:ring-[#007AFF]"
        />
      </div>
      <select
        v-model="sortKey"
        aria-label="排序方式"
        class="px-3 py-2 text-sm rounded-lg border border-gray-200 dark:border-white/10 bg-white/80 dark:bg-[#1e1e1e]/80 text-gray-800 dark:text-gray-200 outline-none focus:ring-2 focus:ring-[#007AFF]"
      >
        <option value="updated">最近更新</option>
        <option value="created">最早创建</option>
        <option value="views">阅读最多</option>
      </select>
    </div>

    <div class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-sm border border-black/5 dark:border-white/10 overflow-hidden">
      <div v-if="loading" class="py-16 text-center text-sm text-gray-500 dark:text-gray-400">加载中…</div>

      <div v-else-if="posts.length === 0" class="py-16 text-center">
        <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">写下第一篇文章</p>
        <button
          class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-lg bg-[#007AFF] text-white hover:bg-[#0062cc] transition-colors"
          @click="emit('create')"
        >
          <i class="ph ph-plus"></i>
          写文章
        </button>
      </div>

      <div v-else-if="visiblePosts.length === 0" class="py-16 text-center text-sm text-gray-500 dark:text-gray-400">
        没有匹配的文章
      </div>

      <template v-else>
        <div
          class="hidden sm:grid grid-cols-[minmax(0,1fr)_6rem_7rem_11rem_9rem] gap-4 px-5 py-2.5 text-xs font-medium text-gray-400 border-b border-gray-100 dark:border-white/10"
        >
          <span>标题</span>
          <span>AI 摘要</span>
          <span>更新时间</span>
          <span>阅读 · 点赞 · 评论</span>
          <span class="text-right">操作</span>
        </div>

        <div
          v-for="post in visiblePosts"
          :key="post.id"
          class="grid grid-cols-[minmax(0,1fr)_auto] sm:grid-cols-[minmax(0,1fr)_6rem_7rem_11rem_9rem] gap-x-4 gap-y-1 items-center px-5 py-3 border-b last:border-b-0 border-gray-100 dark:border-white/5 cursor-pointer hover:bg-black/[0.03] dark:hover:bg-white/5 transition-colors"
          @click="emit('edit', post)"
        >
          <div class="flex items-center gap-2 min-w-0">
            <span
              v-if="post.pinned"
              class="shrink-0 px-1.5 py-0.5 text-[11px] font-medium rounded bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-300"
            >
              置顶
            </span>
            <span class="truncate text-sm font-medium text-gray-800 dark:text-gray-200">{{ post.title }}</span>
          </div>

          <span
            class="col-start-1 row-start-2 sm:col-start-auto sm:row-start-auto inline-flex w-fit items-center gap-1 px-1.5 py-0.5 text-[11px] font-medium rounded"
            :class="summaryMeta(getSummaryStatus(post)).cls"
            :title="`AI 摘要状态：${summaryMeta(getSummaryStatus(post)).label}`"
          >
            <i :class="['ph', summaryMeta(getSummaryStatus(post)).icon]"></i>
            {{ summaryMeta(getSummaryStatus(post)).label }}
          </span>

          <span
            class="hidden sm:block text-xs text-gray-500 dark:text-gray-400"
            :title="formatDateTime(postTime(post))"
          >
            {{ formatRelativeTime(postTime(post)) }}
          </span>

          <span class="hidden sm:block text-xs text-gray-500 dark:text-gray-400 tabular-nums">
            {{ formatCount(post.views) }} · {{ formatCount(post.likes) }} · {{ formatCount(post.comments) }}
          </span>

          <div class="col-start-2 row-start-1 row-span-2 sm:col-start-auto sm:row-start-auto sm:row-span-1 flex items-center justify-end gap-0.5">
            <button
              v-if="canRetry(post)"
              class="p-2 rounded-lg transition-colors hover:bg-black/5 dark:hover:bg-white/10"
              :class="isRetrying(post) ? 'text-purple-400' : 'text-gray-400 hover:text-purple-500'"
              :disabled="isRetrying(post)"
              :title="getSummaryStatus(post) === 'FAILED' ? '重新生成 AI 摘要' : '重新生成 AI 摘要（生成中）'"
              aria-label="重试 AI 摘要"
              @click.stop="retrySummary(post)"
            >
              <i :class="['ph', isRetrying(post) ? 'ph-circle-notch animate-spin' : 'ph-arrows-clockwise']"></i>
            </button>
            <button
              class="p-2 rounded-lg transition-colors hover:bg-black/5 dark:hover:bg-white/10"
              :class="post.pinned ? 'text-amber-500' : 'text-gray-400'"
              :title="post.pinned ? '取消置顶' : '置顶'"
              :aria-label="post.pinned ? '取消置顶' : '置顶'"
              @click.stop="emit('toggle-pin', post)"
            >
              <i class="ph ph-push-pin"></i>
            </button>
            <button
              class="p-2 rounded-lg text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
              title="查看"
              aria-label="查看"
              @click.stop="viewPost(post)"
            >
              <i class="ph ph-arrow-square-out"></i>
            </button>
            <button
              class="p-2 rounded-lg text-gray-400 hover:text-[#007AFF] hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
              title="编辑"
              aria-label="编辑"
              @click.stop="emit('edit', post)"
            >
              <i class="ph ph-pencil-simple"></i>
            </button>
            <button
              class="p-2 rounded-lg text-gray-400 hover:text-red-500 hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
              title="删除"
              aria-label="删除"
              @click.stop="emit('delete', post)"
            >
              <i class="ph ph-trash"></i>
            </button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
