<script setup lang="ts">
import { computed, watch, nextTick, ref } from 'vue'
import { renderMarkdown } from '@/composables/useMarkdown'
import type { Heading } from './ArticleTOC.vue'
import ImageLightbox from '../mac/ImageLightbox.vue'

const props = defineProps<{
  content: string
}>()

const emit = defineEmits<{
  (e: 'headings', headings: Heading[]): void
}>()

const renderedContent = computed(() => renderMarkdown(props.content))
const contentRef = ref<HTMLElement | null>(null)

// 图片预览
const lightboxImageUrl = ref('')

const handleContentImageClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.tagName === 'IMG') {
    const img = target as HTMLImageElement
    lightboxImageUrl.value = img.src
  }
}

const closeLightbox = () => {
  lightboxImageUrl.value = ''
}

// 从渲染后的 HTML 中提取标题
const extractHeadings = () => {
  nextTick(() => {
    if (!contentRef.value) return

    const headingElements = contentRef.value.querySelectorAll('h1, h2, h3, h4, h5, h6')
    const headings: Heading[] = []

    headingElements.forEach((heading, index) => {
      const id = `heading-${index}`

      // 为标题元素添加 ID
      if (heading instanceof HTMLElement) {
        heading.id = id
      }

      const text = heading.textContent || ''
      const level = parseInt(heading.tagName.charAt(1))

      headings.push({
        id,
        text,
        level
      })
    })

    emit('headings', headings)
  })
}

// 监听内容变化，提取标题
watch(() => props.content, () => {
  extractHeadings()
}, { immediate: true })
</script>

<template>
  <article ref="contentRef" class="prose prose-lg dark:prose-invert max-w-none
    prose-headings:font-bold prose-headings:tracking-tight prose-headings:text-gray-900 dark:prose-headings:text-gray-100
    prose-h1:text-3xl prose-h1:mt-12 prose-h1:mb-6
    prose-h2:text-2xl prose-h2:mt-10 prose-h2:mb-5 prose-h2:pb-2 prose-h2:border-b prose-h2:border-gray-200 dark:prose-h2:border-white/10
    prose-h3:text-xl prose-h3:mt-8 prose-h3:mb-4
    prose-p:text-gray-600 dark:prose-p:text-gray-300 prose-p:leading-8 prose-p:text-base
    prose-a:text-blue-600 dark:prose-a:text-blue-400 prose-a:no-underline hover:prose-a:underline prose-a:font-medium
    prose-strong:text-gray-900 dark:prose-strong:text-gray-100
    prose-code:text-pink-600 dark:prose-code:text-pink-400 prose-code:font-medium
    prose-pre:bg-gray-900 dark:prose-pre:bg-black/50
    prose-img:rounded-2xl prose-img:shadow-lg prose-img:my-8
    prose-blockquote:border-l-4 prose-blockquote:border-blue-500 prose-blockquote:bg-blue-50 dark:prose-blockquote:bg-blue-500/10 prose-blockquote:py-2 prose-blockquote:px-4
    prose-ul:list-disc prose-ol:list-decimal
  ">
    <div v-html="renderedContent" @click="handleContentImageClick" />
  </article>

  <!-- 图片预览 Lightbox -->
  <ImageLightbox :image-url="lightboxImageUrl" :is-open="!!lightboxImageUrl" @close="closeLightbox" />
</template>
