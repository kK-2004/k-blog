<script setup lang="ts">
import { ref } from 'vue'
import { useClock } from '@/composables/useClock'
import { useTheme } from '@/composables/useTheme'
import { INITIAL_POSTS } from '@/data/initialPosts'
import AdminDashboard from '@/components/mac/AdminDashboard.vue'
import AdminLogin from '@/components/mac/AdminLogin.vue'
import MacBlogCard from '@/components/mac/MacBlogCard.vue'
import MacNavBar from '@/components/mac/MacNavBar.vue'
import MacSidebar from '@/components/mac/MacSidebar.vue'

type ViewId = 'blog' | 'login' | 'admin' | 'settings'

const { timeStr } = useClock()
const { theme, toggleTheme, bgStyle } = useTheme()

const currentView = ref<ViewId>('blog')
const isSidebarOpen = ref(false)
const posts = ref(INITIAL_POSTS)

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

      <MacSidebar :isOpen="isSidebarOpen" :currentView="currentView" @close="isSidebarOpen = false" @update:currentView="(val) => (currentView = val)" />

      <main class="relative z-10 max-w-4xl mx-auto p-6 mt-6 min-h-[80vh]">
        <div v-if="currentView === 'blog'" class="max-w-2xl mx-auto animate-[slideIn_0.5s_ease-out]">
          <MacBlogCard v-for="post in posts" :key="post.id" :post="post" />
          <div class="text-center mt-12 mb-8 opacity-50 text-[10px] font-mono">POWERED BY VUE 3</div>
        </div>

        <AdminLogin v-else-if="currentView === 'login'" @login-success="currentView = 'admin'" />
        <AdminDashboard v-else-if="currentView === 'admin'" :posts="posts" />

        <div v-else class="max-w-2xl mx-auto">
          <div class="bg-white/70 dark:bg-[#1e1e1e]/70 backdrop-blur-2xl rounded-2xl shadow-2xl border border-white/20 dark:border-white/10 p-8">
            <h2 class="text-lg font-bold text-gray-800 dark:text-gray-100 mb-2">Settings</h2>
            <p class="text-sm text-gray-500 dark:text-gray-400">
              这里留给你接入真实的配置项（主题、摘要服务、账号等）。
            </p>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>
