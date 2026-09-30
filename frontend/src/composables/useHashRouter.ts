import { ref, computed, onMounted } from 'vue'

type ViewId = 'blog' | 'login' | 'admin' | 'settings' | 'article'

export type AdminPage = 'overview' | 'posts' | 'profile' | 'settings'

const ADMIN_PAGES: AdminPage[] = ['overview', 'posts', 'profile', 'settings']

interface RouteParams {
  articleId?: number
  adminPage?: AdminPage
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
  const adminPage = computed<AdminPage>(() => currentRoute.value.params.adminPage ?? 'overview')

  const navigateTo = (view: ViewId, params?: RouteParams) => {
    // 站点设置已并入后台
    if (view === 'settings') {
      window.location.hash = '#/admin/settings'
      return
    }
    let hash = `#/${view}`
    if (view === 'admin' && params?.adminPage && params.adminPage !== 'overview') {
      hash = `#/admin/${params.adminPage}`
    } else if (params?.articleId) {
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

    // 旧的 #/settings 改写为 #/admin/settings，不留历史记录
    if (segments[0] === 'settings') {
      currentRoute.value = { view: 'admin', params: { adminPage: 'settings' } }
      window.location.replace('#/admin/settings')
      return
    }

    // 处理 admin 及其子页面
    if (segments[0] === 'admin') {
      const sub = segments[1] as AdminPage | undefined
      const page: AdminPage = sub && ADMIN_PAGES.includes(sub) ? sub : 'overview'
      currentRoute.value = { view: 'admin', params: { adminPage: page } }
      return
    }

    // 处理静态路由
    if (segments[0] === 'blog' || segments[0] === 'login') {
      currentRoute.value = { view: segments[0], params: {} }
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
    adminPage,
    navigateTo
  }
}
