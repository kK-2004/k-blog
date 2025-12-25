<template>
  <div>
    <!-- 遮罩层 -->
    <div v-if="isOpen" @click="close" class="fixed inset-0 bg-black/10 backdrop-blur-sm z-40 transition-opacity"></div>
    
    <!-- 侧边栏主体 -->
    <aside 
      class="fixed top-8 left-0 z-50 h-[calc(100vh-2rem)] w-64 bg-white/80 dark:bg-[#1e1e1e]/90 backdrop-blur-2xl border-r border-white/20 dark:border-white/5 shadow-2xl transition-transform duration-300 ease-[cubic-bezier(0.25,1,0.5,1)]"
      :class="isOpen ? 'translate-x-0' : '-translate-x-full'"
    >
      <div class="p-4 space-y-1 mt-4">
        <div class="px-3 mb-4">
          <h3 class="text-xs font-bold text-gray-400 dark:text-gray-500 uppercase tracking-widest">Navigation</h3>
        </div>
        <button
          v-for="item in menuItems"
          :key="item.id"
          @click="updateCurrentView(item.id)"
          class="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-all"
          :class="currentView === item.id ? 'bg-[#007AFF] text-white shadow-md shadow-blue-500/30' : 'text-gray-600 dark:text-gray-300 hover:bg-black/5 dark:hover:bg-white/10'"
        >
          <i :class="['ph', item.icon, 'text-lg']"></i>
          {{ item.label }}
        </button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const props = defineProps({
  isOpen: {
    type: Boolean,
    required: true
  },
  currentView: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['close', 'update:currentView']);

const menuItems = ref([
  { id: 'blog', label: '博客首页', icon: 'ph-house' },
  { id: 'admin', label: '后台管理', icon: 'ph-squares-four' },
  { id: 'settings', label: '系统偏好', icon: 'ph-gear' },
]);

const close = () => {
  emit('close');
};

const updateCurrentView = (viewId) => {
  emit('update:currentView', viewId);
  emit('close');
};
</script>