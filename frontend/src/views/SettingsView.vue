<script setup lang="ts">
import { ref } from 'vue'
import { useSidebarConfig, type MenuItem } from '@/composables/useSidebarConfig'

const { allMenuItems, toggleVisibility, reorderItems, resetToDefault } = useSidebarConfig()

const draggedItem = ref<MenuItem | null>(null)
const draggedIndex = ref<number | null>(null)

const onDragStart = (item: MenuItem, index: number) => {
  draggedItem.value = item
  draggedIndex.value = index
}

const onDragOver = (e: DragEvent) => {
  e.preventDefault()
}

const onDrop = (targetIndex: number) => {
  if (draggedIndex.value === null || draggedIndex.value === targetIndex) return

  const items = [...allMenuItems.value]
  const [removed] = items.splice(draggedIndex.value, 1)

  if (removed) {
    items.splice(targetIndex, 0, removed)
    reorderItems(items)
  }

  draggedItem.value = null
  draggedIndex.value = null
}
</script>

<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <!-- 侧边栏配置 -->
    <div class="bg-white/70 dark:bg-[#1e1e1e]/70 backdrop-blur-2xl rounded-2xl shadow-2xl border border-white/20 dark:border-white/10 p-8">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-lg font-bold text-gray-800 dark:text-gray-100">侧边栏菜单配置</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">拖拽可调整菜单顺序，点击眼睛图标切换显示</p>
        </div>
        <button
          class="px-3 py-1.5 text-sm bg-gray-100 dark:bg-white/10 hover:bg-gray-200 dark:hover:bg-white/20 rounded-lg transition-colors text-gray-700 dark:text-gray-300"
          @click="resetToDefault"
        >
          重置默认
        </button>
      </div>

      <div class="space-y-2">
        <div
          v-for="(item, index) in allMenuItems"
          :key="item.id"
          class="flex items-center gap-3 p-3 bg-white/50 dark:bg-black/20 rounded-xl border border-gray-200 dark:border-white/10 transition-all"
          :class="{
            'opacity-50': !item.visible,
            'cursor-move': true
          }"
          draggable="true"
          @dragstart="onDragStart(item, index)"
          @dragover="onDragOver"
          @drop="onDrop(index)"
        >
          <i class="ph ph-dots-six-vertical text-gray-400 text-xl"></i>

          <div class="w-8 h-8 rounded-lg bg-[#007AFF]/10 flex items-center justify-center">
            <i :class="['ph', item.icon, 'text-[#007AFF]']"></i>
          </div>

          <div class="flex-1">
            <div class="font-medium text-gray-800 dark:text-gray-200">{{ item.label }}</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">
              {{ item.requiresAuth ? '需要登录' : '公开访问' }}
            </div>
          </div>

          <button
            class="p-2 rounded-lg hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
            :class="item.visible ? 'text-green-500' : 'text-gray-400'"
            @click="toggleVisibility(item.id)"
          >
            <i :class="['ph', item.visible ? 'ph-eye' : 'ph-eye-slash', 'text-lg']"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- 其他配置 -->
    <div class="bg-white/70 dark:bg-[#1e1e1e]/70 backdrop-blur-2xl rounded-2xl shadow-2xl border border-white/20 dark:border-white/10 p-8">
      <h2 class="text-lg font-bold text-gray-800 dark:text-gray-100 mb-2">其他设置</h2>
      <p class="text-sm text-gray-500 dark:text-gray-400">这里留给你接入真实的配置项（主题、摘要服务、账号等）。</p>
    </div>
  </div>
</template>

