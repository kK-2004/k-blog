import { ref, computed, onMounted } from 'vue'

type ViewId = 'blog' | 'login' | 'admin' | 'settings'

const currentHash = ref<ViewId>('blog')

export function useHashRouter() {
  const currentView = computed<ViewId>(() => currentHash.value)

  const navigateTo = (view: ViewId) => {
    window.location.hash = `#/${view}`
  }

  const updateHashFromLocation = () => {
    const hash = window.location.hash.slice(1) // Remove #
    if (hash && hash.startsWith('/')) {
      const path = hash.slice(1) // Remove /
      if (['blog', 'login', 'admin', 'settings'].includes(path)) {
        currentHash.value = path as ViewId
      } else {
        currentHash.value = 'blog'
      }
    } else {
      // 如果没有 hash 或不是有效路径，默认为 blog
      currentHash.value = 'blog'
    }
  }

  onMounted(() => {
    updateHashFromLocation()
    window.addEventListener('hashchange', updateHashFromLocation)
  })

  return {
    currentView,
    navigateTo
  }
}
