import { computed, ref } from 'vue'
import { listPostsPage } from '@/api/posts'
import type { Post } from '@/api/types'

// 首页文章流：模块级状态，离开首页（BlogView 卸载）后保留，返回时先展示缓存再后台刷新。
// 桌面端与移动端各挂载一个 BlogView，共用这一份状态，避免重复请求。

export const PAGE_SIZE = 5

const posts = ref<Post[]>([])
const page = ref(0)
const hasMore = ref(true)
const loading = ref(false)
const loadError = ref(false)
// 后台刷新拿到的第一页；用户点击胶囊后才替换列表，避免内容在眼前跳动
const freshFirstPage = ref<Post[] | null>(null)
const newCount = ref(0)
let checking = false

// 各滚动容器离开时的位置（桌面/移动端容器不同）
const scrollTops = new WeakMap<HTMLElement, number>()

const hasCache = computed(() => posts.value.length > 0)

const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  loading.value = true
  loadError.value = false
  try {
    const data = await listPostsPage(page.value, PAGE_SIZE)
    // 分页期间若有新文章插入，后一页可能与已有数据重叠，按 id 去重
    const known = new Set(posts.value.map((p) => p.id))
    posts.value.push(...data.filter((p) => !known.has(p.id)))
    if (data.length < PAGE_SIZE) {
      hasMore.value = false
    } else {
      page.value += 1
    }
  } catch (e) {
    console.error('加载文章失败:', e)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/**
 * 后台拉取第一页，与缓存对比：
 * - 已缓存的文章静默更新计数等字段
 * - 比缓存中最大 id 更新的文章计为新文章，交给胶囊提示
 */
const checkForNew = async () => {
  if (checking || !hasCache.value) return
  checking = true
  try {
    // 绕过 apiFetch 默认 60s 的 GET 缓存，否则短时间内返回首页拿到的还是旧数据
    const data = await listPostsPage(0, PAGE_SIZE, { cache: false })
    const byId = new Map(data.map((p) => [p.id, p]))
    posts.value = posts.value.map((p) => byId.get(p.id) ?? p)

    const maxCachedId = Math.max(...posts.value.map((p) => p.id))
    const count = data.filter((p) => p.id > maxCachedId).length
    newCount.value = count
    freshFirstPage.value = count > 0 ? data : null
  } catch (e) {
    // 后台刷新失败不打扰用户，继续展示缓存
    console.warn('刷新文章列表失败:', e)
  } finally {
    checking = false
  }
}

/** 点击胶囊：用最新第一页替换列表，从头分页 */
const applyNew = () => {
  const data = freshFirstPage.value
  if (!data) return
  posts.value = data
  page.value = 1
  hasMore.value = data.length >= PAGE_SIZE
  loadError.value = false
  freshFirstPage.value = null
  newCount.value = 0
}

export function useBlogFeed() {
  return {
    posts,
    hasMore,
    loading,
    loadError,
    newCount,
    hasCache,
    loadMore,
    checkForNew,
    applyNew,
    saveScroll: (el: HTMLElement | null) => {
      if (el) scrollTops.set(el, el.scrollTop)
    },
    restoreScroll: (el: HTMLElement | null) => {
      if (el) el.scrollTop = scrollTops.get(el) ?? 0
    },
  }
}
