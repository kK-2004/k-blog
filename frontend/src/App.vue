<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useClock } from '@/composables/useClock'
import { useTheme } from '@/composables/useTheme'
import { useHashRouter } from '@/composables/useHashRouter'
import { listPosts } from '@/api/posts'
import { logout as apiLogout, me as apiMe } from '@/api/admin'
import { getPublicProfile, type PublicProfile } from '@/api/site'
import type { AdminMe, Post } from '@/api/types'
import MacNavBar from '@/components/mac/MacNavBar.vue'
import MacSidebar from '@/components/mac/MacSidebar.vue'
import ProfileOverview from '@/components/mac/ProfileOverview.vue'
import HotPostsList from '@/components/mac/HotPostsList.vue'
import MobileSideDrawer from '@/components/mac/MobileSideDrawer.vue'
import AdminView from '@/views/AdminView.vue'
import BlogView from '@/views/BlogView.vue'
import LoginView from '@/views/LoginView.vue'
import SettingsView from '@/views/SettingsView.vue'
import ArticleDetailView from '@/views/ArticleDetailView.vue'

const { timeStr } = useClock()
const { theme, toggleTheme, bgStyle } = useTheme()
const { currentView, navigateTo, articleId } = useHashRouter()

const isSidebarOpen = ref(false)
const mainRef = ref<HTMLElement | null>(null)
const mainRefMobile = ref<HTMLElement | null>(null)
const posts = ref<Post[]>([])
const isAuthenticated = ref(false)
const adminMe = ref<AdminMe | null>(null)
const authReady = ref(false)
// 存储完整的公开 profile 数据，避免子组件重复请求
const publicProfile = ref<PublicProfile | null>(null)
const authorAvatarUrl = ref<string | null>(null)
// 移动端抽屉状态
const isMobileDrawerOpen = ref(false)
const drawerTab = ref<'profile' | 'hotposts'>('profile')

const refreshPosts = async () => {
  posts.value = await listPosts()
}

