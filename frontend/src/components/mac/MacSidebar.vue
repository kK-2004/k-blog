<script setup lang="ts">
import { computed } from 'vue'
import { useHashRouter } from '@/composables/useHashRouter'
import { useBackendSidebarConfig } from '@/composables/useBackendSidebarConfig'

const props = defineProps<{
  isOpen: boolean
  currentView: 'blog' | 'admin' | 'settings' | 'login'
  isAuthenticated: boolean
}>()

const { navigateTo } = useHashRouter()
const { visibleMenuItems, loadConfig } = useBackendSidebarConfig()
void loadConfig()

// 根据认证状态过滤可见菜单项
const filteredMenuItems = computed(() => {
  return visibleMenuItems.value.filter(item => {
    if (item.requiresAuth) {
      return props.isAuthenticated // 需要认证的菜单项只在登录后显示
    }
    return true
  })
})
</script>

<template>
  <aside
    class="fixed top-8 left-0 z-30 h-[calc(100vh-2rem)] w-64 bg-white/80 dark:bg-[#1e1e1e]/90 backdrop-blur-2xl border-r border-white/20 dark:border-white/5 shadow-2xl transition-transform duration-300 ease-out"
    :class="isOpen ? 'translate-x-0' : '-translate-x-full'"
  >
    <div class="p-4 space-y-1 mt-4">
      <button
        v-for="item in filteredMenuItems"
        :key="item.id"
        class="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-all"
        :class="
          currentView === item.id
            ? 'bg-[#007AFF] text-white'
            : 'text-gray-600 dark:text-gray-300 hover:bg-black/5 dark:hover:bg-white/10'
        "
        @click="navigateTo(item.id as any)"
      >
        <i :class="['ph', item.icon, 'text-lg']"></i>
        {{ item.label }}
      </button>
    </div>
  </aside>
</template>
