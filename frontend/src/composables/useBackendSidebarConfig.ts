import { computed, ref } from 'vue'
import type { MenuItem } from '@/api/types'
import { listMenuItems, saveMenuItems } from '@/api/site'

const DEFAULT_MENU_ITEMS: MenuItem[] = [
  { id: 'blog', label: 'Blog', icon: 'ph-house', requiresAuth: false, visible: true, order: 1 },
  { id: 'admin', label: 'Dashboard', icon: 'ph-squares-four', requiresAuth: true, visible: true, order: 2 },
  { id: 'settings', label: 'Settings', icon: 'ph-gear', requiresAuth: true, visible: true, order: 3 },
]

const menuItems = ref<MenuItem[]>([])
const isLoaded = ref(false)
const isSaving = ref(false)

export function useBackendSidebarConfig() {
  const loadConfig = async () => {
    if (isLoaded.value) return
    try {
      menuItems.value = await listMenuItems()
    } catch {
      menuItems.value = JSON.parse(JSON.stringify(DEFAULT_MENU_ITEMS))
    } finally {
      isLoaded.value = true
    }
  }

  const persist = async () => {
    if (isSaving.value) return
    isSaving.value = true
    try {
      menuItems.value = await saveMenuItems(menuItems.value)
    } finally {
      isSaving.value = false
    }
  }

  const visibleMenuItems = computed(() => menuItems.value.filter((x) => x.visible).sort((a, b) => a.order - b.order))
  const allMenuItems = computed(() => [...menuItems.value].sort((a, b) => a.order - b.order))

  const toggleVisibility = async (id: string) => {
    const item = menuItems.value.find((i) => i.id === id)
    if (!item) return
    item.visible = !item.visible
    await persist()
  }

  const toggleRequiresAuth = async (id: string) => {
    const item = menuItems.value.find((i) => i.id === id)
    if (!item) return
    item.requiresAuth = !item.requiresAuth
    await persist()
  }

  const reorderItems = async (items: MenuItem[]) => {
    menuItems.value = items.map((item, index) => ({
      ...item,
      order: index + 1,
    }))
    await persist()
  }

  const resetToDefault = async () => {
    menuItems.value = JSON.parse(JSON.stringify(DEFAULT_MENU_ITEMS))
    await persist()
  }

  return {
    visibleMenuItems,
    allMenuItems,
    isSaving,
    loadConfig,
    toggleVisibility,
    toggleRequiresAuth,
    reorderItems,
    resetToDefault,
  }
}
