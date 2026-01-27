<script setup lang="ts">
import { computed, ref, watch, onMounted, onBeforeUnmount, nextTick} from 'vue'

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

// 移动端目录展开状态
const isMobileExpanded = ref(false)

// 移动端目录固定状态
const isMobileTocFixed = ref(false)
const mobileTocBarRef = ref<HTMLElement | null>(null)
const headerHeight = ref(0)
const tocOriginalOffsetTop = ref(0) // 记录目录的初始位置

// 当前激活标题的文字
const currentHeadingText = computed(() => {
  if (props.headings.length === 0) return '暂无目录'
  const active = props.headings.find(h => h.id === props.activeId)
  return active?.text || props.headings[0]?.text || '目录'
})

// 计算固定时的top值（header高度）
const fixedTopStyle = computed(() => {
  return isMobileTocFixed.value ? `${headerHeight.value}px` : 'auto'
})

// 滚动监听处理
const handleScroll = () => {
  const scrollContainer = document.querySelector('article.overflow-y-auto') as HTMLElement | null
  const headerEl = document.querySelector('[data-article-header]') as HTMLElement | null

  if (!scrollContainer || !mobileTocBarRef.value || !headerEl) return

  // 更新header高度
  headerHeight.value = headerEl.getBoundingClientRect().bottom

  // 获取滚动容器的滚动距离
  const scrollTop = scrollContainer.scrollTop

  // 计算触发固定的阈值：目录初始位置 - header高度
  const fixThreshold = tocOriginalOffsetTop.value - headerHeight.value

  // 根据滚动距离判断是否应该固定
  if (scrollTop >= fixThreshold) {
    isMobileTocFixed.value = true
  } else {
    isMobileTocFixed.value = false
  }
}

// 组件挂载时初始化
onMounted(() => {
  const initTocTracking = (retryCount = 0) => {
    const scrollContainer = document.querySelector('article.overflow-y-auto') as HTMLElement | null
    const headerEl = document.querySelector('[data-article-header]')

    if (scrollContainer && mobileTocBarRef.value && headerEl) {
      // 检查是否有标题元素（确保文章内容已渲染）
      const headings = document.querySelectorAll('.prose h1, .prose h2, .prose h3, .prose h4, .prose h5, .prose h6')
      if (headings.length === 0 && retryCount < 20) {
        // 标题还没渲染，继续等待
        setTimeout(() => initTocTracking(retryCount + 1), 100)
        return
      }

      // 记录目录的初始位置（相对于滚动容器）
      const containerRect = scrollContainer.getBoundingClientRect()
      const tocRect = mobileTocBarRef.value.getBoundingClientRect()
      const scrollTop = scrollContainer.scrollTop

      // 计算目录在文档中的位置
      tocOriginalOffsetTop.value = tocRect.top - containerRect.top + scrollTop

      // 添加滚动监听
      scrollContainer.addEventListener('scroll', handleScroll)

      // 初始检查一次位置
      handleScroll()
      return
    }

    // 重试：最多重试 20 次，每次间隔 100ms（总共 2 秒）
    if (retryCount < 20) {
      setTimeout(() => initTocTracking(retryCount + 1), 100)
    }
  }

  initTocTracking()
})

// 组件卸载时清理
onBeforeUnmount(() => {
  const scrollContainer = document.querySelector('article.overflow-y-auto')
  if (scrollContainer) {
    scrollContainer.removeEventListener('scroll', handleScroll)
  }
})

const scrollToHeading = async (id: string) => {
  const el = document.getElementById(id)
  const scrollContainer = document.querySelector('article.overflow-y-auto') as HTMLElement | null

  if (!el || !scrollContainer) return

  // 1. 强制折叠移动端目录
  if (isMobileExpanded.value) {
    isMobileExpanded.value = false
  }

  // 2. 等待 Vue 更新 DOM + 浏览器重绘布局
  await nextTick()

  // 使用 requestAnimationFrame 确保在下一帧渲染后计算
  requestAnimationFrame(() => {
    // 重新获取元素引用（防止引用失效）
    const targetEl = document.getElementById(id)
    if (!targetEl) return

    // --- 核心计算逻辑 ---

    // A. 获取基础参数
    const headerEl = document.querySelector('[data-article-header]') as HTMLElement | null
    // 此时菜单已折叠，offsetHeight 是只有栏目条的高度（不含展开列表）
    const tocBarHeight = mobileTocBarRef.value?.offsetHeight || 0
    const globalHeaderHeight = headerEl?.getBoundingClientRect().height || headerHeight.value || 0

    // B. 计算目标的"绝对"位置（相对于滚动容器内容的顶部）
    const containerRect = scrollContainer.getBoundingClientRect()
    const targetRect = targetEl.getBoundingClientRect()
    const currentScrollTop = scrollContainer.scrollTop
    const targetAbsoluteTop = currentScrollTop + (targetRect.top - containerRect.top)

    // C. 确定偏移量 (Offset)
    const isMobile = window.matchMedia('(max-width: 1024px)').matches
    let offset = 20 // 基础 Padding

    if (isMobile) {
      const safeTocOriginalTop = tocOriginalOffsetTop.value > 0 ? tocOriginalOffsetTop.value : 0

      // 判读逻辑简化：只要目标位置在 TOC 初始位置下方，最终状态 TOC 肯定是 Fixed 的
      // 加上一个小的容错 buffer (5px)
      const willBeFixed = targetAbsoluteTop > (safeTocOriginalTop - globalHeaderHeight + 5)

      if (willBeFixed) {
        offset += globalHeaderHeight + tocBarHeight
      } else {
        offset += globalHeaderHeight
      }
    } else {
      // 桌面端通常只需要减去头部高度
      offset += globalHeaderHeight
    }

    // D. 执行滚动
    const finalScrollTop = Math.max(0, targetAbsoluteTop - offset)

    scrollContainer.scrollTo({
      top: finalScrollTop,
      behavior: 'smooth'
    })

    emit('navigate', id)
  })
}

