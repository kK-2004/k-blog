<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import type { AdminMe } from '@/api/types'

interface ReplyingTo {
  rootId: number
  toUser?: string
}

const props = defineProps<{
  postId: number
  isAuthenticated: boolean
  adminMe: AdminMe | null
  replyingTo?: ReplyingTo | null
}>()

const emit = defineEmits<{
  (e: 'submit', data: { user: string; text: string; replyingTo?: ReplyingTo }): void
  (e: 'cancel-reply'): void
}>()

const draftUser = ref('')
const draftText = ref('')

// 当登录状态改变时，自动填充用户名
watch(
  () => props.adminMe,
  (newMe) => {
    if (newMe) {
      draftUser.value = newMe.username
    }
  },
  { immediate: true }
)

const placeholder = computed(() => {
  if (props.replyingTo?.toUser) {
    return `回复 @${props.replyingTo.toUser}...`
  }
  if (props.replyingTo) {
    return `回复 #${props.replyingTo.rootId}...`
  }
  return '发表评论...'
})

const replyToDisplay = computed(() => {
  if (!props.replyingTo) return null
  if (props.replyingTo.toUser) return `@${props.replyingTo.toUser}`
  return `#${props.replyingTo.rootId}`
})

const canSubmit = computed(() => {
  return draftText.value.trim().length > 0
})

const submit = async () => {
  if (!canSubmit.value) return

  const user = draftUser.value.trim() || 'Guest'
  const text = draftText.value

  // 清空输入框
  draftText.value = ''

  // 触发提交事件
  const replyTo = props.replyingTo ?? undefined
  emit('submit', { user, text, replyingTo: replyTo })
}

const cancelReply = () => {
  emit('cancel-reply')
}

defineExpose({
  focus: () => {
    nextTick(() => {
      const textarea = document.querySelector('.comment-input-textarea') as HTMLTextAreaElement
      textarea?.focus()
    })
  }
})
</script>

<template>
  <div class="comment-input-container">
    <!-- 回复提示 -->
    <div
      v-if="replyingTo"
      class="flex justify-between items-center text-xs text-blue-600 dark:text-blue-400 mb-2 pb-2 border-b border-gray-200 dark:border-white/10"
    >
      <span>
        回复 <span class="font-bold">{{ replyToDisplay }}</span>
      </span>
      <button @click="cancelReply" class="hover:text-red-500 transition-colors">
        <i class="ph ph-x"></i>
      </button>
    </div>

    <!-- 输入区域 -->
    <div class="flex gap-2">
      <!-- 用户名输入（未登录时显示） -->
      <div v-if="!isAuthenticated" class="flex-shrink-0">
        <input
          v-model="draftUser"
          type="text"
          placeholder="昵称"
          class="w-24 px-3 py-2 bg-white dark:bg-white/5 border border-gray-200 dark:border-white/10 rounded-lg text-sm text-gray-800 dark:text-gray-200 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/30 transition-all"
        />
      </div>

      <!-- 评论文本域 -->
      <div class="flex-1 relative">
        <textarea
          v-model="draftText"
          class="comment-input-textarea w-full px-4 py-2 bg-white dark:bg-white/5 border border-gray-200 dark:border-white/10 rounded-lg text-sm text-gray-800 dark:text-gray-200 placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500/30 transition-all"
          :placeholder="placeholder"
          rows="1"
          @keydown.enter.prevent="submit"
        ></textarea>
      </div>

      <!-- 发送按钮 -->
      <button
        @click="submit"
        :disabled="!canSubmit"
        class="flex-shrink-0 px-4 py-2 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 dark:disabled:bg-white/10 disabled:cursor-not-allowed text-white rounded-lg text-sm font-medium transition-all flex items-center gap-1.5"
      >
        <i class="ph-fill ph-paper-plane-right"></i>
        <span>发送</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.comment-input-textarea {
  min-height: 40px;
  max-height: 120px;
}
</style>
