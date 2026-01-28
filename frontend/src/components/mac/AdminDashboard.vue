<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import type { Post, QuickAction } from '@/api/types'
import AdminEditor from './AdminEditor.vue'
import ProfileCenter from '@/views/ProfileCenter.vue'
import { createPost, deletePost as apiDeletePost, listPosts, updatePost } from '@/api/posts'
import { useHashRouter } from '@/composables/useHashRouter'
import { listVisibleQuickActions } from '@/api/site'

const router = useHashRouter()

const props = defineProps<{
  posts: Post[]
}>()

const emit = defineEmits<{
  (e: 'update:posts', posts: Post[]): void
  (e: 'refresh'): void
}>()

// 当前视图：posts 或 profile
const currentView = ref<'posts' | 'profile'>('posts')

const isEditorOpen = ref(false)
const currentEditId = ref<number | null>(null)
const editorContent = ref('')
const editorTitle = ref('')
const isCreating = ref(false)
const editorRef = ref<InstanceType<typeof AdminEditor> | null>(null)
const hashWhenOpened = ref('')

const quickActions = ref<QuickAction[]>([])

// 本地响应式 posts，确保列表能正确刷新
const localPosts = ref<Post[]>([...props.posts])

// 监听 props.posts 变化，同步到本地
watch(() => props.posts, (newPosts) => {
  localPosts.value = [...newPosts]
}, { deep: true })

const loadQuickActions = async () => {
  try {
    quickActions.value = await listVisibleQuickActions()
  } catch {
    quickActions.value = []
  }
}

const openQuickAction = (item: QuickAction) => {
  if (item.targetType === 'internal') {
    const hash = item.target.startsWith('#') ? item.target : `#${item.target}`
    window.location.hash = hash
    return
  }
  window.open(item.target, '_blank', 'noopener,noreferrer')
}

const openEditor = (post: Post) => {
  currentEditId.value = post.id
  editorContent.value = post.content
  editorTitle.value = post.title
  isEditorOpen.value = true
}

const openCreate = () => {
  isCreating.value = true
  currentEditId.value = null
  editorContent.value = ''
  editorTitle.value = ''
  isEditorOpen.value = true
}

const closeEditor = () => {
  isEditorOpen.value = false
  currentEditId.value = null
  isCreating.value = false
  editorTitle.value = ''
}

const refreshPosts = async () => {
  const posts = await listPosts({ cache: false })
  localPosts.value = posts  // 更新本地数据
}

const savePost = async (newContent: string, newTitle: string) => {
  if (!newTitle?.trim()) {
    alert('请输入文章标题')
    return
  }
  if (!newContent?.trim()) {
    alert('内容不能为空')
    return
  }

  if (isCreating.value) {
    await createPost({ title: newTitle.trim(), content: newContent, pinned: false })
    await refreshPosts()
    closeEditor()
    return
  }

  const post = localPosts.value.find((p) => p.id === currentEditId.value)
  if (!post) return
  await updatePost(post.id, { ...post, title: newTitle.trim(), content: newContent })
  await refreshPosts()
  closeEditor()
}

const togglePin = async (postId: number) => {
  const post = localPosts.value.find((p) => p.id === postId)
  if (post) {
    await updatePost(post.id, { ...post, pinned: !post.pinned })
    await refreshPosts()
  }
}

const deletePost = async (postId: number) => {
  if (!window.confirm('Delete this post?')) return
  await apiDeletePost(postId)
  await refreshPosts()
}

const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

const navigateToArticle = (postId: number) => {
  router.navigateTo('article', { articleId: postId })
}

// 监听编辑器打开状态，记录当前哈希
watch(isEditorOpen, (isOpen) => {
  if (isOpen) {
    hashWhenOpened.value = window.location.hash
  }
})

// 哈希变化拦截
const handleHashChange = () => {
  if (!isEditorOpen.value) return
  if (!editorRef.value?.hasUnsavedChanges) return

  // 恢复原哈希并弹出确认
  const newHash = window.location.hash
  window.location.hash = hashWhenOpened.value

  setTimeout(() => {
    if (confirm('你有未保存的更改，确定要离开吗？')) {
      // 用户确认：恢复新哈希（触发路由跳转）并关闭编辑器
      window.location.hash = newHash
      closeEditor()
    }
    // 用户取消：哈希已经恢复，不做任何事
  }, 0)
}

onMounted(() => {
  void loadQuickActions()
  window.addEventListener('hashchange', handleHashChange)
})

onUnmounted(() => {
  window.removeEventListener('hashchange', handleHashChange)
})
</script>

