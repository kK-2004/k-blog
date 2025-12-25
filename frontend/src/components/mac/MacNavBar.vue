<script setup lang="ts">
import AvatarCircle from './AvatarCircle.vue'

defineProps<{
  timeStr: string
  theme: 'light' | 'dark'
}>()

defineEmits<{
  (e: 'toggle-sidebar'): void
  (e: 'avatar-click'): void
  (e: 'toggle-theme'): void
}>()
</script>

<template>
  <header
    class="fixed top-0 left-0 right-0 z-40 h-8 bg-white/70 dark:bg-black/40 backdrop-blur-xl border-b border-white/10 dark:border-white/5 flex items-center justify-between px-4 select-none text-sm text-gray-800 dark:text-gray-200 shadow-sm transition-colors duration-500"
  >
    <div class="flex items-center gap-4 h-full">
      <button class="hover:bg-black/5 dark:hover:bg-white/10 p-1 rounded transition-colors" @click="$emit('toggle-sidebar')">
        <i class="ph ph-sidebar text-lg"></i>
      </button>
      <div class="flex items-center gap-3">
        <button class="relative group cursor-pointer" @click="$emit('avatar-click')">
          <AvatarCircle name="Admin" size="sm" />
          <span class="absolute top-0 right-0 w-1.5 h-1.5 bg-green-500 border border-white dark:border-black rounded-full"></span>
        </button>
        <span class="font-bold tracking-tight">KK Blog</span>
      </div>
    </div>

    <div class="flex items-center gap-4">
      <button class="p-1 rounded hover:bg-black/5 dark:hover:bg-white/10" @click="$emit('toggle-theme')">
        <i :class="['ph', theme === 'light' ? 'ph-moon' : 'ph-sun', 'text-lg']"></i>
      </button>
      <div class="flex items-center gap-2 opacity-90 text-xs font-medium font-mono pl-3 border-l border-gray-300 dark:border-gray-700 h-4">
        <span>{{ timeStr }}</span>
      </div>
    </div>
  </header>
</template>

