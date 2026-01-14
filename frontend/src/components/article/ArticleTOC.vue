<script setup lang="ts">
export interface Heading {
  id: string
  text: string
  level: number
}

const props = defineProps<{
  headings: Heading[]
  activeId: string
}>()

const emit = defineEmits<{
  (e: 'navigate', id: string): void
}>()

const scrollToHeading = (id: string) => {
  const el = document.getElementById(id)
  const scrollContainer = document.querySelector('article.overflow-y-auto')

  if (el && scrollContainer) {
    const containerRect = (scrollContainer as HTMLElement).getBoundingClientRect()
    const elementRect = el.getBoundingClientRect()
    const offset = 100

    // 计算元素相对于滚动容器的位置
    const scrollTop = (scrollContainer as HTMLElement).scrollTop
    const elementPosition = scrollTop + elementRect.top - containerRect.top - offset

    (scrollContainer as HTMLElement).scrollTo({
      top: elementPosition,
      behavior: 'smooth'
    })

    emit('navigate', id)
  }
}

const getHeadingClass = (heading: Heading) => {
  const baseClass = 'block text-sm py-1.5 transition-all duration-200 border-l-2 -ml-[21px] pl-[19px]'

  if (props.activeId === heading.id) {
    return `${baseClass} text-blue-600 dark:text-blue-400 border-blue-600 dark:border-blue-400 font-medium`
  }

  return `${baseClass} text-gray-600 dark:text-gray-400 border-transparent hover:text-gray-900 dark:hover:text-gray-200 hover:border-gray-300 dark:hover:border-white/20`
}
</script>

<template>
  <nav class="article-toc">
    <div class="text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-widest mb-4">
      目录
    </div>
    <ul class="space-y-0.5">
      <li v-for="heading in headings" :key="heading.id" :style="{ marginLeft: `${(heading.level - 1) * 12}px` }">
        <a
          href="#"
          @click.prevent="scrollToHeading(heading.id)"
          :class="getHeadingClass(heading)"
        >
          {{ heading.text }}
        </a>
      </li>
    </ul>

    <!-- 空状态提示 -->
    <div v-if="headings.length === 0" class="text-xs text-gray-400 italic">
      暂无目录
    </div>
  </nav>
</template>

<style scoped>
.article-toc {
  @apply max-w-xs;
}
</style>
