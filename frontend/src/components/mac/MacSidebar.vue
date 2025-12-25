<script setup lang="ts">
defineProps<{
  isOpen: boolean
  currentView: 'blog' | 'admin' | 'settings' | 'login'
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'update:currentView', value: 'blog' | 'admin' | 'settings'): void
}>()

const menuItems = [
  { id: 'blog', label: 'Blog', icon: 'ph-house' },
  { id: 'admin', label: 'Dashboard', icon: 'ph-squares-four' },
  { id: 'settings', label: 'Settings', icon: 'ph-gear' },
] as const
</script>

<template>
  <div>
    <div v-if="isOpen" class="fixed inset-0 bg-black/10 backdrop-blur-sm z-40 transition-opacity" @click="$emit('close')"></div>
    <aside
      class="fixed top-8 left-0 z-50 h-[calc(100vh-2rem)] w-64 bg-white/80 dark:bg-[#1e1e1e]/90 backdrop-blur-2xl border-r border-white/20 dark:border-white/5 shadow-2xl transition-transform duration-300"
      :class="isOpen ? 'translate-x-0' : '-translate-x-full'"
    >
      <div class="p-4 space-y-1 mt-4">
        <button
          v-for="item in menuItems"
          :key="item.id"
          class="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-all"
          :class="
            currentView === item.id
              ? 'bg-[#007AFF] text-white'
              : 'text-gray-600 dark:text-gray-300 hover:bg-black/5 dark:hover:bg-white/10'
          "
          @click="$emit('update:currentView', item.id); $emit('close')"
        >
          <i :class="['ph', item.icon, 'text-lg']"></i>
          {{ item.label }}
        </button>
      </div>
    </aside>
  </div>
</template>
