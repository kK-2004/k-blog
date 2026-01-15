import { computed, ref } from 'vue'
import type { QuickAction } from '@/api/types'
import { listQuickActions, saveQuickActions } from '@/api/site'

const DEFAULT_QUICK_ACTIONS: QuickAction[] = [
  { id: 'home', title: '回到首页', description: '打开博客首页', icon: 'ph-house', targetType: 'internal', target: '#/blog', visible: true, order: 1 },
  { id: 'dashboard', title: '后台管理', description: '打开后台面板', icon: 'ph-squares-four', targetType: 'internal', target: '#/admin', visible: true, order: 2 },
  { id: 'settings', title: '系统设置', description: '配置侧边栏与快捷入口', icon: 'ph-gear', targetType: 'internal', target: '#/settings', visible: true, order: 3 },
]

const quickActions = ref<QuickAction[]>([])
const isLoaded = ref(false)
const isSaving = ref(false)

const newId = (): string => {
  try {
    return crypto.randomUUID()
  } catch {
    return `qa_${Date.now()}_${Math.random().toString(16).slice(2)}`
  }
}

export function useQuickActionsConfig() {
  const loadConfig = async () => {
    if (isLoaded.value) return
    try {
      const items = await listQuickActions()
      quickActions.value = items.map((x) => ({
        ...x,
        description: x.description ?? '',
        targetType: x.targetType === 'external' ? 'external' : 'internal',
      }))
    } catch {
      quickActions.value = JSON.parse(JSON.stringify(DEFAULT_QUICK_ACTIONS))
    } finally {
      isLoaded.value = true
    }
  }

  const persist = async () => {
    if (isSaving.value) return
    isSaving.value = true
    try {
      quickActions.value = await saveQuickActions(quickActions.value)
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
    quickActions.value = JSON.parse(JSON.stringify(DEFAULT_QUICK_ACTIONS))
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
