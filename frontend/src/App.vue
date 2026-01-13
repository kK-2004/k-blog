<script setup lang="ts">
import { ref, watch } from 'vue'
import { useClock } from '@/composables/useClock'
import { useTheme } from '@/composables/useTheme'
import { useHashRouter } from '@/composables/useHashRouter'
import { INITIAL_POSTS } from '@/data/initialPosts'
import MacNavBar from '@/components/mac/MacNavBar.vue'
import MacSidebar from '@/components/mac/MacSidebar.vue'
import AdminView from '@/views/AdminView.vue'
import BlogView from '@/views/BlogView.vue'
import LoginView from '@/views/LoginView.vue'
import SettingsView from '@/views/SettingsView.vue'

const { timeStr } = useClock()
const { theme, toggleTheme, bgStyle } = useTheme()
const { currentView, navigateTo } = useHashRouter()

const isSidebarOpen = ref(false)
const posts = ref(INITIAL_POSTS)
const isAuthenticated = ref(false)

// 登录成功处理
const onLoginSuccess = () => {
  isAuthenticated.value = true
  navigateTo('admin')
}

// 退出登录
const logout = () => {
  isAuthenticated.value = false
  navigateTo('blog')
}

// 点击头像：始终跳转到登录页（如果已登录则显示菜单）
const onAvatarClick = () => {
  navigateTo('login')
}

// 路由守卫：未登录不能访问 admin 和 settings
watch(currentView, (newView) => {
  if ((newView === 'admin' || newView === 'settings') && !isAuthenticated.value) {
    navigateTo('login')
  }
})
</script>

<template>
  <div :class="theme" class="transition-colors duration-500 ease-in-out h-full">
    <div
      class="min-h-screen bg-[#f5f5f7] dark:bg-black font-sans text-[#1d1d1f] dark:text-[#f5f5f7] pt-8 transition-colors duration-500 overflow-x-hidden relative"
      :style="bgStyle"
    >
      <div class="absolute inset-0 bg-white/30 dark:bg-black/40 pointer-events-none"></div>

      <MacNavBar
        :timeStr="timeStr"
        :theme="theme"
        :isAuthenticated="isAuthenticated"
        @toggle-theme="toggleTheme"
        @toggle-sidebar="isSidebarOpen = !isSidebarOpen"
        @avatar-click="onAvatarClick"
        @logout="logout"
      />

      <MacSidebar
        :isOpen="isSidebarOpen"
        :currentView="currentView"
        :isAuthenticated="isAuthenticated"
      />

      <main
        class="relative z-10 max-w-4xl mx-auto p-6 mt-6 min-h-[80vh] transition-transform duration-300 ease-out"
        :class="[
          isSidebarOpen ? 'translate-x-32' : 'translate-x-0'
        ]"
      >
        <BlogView v-if="currentView === 'blog'" :posts="posts" />
        <LoginView v-else-if="currentView === 'login'" @login-success="onLoginSuccess" />
        <AdminView v-else-if="currentView === 'admin'" :posts="posts" />
        <SettingsView v-else-if="currentView === 'settings'" />
      </main>
    </div>
  </div>
</template>
