<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { createPostComment, createPostCommentReply } from '@/api/comments'
import { useMessage } from '@/composables/useMessage'

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
  isSidebarOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'like'): void
  (e: 'share'): void
  (e: 'comment-submitted', data: { id: number; user: string; text: string; rootId?: number; toUser?: string }): void
}>()

const { success } = useMessage()

// 输入框状态
const nicknameInputRef = ref<HTMLElement | null>(null)
const draftUser = ref('')
const inputText = ref('')
const replyingTo = ref<ReplyingTo | null>(null)
const isSubmitting = ref(false)

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
  if (!inputText.value.trim()) {
    alert('请输入评论内容')
    return
  }

  const user = draftUser.value.trim() || 'Guest'
  const text = inputText.value.trim()
  inputText.value = ''

  isSubmitting.value = true
  try {
    if (replyingTo.value) {
      // 提交回复
      const created = await createPostCommentReply(props.postId, replyingTo.value.rootId, {
        user,
        text,
        toUser: replyingTo.value.toUser,
      })

      emit('comment-submitted', {
        id: created.id,
        user: created.user,
        text: created.text,
        rootId: replyingTo.value.rootId,
        toUser: created.toUser ?? undefined,
      })
    } else {
      // 提交评论
      const created = await createPostComment(props.postId, { user, text })

      emit('comment-submitted', {
        id: created.id,
        user: created.user,
        text: created.text,
      })
    }

    // 重置输入框
    replyingTo.value = null
  } catch (error) {
    console.error('提交评论失败:', error)
    alert('提交失败，请重试')
  } finally {
    isSubmitting.value = false
  }
}

const handleShare = async () => {
  // 复制当前页面 URL
  const url = window.location.href
  try {
    await navigator.clipboard.writeText(url)
    success('链接已复制到剪贴板')
  } catch {
    success('复制失败，请手动复制链接')
  }

  emit('share')
}

// 暴露方法给父组件
defineExpose({
  setReplyingTo,
  nicknameInputRef
})
</script>

<template>
  <div class="fixed bottom-0 right-0 transition-all duration-300 z-[100]" :class="isSidebarOpen ? 'left-64' : 'left-0'">
<!--    &lt;!&ndash; 渐变遮罩 &ndash;&gt;-->
<!--    <div class="absolute bottom-full left-0 right-0 h-14 bg-gradient-to-t from-[#f5f5f780] via-[#f5f5f780]/80 to-transparent dark:from-[#12121280] dark:via-[#12121280]/80 pointer-events-none"></div>-->

    <!-- 底部栏 -->
    <div class="bg-[#f5f5f7]/90 dark:bg-[#121212]/90 backdrop-blur-xl border-t border-gray-200/50 dark:border-gray-800/50 shadow-2xl">
      <div class="max-w-4xl mx-auto px-4 py-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))]">

        <!-- 主操作区 -->
        <div class="flex items-end gap-3">

          <!-- 输入框 -->
          <div class="flex-1">
            <div class="bg-gray-100 dark:bg-[#2c2c2e] p-1 rounded-[20px] border border-transparent focus-within:border-blue-500/30 transition-all flex flex-col shadow-inner">
              <!-- 回复提示标签（回复时显示） -->
              <div
                v-if="replyingTo"
                class="px-3 py-1 text-[11px] text-blue-500 flex justify-between items-center bg-blue-50 dark:bg-blue-900/20 rounded-t-[16px] mb-1"
              >
                <span>
                  回复<span v-if="replyingTo.toUser" class="font-bold"> @{{ replyingTo.toUser }}</span
                  ><span v-else class="font-bold"> #{{ replyingTo.rootId }}</span
                  >...
                </span>
                <button class="hover:text-red-500" @click="handleCancelReply">
                  <i class="ph ph-x"></i>
                </button>
              </div>
              <input
                ref="nicknameInputRef"
                v-model="draftUser"
                placeholder="Nickname..."
                class="bg-transparent text-[10px] text-gray-500 dark:text-gray-400 px-3 py-1 outline-none w-full border-b border-gray-200 dark:border-white/5 mb-1 placeholder-gray-400/70"
              />
              <textarea
                v-model="inputText"
                data-comment-input
                :placeholder="replyingTo ? (replyingTo.toUser ? `回复 @${replyingTo.toUser}...` : `回复 #${replyingTo.rootId}...`) : '发表评论...'"
                rows="1"
                class="bg-transparent text-sm px-3 py-1 outline-none w-full resize-none text-gray-800 dark:text-gray-100 placeholder-gray-400"
                style="min-height: 24px"
                @keydown.ctrl.enter="handleSubmit"
                @keydown.meta.enter="handleSubmit"
              ></textarea>
            </div>
          </div>

          <!-- 发送按钮 -->
          <button
            class="w-8 h-8 rounded-full bg-[#007AFF] hover:bg-[#0062cc] disabled:bg-gray-300 dark:disabled:bg-gray-600 flex items-center justify-center text-white transition-all shadow-md active:scale-95 mb-0.5 shrink-0"
            :disabled="isSubmitting || !inputText.trim()"
            @click="handleSubmit"
          >
            <i v-if="isSubmitting" class="ph ph-spinner animate-spin"></i>
            <i v-else class="ph ph-paper-plane-right font-bold"></i>
          </button>

          <!-- 点赞按钮 -->
          <button
            @click="emit('like')"
            class="flex items-center gap-1.5 px-2.5 py-2 rounded-lg transition-all duration-200 shrink-0 h-10"
            :class="
              hasLiked
                ? 'text-red-500 bg-red-50 dark:bg-red-500/10 hover:bg-red-100 dark:hover:bg-red-500/20'
                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-white/10'
            "
          >
            <span class="inline-flex items-center justify-center w-5 h-5 shrink-0">
              <i :class="hasLiked ? 'ph-fill ph-heart' : 'ph ph-heart'" class="text-xl absolute"></i>
            </span>
            <span class="text-sm font-medium leading-none">{{ likes }}</span>
          </button>

          <!-- 分享按钮 -->
          <button
            @click="handleShare"
            class="p-2.5 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-white/10 rounded-lg transition-colors shrink-0 h-10"
            title="分享文章"
          >
            <i class="ph ph-share-network text-xl"></i>
          </button>

        </div>
      </div>
    </div>
  </div>
</template>
