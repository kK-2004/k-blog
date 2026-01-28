<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, readonly, ref, watch } from 'vue'
import { renderMarkdown } from '@/composables/useMarkdown'
import ImageLightbox from './ImageLightbox.vue'

const props = defineProps<{
  initialContent?: string
  initialTitle?: string
  isOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save', content: string, title: string): void
}>()

const content = ref(props.initialContent || '')
const title = ref(props.initialTitle || '')
const originalTitle = ref(props.initialTitle || '')
const originalContent = ref(props.initialContent || '')

const hasUnsavedChanges = computed(() =>
  title.value !== originalTitle.value ||
  content.value !== originalContent.value
)
const previewMode = ref(false)
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const codeMenuOpen = ref(false)
const imageMenuOpen = ref(false)
const imageInputRef = ref<HTMLInputElement | null>(null)
const uploadingImage = ref(false)
const selectedImageSize = ref<'original' | 'small' | 'medium' | 'large' | 'half'>('original')
const lightboxImageUrl = ref('')

watch(
  () => props.initialContent,
  (newVal) => {
    content.value = newVal || ''
  },
)

watch(
  () => props.initialTitle,
  (newVal) => {
    title.value = newVal || ''
    originalTitle.value = newVal || ''
  },
)

watch(
  () => props.isOpen,
  (isOpen) => {
    if (isOpen) {
      originalTitle.value = props.initialTitle || ''
      originalContent.value = props.initialContent || ''
    }
  },
)

const handleClose = () => {
  if (hasUnsavedChanges.value) {
    if (confirm('你有未保存的更改，确定要离开吗？')) {
      emit('close')
    }
  } else {
    emit('close')
  }
}

const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (hasUnsavedChanges.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

// 暴露给父组件使用
defineExpose({
  hasUnsavedChanges: readonly(hasUnsavedChanges)
})

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

const selectImageSize = (size: 'original' | 'small' | 'medium' | 'large' | 'half') => {
  selectedImageSize.value = size
  imageMenuOpen.value = false
  imageInputRef.value?.click()
}

const handleImageSelect = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 重置 input 以便可以重复选择同一文件
  input.value = ''

  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    alert('图片大小不能超过 10MB')
    return
  }

  uploadingImage.value = true

  try {
    // 1. 获取预签名 URL
    const presignRes = await fetch('/api/admin/images/presign', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        filename: file.name,
        contentType: file.type
      })
    })

    if (!presignRes.ok) {
      throw new Error('获取上传地址失败')
    }

    const presignData = await presignRes.json()

    // 2. 直接上传到 OSS（需要携带签名时包含的 header）
    const uploadRes = await fetch(presignData.uploadUrl, {
      method: 'PUT',
      body: file,
      headers: {
        'Content-Type': file.type,
        'x-oss-object-acl': 'public-read'
      }
    })

    if (!uploadRes.ok) {
      throw new Error('上传失败')
    }

    // 3. 根据选择的尺寸插入图片
    const textarea = textareaRef.value
    if (textarea) {
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      const imageUrl = presignData.url

      let imageMarkdown = ''
      switch (selectedImageSize.value) {
        case 'original':
          imageMarkdown = `\n![${file.name}](${imageUrl})\n`
          break
        case 'small':
          imageMarkdown = `\n<img src="${imageUrl}" style="width: 200px;" alt="${file.name}">\n`
          break
        case 'medium':
          imageMarkdown = `\n<img src="${imageUrl}" style="width: 400px;" alt="${file.name}">\n`
          break
        case 'large':
          imageMarkdown = `\n<img src="${imageUrl}" style="width: 600px;" alt="${file.name}">\n`
          break
        case 'half':
          imageMarkdown = `\n<img src="${imageUrl}" style="width: 50%;" alt="${file.name}">\n`
          break
      }

      content.value = content.value.substring(0, start) + imageMarkdown + content.value.substring(end)

      nextTick(() => {
        textarea.focus()
        const newCursorPos = start + imageMarkdown.length
        textarea.setSelectionRange(newCursorPos, newCursorPos)
      })
    }
  } catch (err) {
    console.error('图片上传失败:', err)
    alert('图片上传失败，请重试')
  } finally {
    uploadingImage.value = false
  }
}

