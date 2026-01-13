<script setup lang="ts">
defineProps<{
  currentTab?: string
}>()

const emit = defineEmits<{
  (e: 'tab-change', tab: string): void
}>()

const tabs = [
  { id: 'overview', name: '概览', icon: 'ph-user' },
  { id: 'avatar', name: '头像设置', icon: 'ph-image' },
  { id: 'basic', name: '基本信息', icon: 'ph-identification-card' },
  { id: 'contact', name: '联系方式', icon: 'ph-address-book' },
  { id: 'social', name: '社交链接', icon: 'ph-link' },
]
</script>

<template>
  <div class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden">
    <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center px-6">
      <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
        <i class="ph ph-user-gear text-lg"></i>
        个人中心
      </h2>
    </div>

    <nav class="p-2">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors text-left text-sm"
        :class="[
          currentTab === tab.id
            ? 'bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 font-medium'
            : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'
        ]"
        @click="emit('tab-change', tab.id)"
      >
        <i :class="['ph', tab.icon, 'text-lg']"></i>
        {{ tab.name }}
      </button>
    </nav>
  </div>
</template>
