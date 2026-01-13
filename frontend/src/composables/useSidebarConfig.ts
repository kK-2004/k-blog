import { ref, computed } from 'vue'

export interface MenuItem {
  id: string
  label: string
  icon: string
  requiresAuth: boolean
  visible: boolean
  order: number
}

const DEFAULT_MENU_ITEMS: MenuItem[] = [
  { id: 'blog', label: 'Blog', icon: 'ph-house', requiresAuth: false, visible: true, order: 1 },
  { id: 'admin', label: 'Dashboard', icon: 'ph-squares-four', requiresAuth: true, visible: true, order: 2 },
  { id: 'settings', label: 'Settings', icon: 'ph-gear', requiresAuth: true, visible: true, order: 3 },
]

const menuItems = ref<MenuItem[]>([])
const isLoaded = ref(false)

const STORAGE_KEY = 'kblog-sidebar-config'

export function useSidebarConfig() {
  // 从 localStorage 加载配置
  const loadConfig = () => {
    if (isLoaded.value) return

    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved) {
        const parsed = JSON.parse(saved)
        menuItems.value = parsed
      } else {
        menuItems.value = JSON.parse(JSON.stringify(DEFAULT_MENU_ITEMS))
      }
    } catch (e) {
      menuItems.value = JSON.parse(JSON.stringify(DEFAULT_MENU_ITEMS))
    }
    isLoaded.value = true
  }

  // 保存配置到 localStorage
  const saveConfig = () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(menuItems.value))
  }

  // 获取当前用户可见的菜单项（按 order 排序）
  const visibleMenuItems = computed(() => {
    if (!isLoaded.value) {
      loadConfig()
    }
    return menuItems.value
      .filter(item => item.visible)
      .sort((a, b) => a.order - b.order)
  })

  // 获取所有菜单项（用于配置页面）
  const allMenuItems = computed(() => {
    if (!isLoaded.value) {
      loadConfig()
    }
    return menuItems.value.sort((a, b) => a.order - b.order)
  })

  // 切换菜单项的可见性
  const toggleVisibility = (id: string) => {
    const item = menuItems.value.find(i => i.id === id)
    if (item) {
      item.visible = !item.visible
      saveConfig()
    }
  }

  // 更新菜单项排序
  const reorderItems = (items: MenuItem[]) => {
    menuItems.value = items.map((item, index) => ({
      ...item,
      order: index + 1
    }))
    saveConfig()
  }

  // 重置为默认配置
  const resetToDefault = () => {
    menuItems.value = JSON.parse(JSON.stringify(DEFAULT_MENU_ITEMS))
    saveConfig()
  }

  return {
    visibleMenuItems,
    allMenuItems,
    toggleVisibility,
    reorderItems,
    resetToDefault,
    loadConfig
  }
}
