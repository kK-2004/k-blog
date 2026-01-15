<script setup lang="ts">
import { ref } from 'vue'
import { useBackendSidebarConfig } from '@/composables/useBackendSidebarConfig'
import { useQuickActionsConfig } from '@/composables/useQuickActionsConfig'
import { useBlogQuickActionsConfig } from '@/composables/useBlogQuickActionsConfig'
import type { MenuItem } from '@/api/types'

const {
  allMenuItems,
  toggleVisibility: toggleMenuVisibility,
  toggleRequiresAuth,
  reorderItems: reorderMenuItems,
  resetToDefault: resetMenuToDefault,
  loadConfig: loadMenuConfig,
  isSaving: isSidebarSaving,
} = useBackendSidebarConfig()

const {
  allQuickActions,
  toggleVisibility: toggleQuickActionVisibility,
  reorderItems: reorderQuickActions,
  updateQuickAction,
  addQuickAction,
  removeQuickAction,
  resetToDefault: resetQuickActionsToDefault,
  loadConfig: loadQuickActionsConfig,
  isSaving: isQuickActionsSaving,
} = useQuickActionsConfig()

const {
  allQuickActions: allBlogQuickActions,
  toggleVisibility: toggleBlogQuickActionVisibility,
  reorderItems: reorderBlogQuickActions,
  updateQuickAction: updateBlogQuickAction,
  addQuickAction: addBlogQuickAction,
  removeQuickAction: removeBlogQuickAction,
  resetToDefault: resetBlogQuickActionsToDefault,
  loadConfig: loadBlogQuickActionsConfig,
  isSaving: isBlogQuickActionsSaving,
} = useBlogQuickActionsConfig()

void loadMenuConfig()
void loadQuickActionsConfig()
void loadBlogQuickActionsConfig()

const ICON_PRESETS = [
  'ph-lightning',
  'ph-house',
  'ph-squares-four',
  'ph-gear',
  'ph-plus',
  'ph-note-pencil',
  'ph-pencil-simple',
  'ph-article',
  'ph-file-text',
  'ph-tag',
  'ph-folder',
  'ph-magnifying-glass',
  'ph-link',
  'ph-browser',
  'ph-external-link',
  'ph-upload-simple',
  'ph-download-simple',
  'ph-chart-bar',
  'ph-chat-circle',
  'ph-bell',
]

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
    void reorderMenuItems(items)
  }

  draggedItem.value = null
  draggedIndex.value = null
}

const draggedQuickActionId = ref<string | null>(null)
const draggedQuickActionIndex = ref<number | null>(null)

const onQuickActionDragStart = (id: string, index: number) => {
  draggedQuickActionId.value = id
  draggedQuickActionIndex.value = index
}

const onQuickActionDrop = (targetIndex: number) => {
  if (draggedQuickActionIndex.value === null || draggedQuickActionIndex.value === targetIndex) return

  const items = [...allQuickActions.value]
  const [removed] = items.splice(draggedQuickActionIndex.value, 1)

  if (removed) {
    items.splice(targetIndex, 0, removed)
    void reorderQuickActions(items)
  }

  draggedQuickActionId.value = null
  draggedQuickActionIndex.value = null
}

const draggedBlogQuickActionId = ref<string | null>(null)
const draggedBlogQuickActionIndex = ref<number | null>(null)

const onBlogQuickActionDragStart = (id: string, index: number) => {
  draggedBlogQuickActionId.value = id
  draggedBlogQuickActionIndex.value = index
}

const onBlogQuickActionDrop = (targetIndex: number) => {
  if (draggedBlogQuickActionIndex.value === null || draggedBlogQuickActionIndex.value === targetIndex) return

  const items = [...allBlogQuickActions.value]
  const [removed] = items.splice(draggedBlogQuickActionIndex.value, 1)

  if (removed) {
    items.splice(targetIndex, 0, removed)
    void reorderBlogQuickActions(items)
  }

  draggedBlogQuickActionId.value = null
  draggedBlogQuickActionIndex.value = null
}
</script>

