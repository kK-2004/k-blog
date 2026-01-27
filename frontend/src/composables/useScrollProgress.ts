import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 追踪页面滚动进度和当前激活的标题
 * @returns scrollProgress 滚动百分比 (0-100), activeHeadingId 当前激活的标题 ID
 */
export function useScrollProgress() {
  const scrollProgress = ref(0)
  const activeHeadingId = ref('')
  
  let scrollContainer: HTMLElement | null = null
  let headingsElements: NodeListOf<Element> | null = null

  const updateScrollProgress = () => {
    if (!scrollContainer) return

    const containerHeight = scrollContainer.clientHeight
    const scrollHeight = scrollContainer.scrollHeight
    const scrollTop = scrollContainer.scrollTop

    const documentHeight = scrollHeight - containerHeight

    if (documentHeight > 0) {
      scrollProgress.value = Math.min(100, Math.max(0, (scrollTop / documentHeight) * 100))
    }
  }

  const updateActiveHeading = () => {
    if (!scrollContainer || !headingsElements) return

    const scrollTop = scrollContainer.scrollTop
    const scrollPosition = scrollTop + 150

    let currentHeading = ''
    for (const heading of headingsElements) {
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

  let inThrottle = false
  const throttledHandleScroll = () => {
    if (!inThrottle) {
      handleScroll()
      inThrottle = true
      setTimeout(() => {
        inThrottle = false
      }, 100)
    }
  }

  onMounted(() => {
    const initScrollTracking = (retryCount = 0) => {
      scrollContainer = document.querySelector('article.overflow-y-auto') as HTMLElement
      headingsElements = document.querySelectorAll('.prose h1, .prose h2, .prose h3, .prose h4, .prose h5, .prose h6')

      // 如果找到容器或有标题元素，初始化成功
      if (scrollContainer || headingsElements?.length) {
        if (scrollContainer) {
          scrollContainer.addEventListener('scroll', throttledHandleScroll, { passive: true })
        } else {
          window.addEventListener('scroll', throttledHandleScroll, { passive: true })
        }
        handleScroll()
        return
      }

      // 重试：最多重试 20 次，每次间隔 100ms（总共 2 秒）
      // 解决服务器网络慢时文章还未渲染的问题
      if (retryCount < 20) {
        setTimeout(() => initScrollTracking(retryCount + 1), 100)
      } else {
        // 最后 fallback 到 window
        window.addEventListener('scroll', throttledHandleScroll, { passive: true })
      }
    }

    // 首次尝试
    initScrollTracking()
  })

  onUnmounted(() => {
    if (scrollContainer) {
      scrollContainer.removeEventListener('scroll', throttledHandleScroll)
    } else {
      window.removeEventListener('scroll', throttledHandleScroll)
    }
  })

  return {
    scrollProgress,
    activeHeadingId
  }
}
