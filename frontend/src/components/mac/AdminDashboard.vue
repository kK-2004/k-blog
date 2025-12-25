<script setup lang="ts">
import { ref } from 'vue'
import type { Post } from '@/data/initialPosts'
import AdminEditor from './AdminEditor.vue'
import AvatarCircle from './AvatarCircle.vue'

const props = defineProps<{
  posts: Post[]
}>()

const isEditorOpen = ref(false)
const currentEditId = ref<number | null>(null)
const editorContent = ref('')

const openEditor = (post: Post) => {
  currentEditId.value = post.id
  editorContent.value = post.content
  isEditorOpen.value = true
}

const closeEditor = () => {
  isEditorOpen.value = false
  currentEditId.value = null
}

const savePost = (newContent: string) => {
  const post = props.posts.find((p) => p.id === currentEditId.value)
  if (post) post.content = newContent
  closeEditor()
}
</script>

<template>
  <div class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden min-h-[500px]">
    <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center px-6 justify-between">
      <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
        <i class="ph ph-squares-four text-lg"></i>
        Dashboard
      </h2>
      <div class="flex gap-2 items-center">
        <span class="text-[10px] text-gray-500 uppercase tracking-widest bg-green-100 dark:bg-green-900/30 text-green-600 px-2 py-0.5 rounded">Live</span>
      </div>
    </div>

    <div class="p-0 overflow-x-auto">
      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="border-b border-gray-100 dark:border-white/10 text-xs text-gray-400 font-medium uppercase tracking-wider">
            <th class="p-4 pl-6">Title</th>
            <th class="p-4">Author</th>
            <th class="p-4 text-right">Stats</th>
            <th class="p-4 text-center pr-6">Action</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-50 dark:divide-white/5">
          <tr v-for="post in props.posts" :key="post.id" class="hover:bg-blue-50/50 dark:hover:bg-blue-900/10 transition-colors group">
            <td class="p-4 pl-6">
              <div class="font-medium text-gray-800 dark:text-gray-200 text-sm">{{ post.title }}</div>
              <div class="text-xs text-gray-400 truncate w-48">{{ post.content.substring(0, 30) }}...</div>
            </td>
            <td class="p-4 text-sm text-gray-600 dark:text-gray-400">
              <div class="flex items-center gap-2">
                <AvatarCircle :name="post.author" size="sm" />
                {{ post.author }}
              </div>
            </td>
            <td class="p-4 text-right">
              <div class="text-xs text-gray-500 dark:text-gray-400 font-mono">{{ post.views }} views</div>
            </td>
            <td class="p-4 pr-6 text-center">
              <button class="p-2 text-blue-500 hover:bg-blue-100 rounded dark:hover:bg-blue-900/30 transition-colors" title="Edit Fullscreen" @click="openEditor(post)">
                <i class="ph ph-arrows-out-simple"></i>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <Teleport to="body">
      <AdminEditor :isOpen="isEditorOpen" :initialContent="editorContent" @close="closeEditor" @save="savePost" />
    </Teleport>
  </div>
</template>

