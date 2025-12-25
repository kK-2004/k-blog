<script setup lang="ts">
import { ref } from 'vue'
import { useClock } from '@/composables/useClock'
import { useTheme } from '@/composables/useTheme'
import { INITIAL_POSTS } from '@/data/initialPosts'
import MacNavBar from '@/components/mac/MacNavBar.vue'
import MacSidebar from '@/components/mac/MacSidebar.vue'
import AdminView from '@/views/AdminView.vue'
import BlogView from '@/views/BlogView.vue'
import LoginView from '@/views/LoginView.vue'
import SettingsView from '@/views/SettingsView.vue'

type ViewId = 'blog' | 'login' | 'admin' | 'settings'

const { timeStr } = useClock()
const { theme, toggleTheme, bgStyle } = useTheme()

const currentView = ref<ViewId>('blog')
const isSidebarOpen = ref(false)
const posts = ref(INITIAL_POSTS)

const setView = (view: ViewId) => {
  currentView.value = view
}

const onAvatarClick = () => {
  currentView.value = currentView.value === 'admin' ? 'blog' : 'login'
}
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
        @toggle-theme="toggleTheme"
        @toggle-sidebar="isSidebarOpen = !isSidebarOpen"
        @avatar-click="onAvatarClick"
      />

      <MacSidebar
        :isOpen="isSidebarOpen"
        :currentView="currentView"
        @close="isSidebarOpen = false"
        @update:currentView="(val) => setView(val)"
      />

      <main class="relative z-10 max-w-4xl mx-auto p-6 mt-6 min-h-[80vh]">
        <BlogView v-if="currentView === 'blog'" :posts="posts" />
        <LoginView v-else-if="currentView === 'login'" @login-success="setView('admin')" />
        <AdminView v-else-if="currentView === 'admin'" :posts="posts" />
        <SettingsView v-else />
      </main>
    </div>
  </div>
</template>