// 图片尺寸选项
const imageSizeOptions = [
  { key: 'original', label: '原图', desc: '原始大小' },
  { key: 'small', label: '小图', desc: '200px' },
  { key: 'medium', label: '中图', desc: '400px' },
  { key: 'large', label: '大图', desc: '600px' },
  { key: 'half', label: '50%', desc: '相对宽度' },
]

// 预览区图片点击处理
const handlePreviewImageClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.tagName === 'IMG') {
    const img = target as HTMLImageElement
    lightboxImageUrl.value = img.src
  }
}

const closeLightbox = () => {
  lightboxImageUrl.value = ''
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-[100] bg-white dark:bg-[#1e1e1e] flex flex-col animate-[fadeIn_0.2s_ease-out]">
    <div class="h-14 border-b border-gray-200 dark:border-white/10 flex items-center justify-between px-6 bg-gray-50 dark:bg-[#252525]">
      <div class="flex items-center gap-4">
        <button class="text-gray-500 hover:text-red-500 transition-colors" @click="handleClose">
          <i class="ph ph-x text-xl"></i>
        </button>
        <div class="h-4 w-px bg-gray-300 dark:bg-gray-600"></div>
        <input
          v-model="title"
          type="text"
          placeholder="输入文章标题..."
          class="px-3 py-1.5 bg-white dark:bg-[#1e1e1e] border border-gray-200 dark:border-gray-600 rounded-lg text-sm font-medium text-gray-700 dark:text-gray-200 outline-none focus:border-blue-500 dark:focus:border-blue-400 focus:ring-1 focus:ring-blue-500 dark:focus:ring-blue-400 transition-all w-64 placeholder:text-gray-400 dark:placeholder:text-gray-500"
        />
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

        <!-- 图片按钮 + 尺寸选择菜单 -->
        <div class="relative group/image-menu" @mouseenter="imageMenuOpen = true" @mouseleave="imageMenuOpen = false">
          <button
            class="p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 text-gray-600 dark:text-gray-300"
            title="Image"
            :disabled="uploadingImage"
          >
            <i class="ph ph-image" :class="{ 'animate-pulse': uploadingImage }"></i>
            <i class="ph ph-caret-down text-[10px]"></i>
          </button>
          <!-- 不可见的桥接层 -->
          <div class="absolute top-full left-0 h-1 w-full -mt-1" v-show="imageMenuOpen"></div>
          <!-- 尺寸选择菜单 -->
          <div
            class="absolute top-full right-0 w-36 bg-white dark:bg-[#333] shadow-xl rounded-lg border border-gray-100 dark:border-black py-1 z-50"
            :class="imageMenuOpen ? 'block' : 'hidden'"
            @mouseenter="imageMenuOpen = true"
            @mouseleave="imageMenuOpen = false"
          >
            <button
              v-for="option in imageSizeOptions"
              :key="option.key"
              class="block w-full text-left px-4 py-2 hover:bg-blue-50 dark:hover:bg-blue-900/30 text-sm dark:text-gray-300 flex justify-between items-center"
              @click="selectImageSize(option.key as any)"
            >
              <span>{{ option.label }}</span>
              <span class="text-xs text-gray-400">{{ option.desc }}</span>
            </button>
          </div>
        </div>
        <input
          ref="imageInputRef"
          type="file"
          accept="image/*"
          class="hidden"
          @change="handleImageSelect"
        >
      </div>

      <div class="flex items-center gap-3">
        <button
          class="text-sm font-medium px-3 py-1.5 rounded bg-gray-200 dark:bg-gray-700 hover:opacity-80 text-gray-700 dark:text-gray-200"
          @click="previewMode = !previewMode"
        >
          {{ previewMode ? 'Edit' : 'Preview' }}
        </button>
        <button class="text-sm font-medium px-4 py-1.5 rounded bg-[#007AFF] text-white hover:opacity-90 shadow-md" @click="emit('save', content, title)">
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
          <div
            class="prose dark:prose-invert max-w-2xl mx-auto"
            v-html="renderMarkdown(content)"
            @click="handlePreviewImageClick"
          ></div>
        </div>
      </div>
    </div>

    <!-- 图片预览 Lightbox -->
    <ImageLightbox :image-url="lightboxImageUrl" :is-open="!!lightboxImageUrl" @close="closeLightbox" />
  </div>
</template>
