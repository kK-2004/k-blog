<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AdminMe, Post } from '@/api/types'
import { createPost, deletePost as apiDeletePost, listPosts, updatePost } from '@/api/posts'
import { useHashRouter, type AdminPage } from '@/composables/useHashRouter'
import AdminOverview from './AdminOverview.vue'
import AdminPosts from './AdminPosts.vue'
import AdminEditor from '@/components/mac/AdminEditor.vue'
import ProfileCenter from '@/views/ProfileCenter.vue'
import SettingsView from '@/views/SettingsView.vue'

defineProps<{
  adminMe: AdminMe | null
}>()

const { adminPage, navigateTo } = useHashRouter()

const navItems: { id: AdminPage; label: string; icon: string }[] = [
  { id: 'overview', label: '概览', icon: 'ph-gauge' },
  { id: 'posts', label: '文章', icon: 'ph-article' },
  { id: 'profile', label: '个人资料', icon: 'ph-user-gear' },
  { id: 'settings', label: '站点设置', icon: 'ph-gear' },
]

const posts = ref<Post[]>([])
const loading = ref(true)
const loadError = ref(false)

const loadPosts = async () => {
  loadError.value = false
  try {
    posts.value = await listPosts({ cache: false })
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const retryLoad = () => {
  loading.value = true
  void loadPosts()
}

const isEditorOpen = ref(false)
const editingPost = ref<Post | null>(null)
const editorRef = ref<InstanceType<typeof AdminEditor> | null>(null)
const hashWhenOpened = ref('')

const openCreate = () => {
  editingPost.value = null
  isEditorOpen.value = true
}

const openEdit = (post: Post) => {
  editingPost.value = post
  isEditorOpen.value = true
}

const closeEditor = () => {
  isEditorOpen.value = false
  editingPost.value = null
}

const savePost = async (content: string, title: string) => {
  if (!title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  if (!content.trim()) {
    ElMessage.warning('内容不能为空')
    return
  }
  try {
    if (editingPost.value) {
      await updatePost(editingPost.value.id, { ...editingPost.value, title: title.trim(), content })
    } else {
      await createPost({ title: title.trim(), content, pinned: false })
    }
  } catch {
    // 错误提示已由 apiFetch 统一显示，保持编辑器打开
    return
  }
  closeEditor()
  await loadPosts()
}

const togglePin = async (post: Post) => {
  try {
    await updatePost(post.id, { ...post, pinned: !post.pinned })
  } catch {
    return
  }
  await loadPosts()
}

const removePost = async (post: Post) => {
  try {
    await ElMessageBox.confirm(`删除《${post.title}》？删除后无法恢复。`, '删除文章', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'modern-confirm',
    })
  } catch {
    // 用户取消
    return
  }
  try {
    await apiDeletePost(post.id)
  } catch {
    return
  }
  await loadPosts()
}

// 记录打开编辑器时的哈希，用于拦截离开
watch(isEditorOpen, (open) => {
  if (open) hashWhenOpened.value = window.location.hash
})

const handleHashChange = () => {
  if (!isEditorOpen.value) return
  const newHash = window.location.hash
  // 由下面的恢复操作触发的二次事件，忽略
  if (newHash === hashWhenOpened.value) return

  if (!editorRef.value?.hasUnsavedChanges) {
    closeEditor()
    return
  }

  // 恢复原哈希并弹出确认
  window.location.hash = hashWhenOpened.value
  setTimeout(() => {
    if (confirm('你有未保存的更改，确定要离开吗？')) {
      closeEditor()
      window.location.hash = newHash
    }
  }, 0)
}

onMounted(() => {
  void loadPosts()
  // 捕获阶段：先于路由的 hashchange 监听执行
  window.addEventListener('hashchange', handleHashChange, true)
})

onUnmounted(() => {
  window.removeEventListener('hashchange', handleHashChange, true)
})
</script>

<template>
  <div class="fixed inset-x-0 top-8 bottom-0 z-20 flex flex-col lg:flex-row">
    <nav
      class="shrink-0 lg:w-[220px] lg:h-full flex lg:flex-col gap-1 p-2 lg:p-4 overflow-x-auto lg:overflow-visible scrollbar-hide bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl border-b lg:border-b-0 lg:border-r border-black/5 dark:border-white/10"
      aria-label="后台导航"
    >
      <div class="hidden lg:block px-3 pt-1 pb-3 text-xs font-medium text-gray-400">后台管理</div>
      <button
        v-for="item in navItems"
        :key="item.id"
        class="shrink-0 flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium whitespace-nowrap transition-colors"
        :class="
          adminPage === item.id
            ? 'bg-[#007AFF] text-white'
            : 'text-gray-600 dark:text-gray-300 hover:bg-black/5 dark:hover:bg-white/10'
        "
        :aria-current="adminPage === item.id ? 'page' : undefined"
        @click="navigateTo('admin', { adminPage: item.id })"
      >
        <i :class="['ph', item.icon, 'text-lg']"></i>
        {{ item.label }}
      </button>
      <div class="hidden lg:block flex-1"></div>
      <button
        class="shrink-0 flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium whitespace-nowrap text-gray-600 dark:text-gray-300 hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
        @click="navigateTo('blog')"
      >
        <i class="ph ph-arrow-u-up-left text-lg"></i>
        返回博客
      </button>
    </nav>

    <main class="flex-1 min-w-0 min-h-0 overflow-y-auto mac-scrollbar">
      <div class="mx-auto max-w-[1200px] p-4 lg:p-8">
        <div
          v-if="loadError && (adminPage === 'overview' || adminPage === 'posts')"
          class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-sm border border-black/5 dark:border-white/10 py-16 text-center"
        >
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">文章加载失败</p>
          <button
            class="px-3 py-1.5 text-sm rounded-lg border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-200 hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
            @click="retryLoad"
          >
            重试
          </button>
        </div>
        <AdminOverview
          v-else-if="adminPage === 'overview'"
          :posts="posts"
          :loading="loading"
          :adminMe="adminMe"
          @create="openCreate"
          @edit="openEdit"
        />
        <AdminPosts
          v-else-if="adminPage === 'posts'"
          :posts="posts"
          :loading="loading"
          @create="openCreate"
          @edit="openEdit"
          @toggle-pin="togglePin"
          @delete="removePost"
        />
        <ProfileCenter v-else-if="adminPage === 'profile'" />
        <SettingsView v-else />
      </div>
    </main>

    <Teleport to="body">
      <AdminEditor
        v-if="isEditorOpen"
        ref="editorRef"
        :isOpen="isEditorOpen"
        :initialContent="editingPost?.content ?? ''"
        :initialTitle="editingPost?.title ?? ''"
        @close="closeEditor"
        @save="savePost"
      />
    </Teleport>
  </div>
</template>
