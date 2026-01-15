import { computed, ref } from 'vue'
import type { QuickAction } from '@/api/types'
import { listBlogQuickActions, saveBlogQuickActions } from '@/api/site'

const DEFAULT_BLOG_QUICK_ACTIONS: QuickAction[] = [
  { id: 'blog_home', title: '首页', description: '回到博客列表', icon: 'ph-house', targetType: 'internal', target: '#/blog', visible: true, order: 1 },
  { id: 'blog_login', title: '后台登录', description: '进入管理后台', icon: 'ph-lock-key', targetType: 'internal', target: '#/login', visible: true, order: 2 },
]

const quickActions = ref<QuickAction[]>([])
const isLoaded = ref(false)
const isSaving = ref(false)

const newId = (): string => {
  try {
    return crypto.randomUUID()
  } catch {
    return `bqa_${Date.now()}_${Math.random().toString(16).slice(2)}`
  }
}

export function useBlogQuickActionsConfig() {
  const loadConfig = async () => {
    if (isLoaded.value) return
    try {
      const items = await listBlogQuickActions()
      quickActions.value = items.map((x) => ({
        ...x,
        description: x.description ?? '',
        targetType: x.targetType === 'external' ? 'external' : 'internal',
      }))
    } catch {
      quickActions.value = JSON.parse(JSON.stringify(DEFAULT_BLOG_QUICK_ACTIONS))
    } finally {
      isLoaded.value = true
    }
  }

  const persist = async () => {
    if (isSaving.value) return
    isSaving.value = true
    try {
      quickActions.value = await saveBlogQuickActions(quickActions.value)
    } finally {
      isSaving.value = false
    }
  }

  const allQuickActions = computed(() => [...quickActions.value].sort((a, b) => a.order - b.order))
  const visibleQuickActions = computed(() => allQuickActions.value.filter((x) => x.visible))

  const addQuickAction = async () => {
    const nextOrder = (quickActions.value.reduce((max, x) => Math.max(max, x.order), 0) || 0) + 1
    quickActions.value.push({
      id: newId(),
      title: '新快捷入口',
      description: '',
      icon: 'ph-lightning',
      targetType: 'internal',
      target: '#/blog',
      visible: true,
      order: nextOrder,
    })
    await persist()
  }

  const removeQuickAction = async (id: string) => {
    quickActions.value = quickActions.value.filter((x) => x.id !== id).map((x, index) => ({ ...x, order: index + 1 }))
    await persist()
  }

  const updateQuickAction = async (id: string, patch: Partial<QuickAction>) => {
    const item = quickActions.value.find((x) => x.id === id)
    if (!item) return
    Object.assign(item, patch)
    await persist()
  }

  const toggleVisibility = async (id: string) => {
    const item = quickActions.value.find((x) => x.id === id)
    if (!item) return
    item.visible = !item.visible
    await persist()
  }

  const reorderItems = async (items: QuickAction[]) => {
    quickActions.value = items.map((item, index) => ({ ...item, order: index + 1 }))
    await persist()
  }

  const resetToDefault = async () => {
    quickActions.value = JSON.parse(JSON.stringify(DEFAULT_BLOG_QUICK_ACTIONS))
    await persist()
  }

  return {
    allQuickActions,
    visibleQuickActions,
    isSaving,
    loadConfig,
    addQuickAction,
    removeQuickAction,
    updateQuickAction,
    toggleVisibility,
    reorderItems,
    resetToDefault,
  }
}

