<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listPostComments, likePostComment, deletePostComment } from '@/api/comments'
import AvatarCircle from '@/components/mac/AvatarCircle.vue'
import type { AdminMe } from '@/api/types'

// 类型定义
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
  postId: number
  isAuthenticated: boolean
  adminMe: AdminMe | null
  authorAvatarUrl: string | null
}>()

const emit = defineEmits<{
  (e: 'reply-to', value: { rootId: number; toUser?: string }): void
  (e: 'comment-count-change', count: number): void
  (e: 'comment-submitted', comment: { id: number; user: string; text: string; rootId?: number; toUser?: string }): void
}>()

// 评论数据
const allComments = ref<CommentItem[]>([])
const comments = ref<CommentItem[]>([])
const commentsLoading = ref(false)
const commentsHasMore = ref(false)
const COMMENTS_PAGE_SIZE = 5
const isAdmin = computed(() => props.isAuthenticated && !!props.adminMe)

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
  if (props.isAuthenticated && props.adminMe && username === props.adminMe.username) {
    return props.adminMe.avatarUrl
  }
  if (username === props.authorAvatarUrl) {
    return props.authorAvatarUrl
  }
  return null
}

// 获取评论总数
const totalCommentsCount = computed(() => {
  let count = allComments.value.length
  allComments.value.forEach((c) => {
    count += c.replies.length
  })
  return count
})

// 加载评论
const loadComments = async () => {
  try {
    const data = await listPostComments(props.postId)
    allComments.value = data.map((c) => ({
      ...c,
      hasLiked: false,
      isHot: c.hot,
      replies: (c.replies ?? []).map((r) => ({
        ...r,
        hasLiked: false,
        toUser: r.toUser ?? undefined
      })),
    }))

    // 排序：热门评论优先，然后按时间倒序
    allComments.value.sort((a, b) => {
      if (a.isHot && !b.isHot) return -1
      if (!a.isHot && b.isHot) return 1
      return b.createdAt - a.createdAt
    })

    // 加载第一页
    await loadMoreComments()
  } catch (error) {
    console.error('加载评论失败:', error)
  }
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
    const items = allComments.value.slice(offset, offset + COMMENTS_PAGE_SIZE)
    comments.value.push(...items)
    commentsHasMore.value = comments.value.length < allComments.value.length
  } finally {
    commentsLoading.value = false
  }
}

// 点赞评论
const handleCommentLike = (item: { id: number; likes: number; hasLiked?: boolean }) => {
  if (item.hasLiked) return
  item.likes += 1
  item.hasLiked = true
  likePostComment(props.postId, item.id).catch(() => {
    item.likes = Math.max(0, item.likes - 1)
    item.hasLiked = false
  })
}

// 回复评论 - 发射事件给父组件处理
const handleReply = (comment: CommentItem, toUser?: string) => {
  emit('reply-to', { rootId: comment.id, toUser })
}

const removeRootFromLists = (rootId: number) => {
  const idxAll = allComments.value.findIndex((c) => c.id === rootId)
  if (idxAll !== -1) {
    allComments.value.splice(idxAll, 1)
  }
  const idxView = comments.value.findIndex((c) => c.id === rootId)
  if (idxView !== -1) {
    comments.value.splice(idxView, 1)
    // 补齐当前页
    if (comments.value.length < Math.min(COMMENTS_PAGE_SIZE, allComments.value.length)) {
      const next = allComments.value.slice(comments.value.length, comments.value.length + 1)
      comments.value.push(...next)
    }
    commentsHasMore.value = comments.value.length < allComments.value.length
  }
}

const removeReplyFromLists = (rootId: number, replyId: number) => {
  const root = allComments.value.find((c) => c.id === rootId)
  if (!root) return
  const idx = root.replies.findIndex((r) => r.id === replyId)
  if (idx !== -1) root.replies.splice(idx, 1)
}

const handleDelete = async (rootId: number, commentId: number, isReply: boolean) => {
  if (!isAdmin.value) return
  if (!window.confirm('确定删除这条评论吗？')) return
  try {
    await deletePostComment(props.postId, commentId)
    if (isReply) {
      removeReplyFromLists(rootId, commentId)
    } else {
      removeRootFromLists(commentId)
    }
    emit('comment-count-change', totalCommentsCount.value)
  } catch (e) {
    console.error('删除评论失败:', e)
    alert('删除失败，请重试')
  }
}

// 添加新评论到列表（由父组件调用）
const addComment = (comment: {
  id: number
  user: string
  text: string
  rootId?: number
  toUser?: string
  likes?: number
  createdAt?: number
  hot?: boolean
}) => {
  if (comment.rootId) {
    // 添加回复
    const parent = allComments.value.find((c) => c.id === comment.rootId)
    if (parent) {
      const replyToAdd: ReplyItem = {
        id: comment.id,
        user: comment.user,
        text: comment.text,
        likes: comment.likes ?? 0,
        toUser: comment.toUser,
        hasLiked: false,
        createdAt: comment.createdAt ?? Date.now(),
      }
      parent.replies.unshift(replyToAdd)
    }
  } else {
    // 添加顶级评论
    const commentToAdd: CommentItem = {
      id: comment.id,
      user: comment.user,
      text: comment.text,
      likes: comment.likes ?? 0,
      hasLiked: false,
      isHot: comment.hot ?? false,
      replies: [],
      createdAt: comment.createdAt ?? Date.now(),
    }

    allComments.value.unshift(commentToAdd)
    comments.value.unshift(commentToAdd)
    if (comments.value.length > COMMENTS_PAGE_SIZE) {
      comments.value.pop()
    }
    commentsHasMore.value = comments.value.length < allComments.value.length
  }

  emit('comment-count-change', totalCommentsCount.value)
}

