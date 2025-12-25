<template>
  <header class="fixed top-0 left-0 right-0 z-50 h-8 bg-white/70 dark:bg-black/40 backdrop-blur-xl border-b border-white/10 dark:border-white/5 flex items-center justify-between px-4 select-none text-sm text-gray-800 dark:text-gray-200 shadow-sm">
    <!-- 左侧区域 -->
    <div class="flex items-center gap-4 h-full">
      <button @click="toggleSidebar" class="hover:bg-black/5 dark:hover:bg-white/10 p-1 rounded transition-colors" title="Toggle Sidebar">
        <i class="ph ph-sidebar text-lg"></i>
      </button>
      
      <div class="flex items-center gap-3">
        <button @click="avatarClick" class="relative group cursor-pointer" title="Manage Site">
          <img :src="avatarUrl" class="w-5 h-5 rounded-full border border-gray-300 dark:border-gray-600 group-hover:ring-2 ring-blue-500 transition-all" alt="Admin" />
          <span class="absolute top-0 right-0 w-1.5 h-1.5 bg-green-500 border border-white dark:border-black rounded-full"></span>
        </button>
        <span class="font-bold tracking-tight">KK Blog</span>
      </div>
      
      <!-- 装饰性菜单 -->
      <div class="hidden sm:flex gap-4 text-[13px] font-medium opacity-80 pl-2">
        <span class="hover:opacity-100 cursor-default">File</span>
        <span class="hover:opacity-100 cursor-default">Edit</span>
        <span class="hover:opacity-100 cursor-default">View</span>
      </div>
    </div>

    <!-- 右侧：主题切换与时间 -->
    <div class="flex items-center gap-4">
      <!-- 主题切换按钮 -->
      <button 
        @click="toggleTheme" 
        class="flex items-center justify-center p-1 rounded hover:bg-black/5 dark:hover:bg-white/10 transition-colors text-gray-600 dark:text-gray-300"
        :title="theme === 'light' ? '切换到深色模式' : '切换到浅色模式'"
      >
        <i :class="['ph', theme === 'light' ? 'ph-moon' : 'ph-sun', 'text-lg']"></i>
      </button>

      <div class="flex items-center gap-2 opacity-90 text-xs font-medium font-mono pl-3 border-l border-gray-300 dark:border-gray-700 h-4">
        <span>{{ timeStr }}</span>
      </div>
    </div>
  </header>
</template>

<script setup>
import avatarUrl from '../server/kk-avatar.png';

defineProps({
  timeStr: {
    type: String,
    required: true
  },
  theme: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['toggle-sidebar', 'avatar-click', 'toggle-theme']);

const toggleSidebar = () => {
  emit('toggle-sidebar');
};

const avatarClick = () => {
  emit('avatar-click');
};

const toggleTheme = () => {
  emit('toggle-theme');
};
</script>