const loadPublicProfile = async () => {
  try {
    const profile = await getPublicProfile()
    publicProfile.value = profile
    authorAvatarUrl.value = profile.avatarUrl
  } catch {
    publicProfile.value = null
    authorAvatarUrl.value = null
  }
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

// 点击头像：已登录跳转到 admin，未登录跳转到 login
const onAvatarClick = () => {
  if (isAuthenticated.value) {
    navigateTo('admin')
  } else {
    navigateTo('login')
  }
}

// 移动端抽屉方法
const toggleMobileDrawer = () => {
  isMobileDrawerOpen.value = !isMobileDrawerOpen.value
}

const closeMobileDrawer = () => {
  isMobileDrawerOpen.value = false
}

const switchDrawerTab = (tab: 'profile' | 'hotposts') => {
  drawerTab.value = tab
}

// 路由守卫：未登录不能访问 admin 和 settings
watch(currentView, (newView) => {
  if (!authReady.value) return
  if ((newView === 'admin' || newView === 'settings') && !isAuthenticated.value) {
    navigateTo('login')
  }
  if (newView === 'admin') {
    refreshPosts()
  }
})

onMounted(async () => {
  await Promise.all([refreshAuth(), loadPublicProfile()])
})
</script>

<template>
  <div :class="theme" class="transition-colors duration-500 ease-in-out h-full">
    <div
      class="h-screen bg-[#f5f5f7] dark:bg-black font-sans text-[#1d1d1f] dark:text-[#f5f5f7] pt-8 transition-colors duration-500 overflow-hidden relative"
      :style="bgStyle"
    >
      <MacNavBar
        :timeStr="timeStr"
        :theme="theme"
        :isAuthenticated="isAuthenticated"
        @toggle-theme="toggleTheme"
        @toggle-sidebar="isSidebarOpen = !isSidebarOpen"
        @toggle-mobile-drawer="toggleMobileDrawer"
        @avatar-click="onAvatarClick"
        @logout="logout"
      />

      <MacSidebar
        :isOpen="isSidebarOpen"
        :currentView="currentView"
        :isAuthenticated="isAuthenticated"
      />

      <!-- 桌面端固定侧边栏：左侧 Profile -->
      <aside v-show="currentView === 'blog'" class="hidden lg:block fixed left-12 top-12 z-20 w-[360px] h-[calc(100vh-2rem)] pointer-events-none">
        <div class="sticky top-0 p-4 pointer-events-auto">
          <ProfileOverview
            :show-full-features="false"
            :is-authenticated="isAuthenticated"
            :public-profile="publicProfile"
          />
        </div>
      </aside>

      <!-- 桌面端固定侧边栏：右侧 HotPosts -->
      <aside v-show="currentView === 'blog'" class="hidden lg:block fixed right-12 top-12 z-20 w-[280px] h-[calc(100vh-2rem)] pointer-events-none">
        <div class="sticky top-0 p-4 pointer-events-auto">
          <HotPostsList />
        </div>
      </aside>

      <!-- 主内容区域 -->
      <!-- 外层容器：可滚动，侧边栏区域 pointer-events-none -->
      <div
        ref="mainRef"
        class="hidden lg:block fixed left-0 right-0 top-8 h-[calc(100vh-2rem)] overflow-y-auto scrollbar-hide"
      >
        <div class="mx-auto max-w-[1600px] px-4 flex justify-center min-h-full">
          <!-- 左侧预留空间（不拦截事件） -->
          <div class="w-[240px] shrink-0 pointer-events-none"></div>
          <!-- 中间内容 -->
          <div class="flex-1 max-w-5xl px-4">
            <main
              class="transition-transform duration-300 ease-out relative z-10 py-4"
              :class="[
                // 侧边栏位移
                isSidebarOpen ? 'translate-x-0' : 'translate-x-0'
              ]"
            >
              <BlogView
                v-if="currentView === 'blog'"
                :isAuthenticated="isAuthenticated"
                :adminMe="adminMe"
                :authorAvatarUrl="authorAvatarUrl"
                :scrollContainer="mainRef"
              />
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
          <!-- 右侧预留空间（不拦截事件） -->
          <div class="w-[280px] shrink-0 pointer-events-none"></div>
        </div>
      </div>

      <!-- 移动端主内容区域 -->
      <main
        ref="mainRefMobile"
        class="lg:hidden scrollbar-hide transition-transform duration-300 ease-out relative z-10 h-[calc(100vh-8rem)] overflow-y-auto px-4 py-6"
        :class="[
          isSidebarOpen ? 'translate-x-64' : 'translate-x-0'
        ]"
      >
        <BlogView
          v-if="currentView === 'blog'"
          :isAuthenticated="isAuthenticated"
          :adminMe="adminMe"
          :authorAvatarUrl="authorAvatarUrl"
          :scrollContainer="mainRefMobile"
        />
        <LoginView v-else-if="currentView === 'login'" @login-success="onLoginSuccess" />
        <AdminView
          v-else-if="currentView === 'admin'"
          :posts="posts"
          @update:posts="setPosts"
          @refresh="refreshPosts"
        />
        <SettingsView v-else-if="currentView === 'settings'" />
      </main>

      <!-- 移动端抽屉 -->
      <MobileSideDrawer
        :is-open="isMobileDrawerOpen"
        :active-tab="drawerTab"
        :is-authenticated="isAuthenticated"
        :public-profile="publicProfile"
        :current-view="currentView"
        @close="closeMobileDrawer"
        @switch-tab="switchDrawerTab"
      />

      <!-- ArticleDetailView 单独渲染，不受 main 容器宽度限制 -->
      <ArticleDetailView
        v-if="currentView === 'article'"
        :postId="articleId!"
        :isAuthenticated="isAuthenticated"
        :adminMe="adminMe"
        :authorAvatarUrl="authorAvatarUrl"
        :isSidebarOpen="isSidebarOpen"
      />
    </div>
  </div>
</template>
