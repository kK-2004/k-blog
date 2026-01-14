<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { createPostComment, createPostCommentReply } from '@/api/comments'

interface ReplyingTo {
  rootId: number
  toUser?: string
}

const props = defineProps<{
  likes: number
  hasLiked: boolean
  hasComments: boolean
  postId: number
  isAuthenticated: boolean
  adminMe: { username: string; avatarUrl: string | null } | null
}>()

const emit = defineEmits<{
  (e: 'like'): void
  (e: 'share'): void
  (e: 'comment-submitted', data: { id: number; user: string; text: string; rootId?: number; toUser?: string }): void
}>()

// 输入框状态
const inputText = ref('')
const replyingTo = ref<ReplyingTo | null>(null)
const isSubmitting = ref(false)

// 监听外部传入的回复状态
const setReplyingTo = (value: ReplyingTo | null) => {
  replyingTo.value = value
  // 聚焦到输入框
  nextTick(() => {
    const textarea = document.querySelector('textarea[data-comment-input]') as HTMLTextAreaElement
    if (textarea) {
      textarea.focus()
    }
  })
}

// 取消回复
const handleCancelReply = () => {
  replyingTo.value = null
}

// 提交评论
const handleSubmit = async () => {
  if (!props.isAuthenticated) {
    alert('请先登录')
    return
  }

  if (!inputText.value.trim()) {
    alert('请输入评论内容')
    return
  }

  if (!props.adminMe) {
    alert('请先登录')
    return
  }

  isSubmitting.value = true
  try {
    if (replyingTo.value) {
      // 提交回复
      const created = await createPostCommentReply(props.postId, replyingTo.value.rootId, {
        user: props.adminMe.username,
        text: inputText.value.trim(),
        toUser: replyingTo.value.toUser,
      })

      emit('comment-submitted', {
        id: created.id,
        user: created.user,
        text: created.text,
        rootId: replyingTo.value.rootId,
        toUser: created.toUser,
      })
    } else {
      // 提交评论
      const created = await createPostComment(props.postId, {
        user: props.adminMe.username,
        text: inputText.value.trim(),
      })

      emit('comment-submitted', {
        id: created.id,
        user: created.user,
        text: created.text,
      })
    }

    // 重置输入框
    inputText.value = ''
    replyingTo.value = null
  } catch (error) {
    console.error('提交评论失败:', error)
    alert('提交失败，请重试')
  } finally {
    isSubmitting.value = false
  }
}

const handleShare = () => {
  // 复制当前页面 URL
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    alert('链接已复制到剪贴板')
  }).catch(() => {
    alert('复制失败，请手动复制链接')
  })

  emit('share')
}

// 暴露方法给父组件
defineExpose({
  setReplyingTo
})
</script>

<template>
  <div class="fixed bottom-0 left-0 right-0 z-[100]">
    <!-- 渐变遮罩 -->
    <div class="absolute bottom-full left-0 right-0 h-24 bg-gradient-to-t from-[#f5f5f7] via-[#f5f5f7]/80 to-transparent dark:from-[#121212] dark:via-[#121212]/80 pointer-events-none"></div>

    <!-- 底部栏 -->
    <div class="bg-[#f5f5f7]/90 dark:bg-[#121212]/90 backdrop-blur-xl border-t border-gray-200/50 dark:border-gray-800/50 shadow-2xl">
      <div class="max-w-4xl mx-auto px-4 py-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))]">

        <!-- 回复提示标签（回复时显示） -->
        <div
          v-if="replyingTo"
          class="flex items-center justify-between mb-2 px-2"
        >
          <div class="flex items-center gap-2 bg-blue-500 text-white text-xs px-3 py-1.5 rounded-full">
            <i class="ph-bold ph-arrow-return-left"></i>
            <span>回复 @{{ replyingTo.toUser }}</span>
          </div>
          <button
            @click="handleCancelReply"
            class="text-xs text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
          >
            取消回复
          </button>
        </div>

        <!-- 主操作区 -->
        <div class="flex items-center gap-3">

          <!-- 输入框 -->
          <div class="flex-1 relative">
            <textarea
              v-model="inputText"
              data-comment-input
              :placeholder="replyingTo ? `回复 @${replyingTo.toUser}...` : '写下你的想法...'"
              class="w-full px-4 py-2.5 pr-24 bg-white dark:bg-[#1e1e1e] rounded-full text-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 border border-gray-200 dark:border-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
              rows="1"
              @keydown.ctrl.enter="handleSubmit"
              @keydown.meta.enter="handleSubmit"
              style="min-height: 42px; max-height: 120px;"
            ></textarea>

            <!-- 发送按钮（绝对定位在输入框右侧） -->
            <button
              @click="handleSubmit"
              :disabled="isSubmitting || !inputText.trim()"
              class="absolute right-2 top-1/2 -translate-y-1/2 px-3 py-1.5 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 dark:disabled:bg-white/10 text-white rounded-full text-xs font-medium transition-colors disabled:cursor-not-allowed flex items-center gap-1"
            >
              <i v-if="isSubmitting" class="ph ph-spinner animate-spin"></i>
              <span v-else>发送</span>
            </button>
          </div>

          <!-- 点赞按钮 -->
          <button
            @click="emit('like')"
            class="flex items-center gap-1.5 px-3 py-2 rounded-lg transition-all duration-200 shrink-0"
            :class="
              hasLiked
                ? 'text-red-500 bg-red-50 dark:bg-red-500/10 hover:bg-red-100 dark:hover:bg-red-500/20'
                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-white/10'
            "
          >
            <i :class="hasLiked ? 'ph-fill ph-heart' : 'ph ph-heart'" class="text-xl"></i>
            <span class="text-sm font-medium">{{ likes }}</span>
          </button>

          <!-- 分享按钮 -->
          <button
            @click="handleShare"
            class="p-2.5 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-white/10 rounded-lg transition-colors shrink-0"
            title="分享文章"
          >
            <i class="ph ph-share-network text-xl"></i>
          </button>

        </div>
      </div>
    </div>
  </div>
</template>