<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <!-- 侧边栏配置 -->
    <div class="bg-white/70 dark:bg-[#1e1e1e]/70 backdrop-blur-2xl rounded-2xl shadow-2xl border border-white/20 dark:border-white/10 p-8">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-lg font-bold text-gray-800 dark:text-gray-100">侧边栏菜单配置</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
            拖拽可调整菜单顺序，锁图标控制是否需要登录，眼睛图标控制是否显示
          </p>
        </div>
        <button
          class="px-3 py-1.5 text-sm bg-gray-100 dark:bg-white/10 hover:bg-gray-200 dark:hover:bg-white/20 rounded-lg transition-colors text-gray-700 dark:text-gray-300"
          @click="resetMenuToDefault"
          :disabled="isSidebarSaving"
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

          <div class="flex items-center gap-1">
            <button
              class="p-2 rounded-lg hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
              :class="item.requiresAuth ? 'text-orange-500' : 'text-gray-400'"
              :title="item.requiresAuth ? '点击设为公开' : '点击设为需要登录'"
              @click="toggleRequiresAuth(item.id)"
              :disabled="isSidebarSaving"
            >
              <i :class="['ph', item.requiresAuth ? 'ph-lock-key' : 'ph-lock-open', 'text-lg']"></i>
            </button>
            <button
              class="p-2 rounded-lg hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
              :class="item.visible ? 'text-green-500' : 'text-gray-400'"
              :title="item.visible ? '点击隐藏' : '点击显示'"
              @click="toggleMenuVisibility(item.id)"
              :disabled="isSidebarSaving"
            >
              <i :class="['ph', item.visible ? 'ph-eye' : 'ph-eye-slash', 'text-lg']"></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 后台快捷入口配置 -->
    <div class="bg-white/70 dark:bg-[#1e1e1e]/70 backdrop-blur-2xl rounded-2xl shadow-2xl border border-white/20 dark:border-white/10 p-8">
      <div class="flex items-start justify-between mb-6 gap-4">
        <div>
          <h2 class="text-lg font-bold text-gray-800 dark:text-gray-100">后台快速操作配置</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
            对应后台 Dashboard 的“快速操作”按钮：图标、标题、描述，以及跳转到外部 URL 或站内 #/xxx
          </p>
        </div>
        <div class="flex items-center gap-2">
          <button
            class="px-3 py-1.5 text-sm bg-gray-100 dark:bg-white/10 hover:bg-gray-200 dark:hover:bg-white/20 rounded-lg transition-colors text-gray-700 dark:text-gray-300"
            @click="resetQuickActionsToDefault"
            :disabled="isQuickActionsSaving"
          >
            重置默认
          </button>
          <button
            class="px-3 py-1.5 text-sm bg-[#007AFF] hover:opacity-90 rounded-lg transition-colors text-white shadow"
            @click="addQuickAction"
            :disabled="isQuickActionsSaving"
          >
            新增
          </button>
        </div>
      </div>

      <div class="space-y-3">
        <div
          v-for="(item, index) in allQuickActions"
          :key="item.id"
          class="p-4 bg-white/50 dark:bg-black/20 rounded-xl border border-gray-200 dark:border-white/10 transition-all"
          :class="{ 'opacity-50': !item.visible, 'cursor-move': true }"
          draggable="true"
          @dragstart="onQuickActionDragStart(item.id, index)"
          @dragover="onDragOver"
          @drop="onQuickActionDrop(index)"
        >
          <div class="flex items-start gap-3">
            <i class="ph ph-dots-six-vertical text-gray-400 text-xl mt-1"></i>

            <div class="w-10 h-10 rounded-lg bg-[#007AFF]/10 flex items-center justify-center flex-shrink-0">
              <i :class="['ph', item.icon, 'text-[#007AFF]', 'text-xl']"></i>
            </div>

            <div class="flex-1 min-w-0 space-y-2">
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <input
                  class="w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-black/20 text-sm"
                  v-model="item.title"
                  placeholder="标题"
                  @blur="updateQuickAction(item.id, { title: item.title })"
                  :disabled="isQuickActionsSaving"
                />
                <select
                  class="w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-black/20 text-sm"
                  v-model="item.icon"
                  @change="updateQuickAction(item.id, { icon: item.icon })"
                  :disabled="isQuickActionsSaving"
                >
                  <option v-for="icon in ICON_PRESETS" :key="icon" :value="icon">{{ icon }}</option>
                </select>
              </div>

              <input
                class="w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-black/20 text-sm"
                v-model="item.description"
                placeholder="描述（可选）"
                @blur="updateQuickAction(item.id, { description: item.description || '' })"
                :disabled="isQuickActionsSaving"
              />

              <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
                <select
                  class="w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-black/20 text-sm"
                  v-model="item.targetType"
                  @change="updateQuickAction(item.id, { targetType: item.targetType })"
                  :disabled="isQuickActionsSaving"
                >
                  <option value="internal">站内（#/xxx）</option>
                  <option value="external">外链（https://）</option>
                </select>
                <input
                  class="sm:col-span-2 w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-black/20 text-sm"
                  v-model="item.target"
                  :placeholder="item.targetType === 'external' ? 'https://example.com' : '#/settings'"
                  @blur="updateQuickAction(item.id, { target: item.target })"
                  :disabled="isQuickActionsSaving"
                />
              </div>
            </div>

            <div class="flex items-center gap-1 flex-shrink-0">
              <button
                class="p-2 rounded-lg hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
                :class="item.visible ? 'text-green-500' : 'text-gray-400'"
                :title="item.visible ? '点击隐藏' : '点击显示'"
                @click="toggleQuickActionVisibility(item.id)"
                :disabled="isQuickActionsSaving"
              >
                <i :class="['ph', item.visible ? 'ph-eye' : 'ph-eye-slash', 'text-lg']"></i>
              </button>
              <button
                class="p-2 rounded-lg hover:bg-black/5 dark:hover:bg-white/10 transition-colors text-red-500"
                title="删除"
                @click="removeQuickAction(item.id)"
                :disabled="isQuickActionsSaving"
              >
                <i class="ph ph-trash text-lg"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 博客个人栏快捷入口配置 -->
    <div class="bg-white/70 dark:bg-[#1e1e1e]/70 backdrop-blur-2xl rounded-2xl shadow-2xl border border-white/20 dark:border-white/10 p-8">
      <div class="flex items-start justify-between mb-6 gap-4">
        <div>
          <h2 class="text-lg font-bold text-gray-800 dark:text-gray-100">博客个人栏快捷入口</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
            显示在 Blog 页左侧（或移动端顶部）的个人信息卡片下方：图标、标题、描述，以及跳转到外部 URL 或站内 #/xxx
          </p>
        </div>
        <div class="flex items-center gap-2">
          <button
            class="px-3 py-1.5 text-sm bg-gray-100 dark:bg-white/10 hover:bg-gray-200 dark:hover:bg-white/20 rounded-lg transition-colors text-gray-700 dark:text-gray-300"
            @click="resetBlogQuickActionsToDefault"
            :disabled="isBlogQuickActionsSaving"
          >
            重置默认
          </button>
          <button
            class="px-3 py-1.5 text-sm bg-[#007AFF] hover:opacity-90 rounded-lg transition-colors text-white shadow"
            @click="addBlogQuickAction"
            :disabled="isBlogQuickActionsSaving"
          >
            新增
          </button>
        </div>
      </div>

      <div class="space-y-3">
        <div
          v-for="(item, index) in allBlogQuickActions"
          :key="item.id"
          class="p-4 bg-white/50 dark:bg-black/20 rounded-xl border border-gray-200 dark:border-white/10 transition-all"
          :class="{ 'opacity-50': !item.visible, 'cursor-move': true }"
          draggable="true"
          @dragstart="onBlogQuickActionDragStart(item.id, index)"
          @dragover="onDragOver"
          @drop="onBlogQuickActionDrop(index)"
        >
          <div class="flex items-start gap-3">
            <i class="ph ph-dots-six-vertical text-gray-400 text-xl mt-1"></i>

            <div class="w-10 h-10 rounded-lg bg-[#007AFF]/10 flex items-center justify-center flex-shrink-0">
              <i :class="['ph', item.icon, 'text-[#007AFF]', 'text-xl']"></i>
            </div>

            <div class="flex-1 min-w-0 space-y-2">
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <input
                  class="w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-black/20 text-sm"
                  v-model="item.title"
                  placeholder="标题"
                  @blur="updateBlogQuickAction(item.id, { title: item.title })"
                  :disabled="isBlogQuickActionsSaving"
                />
                <select
                  class="w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-black/20 text-sm"
                  v-model="item.icon"
                  @change="updateBlogQuickAction(item.id, { icon: item.icon })"
                  :disabled="isBlogQuickActionsSaving"
                >
                  <option v-for="icon in ICON_PRESETS" :key="icon" :value="icon">{{ icon }}</option>
                </select>
              </div>

              <input
                class="w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-black/20 text-sm"
                v-model="item.description"
                placeholder="描述（可选）"
                @blur="updateBlogQuickAction(item.id, { description: item.description || '' })"
                :disabled="isBlogQuickActionsSaving"
              />

              <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
                <select
                  class="w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-black/20 text-sm"
                  v-model="item.targetType"
                  @change="updateBlogQuickAction(item.id, { targetType: item.targetType })"
                  :disabled="isBlogQuickActionsSaving"
                >
                  <option value="internal">站内（#/xxx）</option>
                  <option value="external">外链（https://）</option>
                </select>
                <input
                  class="sm:col-span-2 w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-black/20 text-sm"
                  v-model="item.target"
                  :placeholder="item.targetType === 'external' ? 'https://example.com' : '#/blog'"
                  @blur="updateBlogQuickAction(item.id, { target: item.target })"
                  :disabled="isBlogQuickActionsSaving"
                />
              </div>
            </div>

            <div class="flex items-center gap-1 flex-shrink-0">
              <button
                class="p-2 rounded-lg hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
                :class="item.visible ? 'text-green-500' : 'text-gray-400'"
                :title="item.visible ? '点击隐藏' : '点击显示'"
                @click="toggleBlogQuickActionVisibility(item.id)"
                :disabled="isBlogQuickActionsSaving"
              >
                <i :class="['ph', item.visible ? 'ph-eye' : 'ph-eye-slash', 'text-lg']"></i>
              </button>
              <button
                class="p-2 rounded-lg hover:bg-black/5 dark:hover:bg-white/10 transition-colors text-red-500"
                title="删除"
                @click="removeBlogQuickAction(item.id)"
                :disabled="isBlogQuickActionsSaving"
              >
                <i class="ph ph-trash text-lg"></i>
              </button>
            </div>
          </div>
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
