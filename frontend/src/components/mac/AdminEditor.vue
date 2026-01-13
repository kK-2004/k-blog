<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { renderMarkdown } from '@/composables/useMarkdown'

const props = defineProps<{
  initialContent?: string
  isOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save', content: string): void
}>()

const content = ref(props.initialContent || '')
const previewMode = ref(false)
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const codeMenuOpen = ref(false)

watch(
  () => props.initialContent,
  (newVal) => {
    content.value = newVal || ''
  },
)

const insertText = (before: string, after = '') => {
  const textarea = textareaRef.value
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selected = content.value.substring(start, end)

  const newText = before + selected + after
  content.value = content.value.substring(0, start) + newText + content.value.substring(end)

  nextTick(() => {
    textarea.focus()
    textarea.setSelectionRange(start + before.length, end + before.length)
  })
}

const insertCode = (lang: string) => insertText(`\n\`\`\`${lang}\n`, '\n```\n')
const insertImage = () => insertText('![Image Description](', ')')
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-[100] bg-white dark:bg-[#1e1e1e] flex flex-col animate-[fadeIn_0.2s_ease-out]">
    <div class="h-14 border-b border-gray-200 dark:border-white/10 flex items-center justify-between px-6 bg-gray-50 dark:bg-[#252525]">
      <div class="flex items-center gap-4">
        <button class="text-gray-500 hover:text-red-500 transition-colors" @click="emit('close')">
          <i class="ph ph-x text-xl"></i>
        </button>
        <div class="h-4 w-px bg-gray-300 dark:bg-gray-600"></div>
        <h2 class="font-bold text-gray-700 dark:text-gray-200">Editor</h2>
      </div>

      <div class="flex items-center gap-2">
        <button
          class="p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 text-gray-600 dark:text-gray-300"
          title="Bold"
          @click="insertText('**', '**')"
        >
          <i class="ph ph-text-b"></i>
        </button>
        <button
          class="p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 text-gray-600 dark:text-gray-300"
          title="Italic"
          @click="insertText('*', '*')"
        >
          <i class="ph ph-text-italic"></i>
        </button>
        <div class="h-4 w-px bg-gray-300 dark:bg-gray-600 mx-1"></div>

        <div class="relative group/code-menu" @mouseenter="codeMenuOpen = true" @mouseleave="codeMenuOpen = false">
          <button class="p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 flex items-center gap-1 text-gray-600 dark:text-gray-300">
            <i class="ph ph-code"></i>
            <i class="ph ph-caret-down text-[10px]"></i>
          </button>
          <!-- 不可见的桥接层，连接按钮和菜单 -->
          <div class="absolute top-full left-0 h-1 w-full -mt-1" v-show="codeMenuOpen"></div>
          <div
            class="absolute top-full left-0 w-40 bg-white dark:bg-[#333] shadow-xl rounded-lg border border-gray-100 dark:border-black py-1 z-50"
            :class="codeMenuOpen ? 'block' : 'hidden'"
            @mouseenter="codeMenuOpen = true"
            @mouseleave="codeMenuOpen = false"
          >
            <button class="block w-full text-left px-4 py-2 hover:bg-blue-50 dark:hover:bg-blue-900/30 text-xs dark:text-gray-300" @click="insertCode('javascript')">
              JavaScript
            </button>
            <button class="block w-full text-left px-4 py-2 hover:bg-blue-50 dark:hover:bg-blue-900/30 text-xs dark:text-gray-300" @click="insertCode('typescript')">
              TypeScript
            </button>
            <button class="block w-full text-left px-4 py-2 hover:bg-blue-50 dark:hover:bg-blue-900/30 text-xs dark:text-gray-300" @click="insertCode('python')">
              Python
            </button>
            <button class="block w-full text-left px-4 py-2 hover:bg-blue-50 dark:hover:bg-blue-900/30 text-xs dark:text-gray-300" @click="insertCode('java')">
              Java
            </button>
            <button class="block w-full text-left px-4 py-2 hover:bg-blue-50 dark:hover:bg-blue-900/30 text-xs dark:text-gray-300" @click="insertCode('go')">
              Go
            </button>
            <button class="block w-full text-left px-4 py-2 hover:bg-blue-50 dark:hover:bg-blue-900/30 text-xs dark:text-gray-300" @click="insertCode('cpp')">
              C++
            </button>
            <button class="block w-full text-left px-4 py-2 hover:bg-blue-50 dark:hover:bg-blue-900/30 text-xs dark:text-gray-300" @click="insertCode('c')">
              C
            </button>
            <div class="h-px bg-gray-200 dark:bg-gray-600 my-1"></div>
            <button class="block w-full text-left px-4 py-2 hover:bg-blue-50 dark:hover:bg-blue-900/30 text-xs dark:text-gray-300" @click="insertCode('html')">
              HTML
            </button>
            <button class="block w-full text-left px-4 py-2 hover:bg-blue-50 dark:hover:bg-blue-900/30 text-xs dark:text-gray-300" @click="insertCode('css')">
              CSS
            </button>
          </div>
        </div>

        <button
          class="p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 text-gray-600 dark:text-gray-300"
          title="Image"
          @click="insertImage"
        >
          <i class="ph ph-image"></i>
        </button>
      </div>

      <div class="flex items-center gap-3">
        <button
          class="text-sm font-medium px-3 py-1.5 rounded bg-gray-200 dark:bg-gray-700 hover:opacity-80 text-gray-700 dark:text-gray-200"
          @click="previewMode = !previewMode"
        >
          {{ previewMode ? 'Edit' : 'Preview' }}
        </button>
        <button class="text-sm font-medium px-4 py-1.5 rounded bg-[#007AFF] text-white hover:opacity-90 shadow-md" @click="emit('save', content)">
          Save
        </button>
      </div>
    </div>

    <div class="flex-1 overflow-hidden relative">
      <div class="flex h-full">
        <textarea
          v-show="!previewMode"
          ref="textareaRef"
          v-model="content"
          class="flex-1 h-full resize-none p-8 outline-none bg-white dark:bg-[#1e1e1e] text-gray-800 dark:text-gray-200 font-mono text-sm leading-6 mac-scrollbar"
          placeholder="# Write something amazing..."
        ></textarea>
        <div v-show="previewMode" class="flex-1 h-full overflow-y-auto p-8 bg-gray-50 dark:bg-[#252525] mac-scrollbar">
          <div class="prose dark:prose-invert max-w-2xl mx-auto" v-html="renderMarkdown(content)"></div>
        </div>
      </div>
    </div>
  </div>
</template>

