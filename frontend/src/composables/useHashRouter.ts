import { ref, computed, onMounted } from 'vue'

type ViewId = 'blog' | 'login' | 'admin' | 'settings' | 'article'

interface RouteParams {
  articleId?: number
}

interface Route {
  view: ViewId
  params: RouteParams
}

const currentRoute = ref<Route>({ view: 'blog', params: {} })

export function useHashRouter() {
  const currentView = computed<ViewId>(() => currentRoute.value.view)
  const routeParams = computed<RouteParams>(() => currentRoute.value.params)
  const articleId = computed<number | undefined>(() => currentRoute.value.params.articleId)

  const navigateTo = (view: ViewId, params?: RouteParams) => {
    let hash = `#/${view}`
    if (params?.articleId) {
      hash = `#/${view}/${params.articleId}`
    }
    window.location.hash = hash
  }

  const updateHashFromLocation = () => {
    const hash = window.location.hash.slice(1) // Remove #
    if (!hash || !hash.startsWith('/')) {
      currentRoute.value = { view: 'blog', params: {} }
      return
    }

    const path = hash.slice(1) // Remove /
    const segments = path.split('/').filter(Boolean)

    if (segments.length === 0) {
      currentRoute.value = { view: 'blog', params: {} }
      return
    }

    // 处理 article/:id 格式
    if (segments[0] === 'article' && segments[1] !== undefined) {
      const id = parseInt(segments[1]!, 10)
      if (!isNaN(id)) {
        currentRoute.value = { view: 'article', params: { articleId: id } }
        return
      }
    }

    // 处理静态路由
    if (segments[0] && ['blog', 'login', 'admin', 'settings'].includes(segments[0])) {
      currentRoute.value = { view: segments[0] as ViewId, params: {} }
    } else {
      currentRoute.value = { view: 'blog', params: {} }
    }
  }

  onMounted(() => {
    updateHashFromLocation()
    window.addEventListener('hashchange', updateHashFromLocation)
  })

  return {
    currentView,
    routeParams,
    articleId,
    navigateTo
  }
}
