<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useClock } from '@/composables/useClock'
import { useTheme } from '@/composables/useTheme'
import { useHashRouter } from '@/composables/useHashRouter'
import { listPosts } from '@/api/posts'
import { logout as apiLogout, me as apiMe } from '@/api/admin'
import type { AdminMe, Post } from '@/api/types'
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
const posts = ref<Post[]>([])
const isAuthenticated = ref(false)
const adminMe = ref<AdminMe | null>(null)
const authReady = ref(false)

const refreshPosts = async () => {
  posts.value = await listPosts()
}

const setPosts = (newPosts: Post[]) => {
  posts.value = newPosts
}

const refreshAuth = async () => {
  try {
    adminMe.value = await apiMe()
    isAuthenticated.value = true
  } catch {
    adminMe.value = null
    isAuthenticated.value = false
  } finally {
    authReady.value = true
  }
}

// 登录成功处理
const onLoginSuccess = (me: AdminMe) => {
  adminMe.value = me
  isAuthenticated.value = true
  authReady.value = true
  navigateTo('admin')
}

// 退出登录
const logout = async () => {
  try {
    await apiLogout()
  } finally {
    adminMe.value = null
    isAuthenticated.value = false
    navigateTo('blog')
  }
}

// 点击头像：始终跳转到登录页（如果已登录则显示菜单）
const onAvatarClick = () => {
  navigateTo('login')
}

// 路由守卫：未登录不能访问 admin 和 settings
watch(currentView, (newView) => {
  if (!authReady.value) return
  if ((newView === 'admin' || newView === 'settings') && !isAuthenticated.value) {
    navigateTo('login')
  }
})

onMounted(async () => {
  await Promise.all([refreshPosts(), refreshAuth()])
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
        <AdminView
          v-else-if="currentView === 'admin'"
          :posts="posts"
          @update:posts="setPosts"
          @refresh="refreshPosts"
        />
        <SettingsView v-else-if="currentView === 'settings'" />
      </main>
    </div>
  </div>
</template>