<template>
  <div class="divide-y-6">
    <!-- 顶部导航 -->
    <div class="mb-4 bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden">
      <div class="flex">
        <button
          class="flex-1 py-4 px-6 text-sm font-medium transition-colors flex items-center justify-center gap-2"
          :class="currentView === 'posts'
            ? 'bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 border-b-2 border-blue-500'
            : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800'"
          @click="currentView = 'posts'"
        >
          <i class="ph ph-squares-four text-lg"></i>
          文章管理
        </button>
        <button
          class="flex-1 py-4 px-6 text-sm font-medium transition-colors flex items-center justify-center gap-2"
          :class="currentView === 'profile'
            ? 'bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 border-b-2 border-blue-500'
            : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800'"
          @click="currentView = 'profile'"
        >
          <i class="ph ph-user-gear text-lg"></i>
          个人中心
        </button>
      </div>
    </div>

    <!-- 文章管理视图 -->
    <div v-if="currentView === 'posts'" class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden min-h-[500px]">
      <div v-if="quickActions.length" class="p-6 border-b border-gray-200 dark:border-gray-700 bg-white/40 dark:bg-black/10">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
            <i class="ph ph-lightning text-lg"></i>
            快速操作
          </h2>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          <button
            v-for="item in quickActions"
            :key="item.id"
            class="flex items-center gap-3 px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
            @click="openQuickAction(item)"
          >
            <div class="w-10 h-10 rounded-lg bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center">
              <i :class="['ph', item.icon, 'text-blue-500 dark:text-blue-400 text-xl']"></i>
            </div>
            <div class="min-w-0">
              <div class="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{{ item.title }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400 truncate">{{ item.description || '' }}</div>
            </div>
          </button>
        </div>
      </div>
      <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center px-6 justify-between">
        <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
          <i class="ph ph-squares-four text-lg"></i>
          文章列表
        </h2>
        <button class="px-2.5 py-1 text-[10px] rounded bg-[#007AFF] text-white hover:opacity-90 shadow" @click="openCreate">
          New Post
        </button>
      </div>

      <div class="p-0 overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
          <tr class="border-b border-gray-100 dark:border-white/10 text-xs text-gray-400 font-medium uppercase tracking-wider">
            <th class="p-4 pl-6">Title</th>
            <th class="p-4 text-center">Stats</th>
            <th class="p-4 text-center pr-6">Action</th>
          </tr>
          </thead>
          <tbody class="divide-y divide-gray-50 dark:divide-white/5">
          <tr
              v-for="post in localPosts"
              :key="post.id"
              class="transition-colors group cursor-pointer"
              :class="{
                'hover:bg-blue-50/50 dark:hover:bg-blue-900/10': !post.pinned,
                'bg-amber-50/30 dark:bg-amber-900/10 hover:bg-amber-100/50 dark:hover:bg-amber-900/20': post.pinned
              }"
              @click="navigateToArticle(post.id)"
          >
            <td class="p-4 pl-6">
              <div class="flex items-center gap-2">
                <div class="font-medium text-gray-800 dark:text-gray-200 text-sm group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">{{ post.title }}</div>
              </div>
              <div class="text-xs text-gray-400 truncate w-48">{{ post.content.substring(0, 30) }}...</div>
            </td>
            <td class="p-4 text-right">
              <div class="flex items-center justify-center gap-4">
                <div class="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-blue-50/50 dark:bg-blue-500/10 border border-blue-100 dark:border-blue-500/20 transition-all hover:bg-blue-100/70 dark:hover:bg-blue-500/20">
                  <i class="ph ph-eye text-blue-500 dark:text-blue-400 text-sm"></i>
                  <span class="text-xs font-semibold text-blue-600 dark:text-blue-400 min-w-[2.5rem] text-right">{{ formatNumber(post.views) }}</span>
                </div>
                <div class="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-rose-50/50 dark:bg-rose-500/10 border border-rose-100 dark:border-rose-500/20 transition-all hover:bg-rose-100/70 dark:hover:bg-rose-500/20">
                  <i class="ph ph-heart text-rose-500 dark:text-rose-400 text-sm"></i>
                  <span class="text-xs font-semibold text-rose-600 dark:text-rose-400 min-w-[2.5rem] text-right">{{ formatNumber(post.likes) }}</span>
                </div>
                <div class="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-emerald-50/50 dark:bg-emerald-500/10 border border-emerald-100 dark:border-emerald-500/20 transition-all hover:bg-emerald-100/70 dark:hover:bg-emerald-500/20">
                  <i class="ph ph-chat-circle text-emerald-500 dark:text-emerald-400 text-sm"></i>
                  <span class="text-xs font-semibold text-emerald-600 dark:text-emerald-400 min-w-[2.5rem] text-right">{{ formatNumber(post.comments) }}</span>
                </div>
              </div>
            </td>
            <td class="p-4 pr-6 text-center">
              <div class="flex items-center justify-center gap-1">
                <button
                    class="p-2 transition-colors rounded"
                    :class="post.pinned ? 'text-amber-500 hover:bg-amber-100 dark:hover:bg-amber-900/30' : 'text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'"
                    title="置顶"
                    @click.stop="togglePin(post.id)"
                >
                  <i class="ph ph-push-pin"></i>
                </button>
                <button class="p-2 text-blue-500 hover:bg-blue-100 rounded dark:hover:bg-blue-900/30 transition-colors" title="编辑" @click.stop="openEditor(post)">
                  <i class="ph ph-pencil-simple"></i>
                </button>
                <button class="p-2 text-red-500 hover:bg-red-100 rounded dark:hover:bg-red-900/30 transition-colors" title="删除" @click.stop="deletePost(post.id)">
                  <i class="ph ph-trash"></i>
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <Teleport to="body">
        <AdminEditor ref="editorRef" :isOpen="isEditorOpen" :initialContent="editorContent" :initialTitle="editorTitle" @close="closeEditor" @save="savePost" />
      </Teleport>
    </div>

    <!-- 个人中心视图 -->
    <ProfileCenter v-else />
  </div>
</template>
