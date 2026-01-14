import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 追踪页面滚动进度和当前激活的标题
 * @returns scrollProgress 滚动百分比 (0-100), activeHeadingId 当前激活的标题 ID
 */
export function useScrollProgress() {
  const scrollProgress = ref(0)
  const activeHeadingId = ref('')

  const updateScrollProgress = () => {
    // 查找正文滚动容器
    const scrollContainer = document.querySelector('article.overflow-y-auto')
    if (!scrollContainer) return

    const containerHeight = (scrollContainer as HTMLElement).clientHeight
    const scrollHeight = (scrollContainer as HTMLElement).scrollHeight
    const scrollTop = (scrollContainer as HTMLElement).scrollTop

    const documentHeight = scrollHeight - containerHeight

    if (documentHeight > 0) {
      scrollProgress.value = Math.min(100, Math.max(0, (scrollTop / documentHeight) * 100))
    }
  }

  const updateActiveHeading = () => {
    const headings = document.querySelectorAll('.prose h1, .prose h2, .prose h3, .prose h4, .prose h5, .prose h6')
    const scrollContainer = document.querySelector('article.overflow-y-auto')

    if (!scrollContainer) return

    const containerTop = (scrollContainer as HTMLElement).getBoundingClientRect().top
    const scrollTop = (scrollContainer as HTMLElement).scrollTop

    // 找到当前视口中最近的标题
    let currentHeading = ''
    const scrollPosition = scrollTop + 150 // 偏移量，用于更准确的判断

    for (const heading of headings) {
      if (heading instanceof HTMLElement && heading.offsetTop <= scrollPosition) {
        currentHeading = heading.id
      }
    }

    if (currentHeading) {
      activeHeadingId.value = currentHeading
    }
  }

  const handleScroll = () => {
    updateScrollProgress()
    updateActiveHeading()
  }

  onMounted(() => {
    // 延迟绑定，确保 DOM 已渲染
    setTimeout(() => {
      const scrollContainer = document.querySelector('article.overflow-y-auto')
      if (scrollContainer) {
        scrollContainer.addEventListener('scroll', handleScroll, { passive: true })
      } else {
        // 降级：如果没有找到容器，使用 window
        window.addEventListener('scroll', handleScroll, { passive: true })
      }
      // 初始化
      handleScroll()
    }, 100)
  })

  onUnmounted(() => {
    const scrollContainer = document.querySelector('article.overflow-y-auto')
    if (scrollContainer) {
      scrollContainer.removeEventListener('scroll', handleScroll)
    } else {
      window.removeEventListener('scroll', handleScroll)
    }
  })

  return {
    scrollProgress,
    activeHeadingId
  }
}