// 暴露方法给父组件
defineExpose({
  addComment
})

onMounted(() => {
  loadComments()
})
</script>

<template>
  <div class="comment-section">
    <!-- 评论标题 -->
    <h3 class="text-xl font-bold mb-6 flex items-center gap-2 text-gray-900 dark:text-gray-100">
      评论
      <span class="text-sm font-normal text-gray-400">({{ totalCommentsCount }})</span>
    </h3>

    <!-- 评论列表 -->
    <div v-if="comments.length > 0" class="space-y-6">
      <div
        v-for="comment in comments"
        :key="comment.id"
        :data-comment-id="comment.id"
        class="flex gap-4 group/comment"
      >
        <!-- 头像 -->
        <div class="flex-shrink-0">
          <img
            v-if="getCommenterAvatarUrl(comment.user)"
            :src="getCommenterAvatarUrl(comment.user)!"
            :alt="comment.user"
            class="w-10 h-10 rounded-full object-cover"
          />
          <AvatarCircle v-else :name="comment.user" size="md" />
        </div>

        <!-- 评论内容 -->
        <div class="flex-1 min-w-0">
          <!-- 评论头部 -->
          <div class="flex items-center justify-between mb-1">
            <span class="font-semibold text-sm text-gray-800 dark:text-gray-200">
              {{ comment.user }}
            </span>
            <span class="text-xs text-gray-400">{{ formatCommentTime(comment.createdAt) }}</span>
          </div>

          <!-- 热门标签 -->
          <div v-if="comment.isHot" class="mb-1">
            <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-orange-100 text-orange-800 dark:bg-orange-500/20 dark:text-orange-400">
              <i class="ph-fill ph-fire mr-1"></i>
              热门
            </span>
          </div>

          <!-- 评论文本 -->
          <p class="text-gray-600 dark:text-gray-300 text-sm leading-relaxed mb-2">
            {{ comment.text }}
          </p>

          <!-- 操作按钮 -->
          <div class="flex items-center gap-4 opacity-0 group-hover/comment:opacity-100 transition-opacity">
            <button
              @click="handleCommentLike(comment)"
              class="flex items-center gap-1 text-xs transition-colors"
              :class="comment.hasLiked ? 'text-red-500' : 'text-gray-400 hover:text-red-500'"
            >
              <i :class="comment.hasLiked ? 'ph-fill ph-heart' : 'ph ph-heart'"></i>
              {{ comment.likes }}
            </button>
            <button
              @click="handleReply(comment)"
              class="text-xs text-gray-400 hover:text-blue-500 transition-colors font-medium"
            >
              回复
            </button>
            <button
              v-if="isAdmin"
              @click="handleDelete(comment.id, comment.id, false)"
              class="text-xs text-gray-400 hover:text-red-500 transition-colors font-medium"
            >
              删除
            </button>
          </div>

          <!-- 回复列表 -->
          <div v-if="comment.replies.length > 0" class="mt-4 pl-4 border-l-2 border-gray-200 dark:border-white/10 space-y-3">
            <div v-for="reply in comment.replies" :key="reply.id" class="flex gap-3">
              <!-- 回复头像 -->
              <div class="flex-shrink-0">
                <img
                  v-if="getCommenterAvatarUrl(reply.user)"
                  :src="getCommenterAvatarUrl(reply.user)!"
                  :alt="reply.user"
                  class="w-8 h-8 rounded-full object-cover"
                />
                <AvatarCircle v-else :name="reply.user" size="sm" />
              </div>

              <!-- 回复内容 -->
              <div class="flex-1 min-w-0">
                <!-- 回复头部 -->
                <div class="text-xs mb-0.5">
                  <span class="font-semibold text-gray-700 dark:text-gray-300">
                    {{ reply.user }}
                  </span>
                  <span v-if="reply.toUser" class="text-gray-400 mx-1">回复</span>
                  <span v-if="reply.toUser" class="font-medium text-blue-500">@{{ reply.toUser }}</span>
                </div>

                <!-- 回复文本 -->
                <p class="text-gray-600 dark:text-gray-300 text-sm">
                  {{ reply.text }}
                </p>

                <!-- 回复操作 -->
                <div class="flex items-center gap-3 mt-1">
                  <button
                    @click="handleCommentLike(reply)"
                    class="flex items-center gap-1 text-[10px] transition-colors"
                    :class="reply.hasLiked ? 'text-red-500' : 'text-gray-400 hover:text-red-500'"
                  >
                    <i :class="reply.hasLiked ? 'ph-fill ph-heart' : 'ph ph-heart'"></i>
                    {{ reply.likes }}
                  </button>
                  <button
                    @click="handleReply(comment, reply.user)"
                    class="text-[10px] text-gray-400 hover:text-blue-500 transition-colors"
                  >
                    回复
                  </button>
                  <button
                    v-if="isAdmin"
                    @click="handleDelete(comment.id, reply.id, true)"
                    class="text-[10px] text-gray-400 hover:text-red-500 transition-colors"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="commentsHasMore" class="text-center pt-4">
        <button
          @click="loadMoreComments"
          :disabled="commentsLoading"
          class="px-6 py-2 bg-gray-100 dark:bg-white/10 hover:bg-gray-200 dark:hover:bg-white/15 text-gray-600 dark:text-gray-400 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ commentsLoading ? '加载中...' : '加载更多评论' }}
        </button>
      </div>
    </div>

    <!-- 空状态 -->
    <div
      v-else
      class="text-center py-12 bg-gray-50 dark:bg-white/5 rounded-xl border border-dashed border-gray-200 dark:border-white/10"
    >
      <div class="text-gray-400 text-sm">暂无评论，来抢沙发吧 ~</div>
    </div>
  </div>
</template>