// 桌面端目录样式
const getDesktopHeadingClass = (heading: Heading) => {
  const baseClass = 'block text-sm py-1.5 transition-all duration-200 border-l-2 -ml-[21px] pl-[19px] cursor-pointer'

  if (props.activeId === heading.id) {
    return `${baseClass} text-blue-600 dark:text-blue-400 border-blue-600 dark:border-blue-400 font-medium`
  }

  return `${baseClass} text-gray-600 dark:text-gray-400 border-transparent hover:text-gray-900 dark:hover:text-gray-200 hover:border-gray-300 dark:hover:border-white/20`
}

const desktopHeadingClassMap = computed(() => {
  const map = new Map<string, string>()
  props.headings.forEach(heading => {
    map.set(heading.id, getDesktopHeadingClass(heading))
  })
  return map
})

// 移动端目录项样式
const getMobileHeadingClass = (heading: Heading) => {
  const isActive = props.activeId === heading.id

  // 根据级别设置缩进和字体大小
  const levelClass = heading.level === 2
      ? 'text-base font-medium'
      : 'text-sm pl-4'

  // 激活状态样式
  const activeClass = isActive
      ? 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/20'
      : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-white/5'

  return `block py-3 px-4 text-left transition-colors duration-200 ${levelClass} ${activeClass}`
}

// 监听 activeId 变化
watch(() => props.activeId, () => {
  // 可以在这里添加其他逻辑
})
</script>

<template>
  <div class="article-toc-container">
    <!-- 移动端目录占位符 - 当目录固定时显示，保持文档流 -->
    <div
        v-if="isMobileTocFixed"
        class="lg:hidden"
        :style="{ height: mobileTocBarRef?.offsetHeight + 'px' }"
    ></div>

    <!-- 移动端目录 - 动态固定定位 -->
    <div
        ref="mobileTocBarRef"
        data-article-toc-bar
        class="lg:hidden z-50 bg-white dark:bg-[#1e1e1e] border-b border-gray-200 dark:border-white/10 transition-all duration-200"
        :class="isMobileTocFixed ? 'fixed left-0 right-0' : 'relative'"
        :style="{ top: fixedTopStyle }"
    >
      <!-- 折叠状态 -->
      <div
          v-if="!isMobileExpanded"
          @click="isMobileExpanded = true"
          class="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-gray-50 dark:hover:bg-white/5 transition-colors"
      >
        <div class="flex items-center gap-2">
          <i class="ph ph-list text-lg text-gray-500 dark:text-gray-400"></i>
          <span class="text-sm font-medium text-gray-700 dark:text-gray-300">目录</span>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm text-blue-600 dark:text-blue-400 truncate max-w-[180px]">
            {{ currentHeadingText }}
          </span>
          <i class="ph ph-caret-down text-gray-400"></i>
        </div>
      </div>

      <!-- 展开状态 -->
      <div v-else class="toc-expanded">
        <!-- 头部 -->
        <div
            @click="isMobileExpanded = false"
            class="flex items-center justify-between px-4 py-3 cursor-pointer bg-gray-50 dark:bg-white/5 border-b border-gray-100 dark:border-white/5"
        >
          <span class="text-sm font-semibold text-gray-800 dark:text-gray-100">目录</span>
          <i class="ph ph-caret-up text-gray-400"></i>
        </div>

        <!-- 目录列表 -->
        <div class="max-h-[50vh] overflow-y-auto toc-list">
          <ul v-if="headings.length > 0" class="divide-y divide-gray-100 dark:divide-white/5">
            <li v-for="heading in headings" :key="heading.id">
              <a
                  href="#"
                  @click.prevent="scrollToHeading(heading.id)"
                  :class="getMobileHeadingClass(heading)"
              >
                <span v-if="heading.level === 3" class="text-gray-400 mr-2">·</span>
                {{ heading.text }}
              </a>
            </li>
          </ul>
          <div v-else class="text-center py-8 text-gray-400 text-sm">
            暂无目录
          </div>
        </div>
      </div>
    </div>

    <!-- 桌面端目录（保持原有样式） -->
    <nav class="article-toc hidden lg:block">
      <div class="text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-widest mb-4">
        目录
      </div>
      <ul class="space-y-0.5">
        <li v-for="heading in headings" :key="heading.id" :style="{ marginLeft: `${(heading.level - 1) * 12}px` }">
          <a
              href="#"
              @click.prevent="scrollToHeading(heading.id)"
              :class="desktopHeadingClassMap.get(heading.id)"
          >
            {{ heading.text }}
          </a>
        </li>
      </ul>

      <div v-if="headings.length === 0" class="text-xs text-gray-400 italic">
        暂无目录
      </div>
    </nav>
  </div>
</template>

<style scoped>
.article-toc {
  @apply max-w-xs;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  padding-bottom: 80px; /* 底部额外空间，防止被评论框遮挡 */
}

/* 桌面端隐藏滚动条但保留滚动功能 */
.article-toc::-webkit-scrollbar {
  width: 0;
  background: transparent;
}

.article-toc {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 移动端目录列表滚动条美化 */
.toc-list::-webkit-scrollbar {
  width: 4px;
}

.toc-list::-webkit-scrollbar-track {
  background: transparent;
}

.toc-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.dark .toc-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
}

/* 展开动画 */
.toc-expanded {
  animation: tocSlideDown 0.2s ease-out;
}

@keyframes tocSlideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
