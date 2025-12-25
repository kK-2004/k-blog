<template>
  <div :class="theme" class="transition-colors duration-500 ease-in-out h-full">
    <div 
      class="app-dots-bg min-h-screen font-sans text-[#1d1d1f] dark:text-[#f5f5f7] pt-8 transition-colors duration-500 overflow-x-hidden relative"
    >
      <!-- 背景遮罩 -->
      <div class="absolute inset-0 bg-white/30 dark:bg-black/40 pointer-events-none"></div>

      <MacNavBar 
        :timeStr="timeStr"
        :theme="theme"
        @toggle-theme="toggleTheme"
        @toggle-sidebar="toggleSidebar"
        @avatar-click="handleAvatarClick"
      />

      <Sidebar 
        :isOpen="isSidebarOpen" 
        :currentView="currentView"
        @close="isSidebarOpen = false"
        @update:currentView="updateCurrentView"
      />

      <!-- 主视图区域 -->
      <main class="relative z-10 max-w-4xl mx-auto p-6 mt-6 min-h-[80vh]">
        <!-- 博客列表 -->
        <div v-if="currentView === 'blog'" class="max-w-2xl mx-auto animate-[slideIn_0.5s_ease-out]">
          <MacBlogCard v-for="post in posts" :key="post.id" :post="post" />
          <div class="text-center mt-12 mb-8">
            <p class="text-[10px] font-mono opacity-50">DESIGNED BY KK BLOG (VUE 3)</p>
          </div>
        </div>

        <!-- 登录页 -->
        <AdminLogin v-if="currentView === 'login'" @login-success="currentView = 'admin'" />

        <!-- 后台 -->
        <AdminDashboard v-if="currentView === 'admin'" :posts="posts" />

        <!-- 设置页 -->
        <div v-if="currentView === 'settings'" class="flex flex-col items-center justify-center h-64 bg-white/60 dark:bg-black/60 backdrop-blur-xl rounded-xl border border-white/20">
          <i class="ph ph-gear text-4xl mb-2 opacity-50"></i>
          <p class="mt-4 text-sm font-medium opacity-60">System Preferences Locked</p>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useTheme } from './composables/useTheme';
import { useTime } from './composables/useTime';
import { usePostsStore } from './stores/usePostsStore';
import MacBlogCard from './components/MacBlogCard.vue';
import AdminDashboard from './components/AdminDashboard.vue';
import AdminLogin from './views/AdminLogin.vue';
import MacNavBar from './components/MacNavBar.vue';
import Sidebar from './components/Sidebar.vue';

const { theme, toggleTheme } = useTheme();
const { timeStr } = useTime();
const { posts } = usePostsStore();
const currentView = ref('blog');
const isSidebarOpen = ref(false);

// 路由跳转
const handleAvatarClick = () => {
  if (currentView.value === 'admin') {
    currentView.value = 'blog';
  } else {
    currentView.value = 'login';
  }
};

const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value;
};

const updateCurrentView = (viewId: string) => {
  currentView.value = viewId;
};
</script>
