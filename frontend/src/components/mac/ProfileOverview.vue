<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useUserProfile } from '@/composables/useUserProfile'
import { usePublicProfile } from '@/composables/usePublicProfile'
import { useUserAvatar } from '@/composables/useUserAvatar'
import { listVisibleBlogQuickActions } from '@/api/site'
import type { QuickAction } from '@/api/types'
import type { PublicProfile } from '@/api/site'


const props = defineProps<{
  showFullFeatures?: boolean
  isAuthenticated?: boolean
  /** 外部传入的公开资料，如果提供则不重新请求 */
  publicProfile?: PublicProfile | null
}>()

// 根据登录状态选择使用哪个 composable
const userProfile = useUserProfile()
const publicProfileComposable = usePublicProfile()

// 动态选择 profile 数据源
// 如果外部传入了 publicProfile，使用外部传入的；否则使用 composable 的数据
const profile = computed(() => {
  if (props.isAuthenticated) {
    return userProfile.profile.value
  }
  // 未登录时，优先使用外部传入的 publicProfile，否则使用 composable 的数据
  return props.publicProfile || publicProfileComposable.profile.value
})

// 动态选择加载方法
// 如果外部已传入 publicProfile 且未登录，则无需重新加载
const loadProfile = async () => {
  if (props.isAuthenticated) {
    return await userProfile.loadProfile()
  } else {
    // 如果外部已经传入了 publicProfile，不需要重新加载
    if (props.publicProfile) {
      return true
    }
    return await publicProfileComposable.loadProfile()
  }
}

// 动态选择可见性判断方法（公开资料模式下所有字段都可见）
const isFieldVisible = (field: string) => {
  if (props.isAuthenticated) {
    return userProfile.isFieldVisible(field as any)
  }
  return true
}

// 动态选择填充判断方法
const isFieldFilled = (field: string) => {
  if (props.isAuthenticated) {
    return userProfile.isFieldFilled(field as any)
  }
  // 未登录时，优先使用外部传入的 publicProfile
  const profileData = props.publicProfile || publicProfileComposable.profile.value
  if (!profileData) return false
  const value = profileData[field as keyof PublicProfile]
  return value !== null && value !== undefined && value !== ''
}

// 登录用户使用 useUserAvatar，未登录用户从 publicProfile 获取头像
const { avatarUrl: userAvatarUrl, hasCustomAvatar } = useUserAvatar()
const avatarUrl = computed(() => {
  if (props.isAuthenticated) {
    return userAvatarUrl.value
  }
  // 未登录时，优先使用外部传入的 publicProfile 的头像
  return (props.publicProfile || publicProfileComposable.profile.value)?.avatarUrl || null
})

const isHovering = ref(false)
const blogQuickActions = ref<QuickAction[]>([])

onMounted(async () => {
  await loadProfile()
  if (props.showFullFeatures === false) {
    try {
      blogQuickActions.value = await listVisibleBlogQuickActions()
    } catch {
      blogQuickActions.value = []
    }
  }
})

const openBlogQuickAction = (item: QuickAction) => {
  if (item.targetType === 'internal') {
    const hash = item.target.startsWith('#') ? item.target : `#${item.target}`
    window.location.hash = hash
    return
  }
  window.open(item.target, '_blank', 'noopener,noreferrer')
}

const getGenderText = (gender: string | null) => {
  const map: Record<string, string> = {
    male: '男',
    female: '女',
    other: '其他',
    secret: '保密',
  }
  return gender ? (map[gender] || '未设置') : '未设置'
}

const getGenderIcon = (gender: string | null) => {
  const map: Record<string, string> = {
    male: 'ph-gender-male',
    female: 'ph-gender-female',
    other: 'ph-gender-intersex',
    secret: 'ph-lock',
  }
  return gender ? (map[gender] || 'ph-question') : 'ph-question'
}
</script>

<template>
  <div class="space-y-4">
    <!-- 用户卡片 - 简洁设计 -->
    <div class="bg-white/90 dark:bg-[#1e1e1e]/90 backdrop-blur-xl rounded-2xl shadow-lg border border-gray-200/50 dark:border-white/5 overflow-hidden">
      <!-- 头像和基本信息 -->
      <div class="p-6 text-center">
        <!-- 头像 -->
        <div
            class="relative inline-block mb-4"
            @mouseenter="showFullFeatures ? isHovering = true : null"
            @mouseleave="showFullFeatures ? isHovering = false : null"
        >
          <div class="w-20 h-20 rounded-full overflow-hidden bg-gray-200 dark:bg-gray-700 shadow-lg">
            <img
                v-if="avatarUrl"
                :src="avatarUrl"
                alt="Avatar"
                class="w-full h-full object-cover"
                draggable="false"
            />
            <div v-else class="w-full h-full flex items-center justify-center text-3xl font-bold text-gray-400">
              {{ profile.username.charAt(0).toUpperCase() }}
            </div>
          </div>
          <div
              v-if="showFullFeatures && isHovering && hasCustomAvatar"
              class="absolute inset-0 bg-black/60 flex items-center justify-center rounded-full cursor-pointer backdrop-blur-sm"
          >
            <i class="ph ph-camera text-white text-xl"></i>
          </div>
        </div>

        <!-- 用户名 -->
        <h3 class="text-xl font-bold text-gray-800 dark:text-gray-100 mb-1">
          {{ profile.username }}
        </h3>
        <p class="text-xs text-gray-500 dark:text-gray-400">
          欢迎来到我的博客
        </p>
      </div>

      <!-- 信息标签 - 紧凑布局 -->
      <div class="px-4 pb-4 space-y-2">
        <!-- 性别 -->
        <div
            v-if="isFieldFilled('gender') && isFieldVisible('gender')"
            class="flex items-center gap-2 px-3 py-2 rounded-lg bg-gray-50 dark:bg-white/5 hover:bg-gray-100 dark:hover:bg-white/10 transition-colors"
        >
          <i :class="['ph', getGenderIcon(profile.gender), 'text-blue-500 dark:text-blue-400']"></i>
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ getGenderText(profile.gender) }}</span>
        </div>

        <!-- 年龄 -->
        <div
            v-if="isFieldFilled('age') && isFieldVisible('age')"
            class="flex items-center gap-2 px-3 py-2 rounded-lg bg-gray-50 dark:bg-white/5 hover:bg-gray-100 dark:hover:bg-white/10 transition-colors"
        >
          <i class="ph ph-calendar text-green-500 dark:text-green-400"></i>
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ profile.age }} 岁</span>
        </div>

        <!-- QQ -->
        <div
            v-if="isFieldFilled('qq') && isFieldVisible('qq')"
            class="flex items-center gap-2 px-3 py-2 rounded-lg bg-gray-50 dark:bg-white/5 hover:bg-gray-100 dark:hover:bg-white/10 transition-colors"
        >
          <i class="fa-brands fa-qq text-cyan-500 dark:text-cyan-400"></i>
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ profile.qq }}</span>
        </div>

        <!-- 微信 -->
        <div
            v-if="isFieldFilled('wechat') && isFieldVisible('wechat')"
            class="flex items-center gap-2 px-3 py-2 rounded-lg bg-gray-50 dark:bg-white/5 hover:bg-gray-100 dark:hover:bg-white/10 transition-colors"
        >
          <i class="ph ph-wechat-logo text-emerald-500 dark:text-emerald-400"></i>
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ profile.wechat }}</span>
        </div>

        <!-- 邮箱 -->
        <div
            v-if="isFieldFilled('email') && isFieldVisible('email')"
            class="flex items-center gap-2 px-3 py-2 rounded-lg bg-gray-50 dark:bg-white/5 hover:bg-gray-100 dark:hover:bg-white/10 transition-colors"
        >
          <i class="ph ph-envelope text-purple-500 dark:text-purple-400"></i>
          <span class="text-sm text-gray-700 dark:text-gray-300 truncate">{{ profile.email }}</span>
        </div>

        <!-- 手机号 -->
        <div
            v-if="isFieldFilled('phone') && isFieldVisible('phone')"
            class="flex items-center gap-2 px-3 py-2 rounded-lg bg-gray-50 dark:bg-white/5 hover:bg-gray-100 dark:hover:bg-white/10 transition-colors"
        >
          <i class="ph ph-phone text-orange-500 dark:text-orange-400"></i>
          <span class="text-sm text-gray-500 dark:text-gray-400">手机:</span>
          <span class="text-sm text-gray-700 dark:text-gray-300">{{ profile.phone }}</span>
        </div>
      </div>

      <!-- 社交链接 -->
      <div
          v-if="isFieldFilled('github') || isFieldFilled('gitee')"
          class="px-4 pb-4 flex gap-2"
      >
        <a
            v-if="isFieldFilled('github') && isFieldVisible('github')"
            :href="`https://github.com/${profile.github}`"
            target="_blank"
            rel="noopener noreferrer"
            class="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg text-xs font-medium bg-[#24292e] hover:bg-[#1c2127] text-white transition-all shadow-sm active:scale-95"
        >
          <i class="ph ph-github-logo"></i>
          <span>GitHub</span>
        </a>

        <a
            v-if="isFieldFilled('gitee') && isFieldVisible('gitee')"
            :href="`https://gitee.com/${profile.gitee}`"
            target="_blank"
            rel="noopener noreferrer"
            class="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg text-xs font-medium bg-[#c71d23] hover:bg-[#a91920] text-white transition-all shadow-sm active:scale-95"
        >
          <i class="ph ph-git-branch"></i>
          <span>Gitee</span>
        </a>
      </div>
    </div>

    <!-- 信息完整度卡片 -->
    <div
        v-if="showFullFeatures"
        class="bg-white/90 dark:bg-[#1e1e1e]/90 backdrop-blur-xl rounded-2xl shadow-lg border border-gray-200/50 dark:border-white/5 p-4"
    >
      <div class="flex items-center gap-2 mb-3">
        <i class="ph ph-chart-bar text-gray-400"></i>
        <h3 class="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide">信息完整度</h3>
      </div>

      <div class="space-y-2">
        <div class="flex items-center justify-between text-sm">
          <span class="text-gray-600 dark:text-gray-400">已填写</span>
          <span class="font-bold text-gray-800 dark:text-gray-200">
            {{
              [
                profile.gender,
                profile.age,
                profile.email,
                profile.phone,
                profile.qq,
                profile.wechat,
                profile.github,
                profile.gitee,
              ].filter(Boolean).length
            }} / 8
          </span>
        </div>

        <div class="w-full h-2 bg-gray-200 dark:bg-gray-700/50 rounded-full overflow-hidden">
          <div
              class="h-full bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500 transition-all duration-500 rounded-full"
              :style="{
              width: `${
                [
                  profile.gender,
                  profile.age,
                  profile.email,
                  profile.phone,
                  profile.qq,
                  profile.wechat,
                  profile.github,
                  profile.gitee,
                ].filter(Boolean).length * 12.5
              }%`
            }"
          ></div>
        </div>
      </div>
    </div>

    <!-- 快速操作卡片 -->
    <div
        v-if="showFullFeatures"
        class="bg-white/90 dark:bg-[#1e1e1e]/90 backdrop-blur-xl rounded-2xl shadow-lg border border-gray-200/50 dark:border-white/5 p-4"
    >
      <div class="flex items-center gap-2 mb-3">
        <i class="ph ph-lightning text-gray-400"></i>
        <h3 class="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide">快速操作</h3>
      </div>

      <div class="grid grid-cols-2 gap-2">
        <button
            class="flex flex-col items-center gap-2 p-3 rounded-lg border border-gray-200 dark:border-white/10 hover:bg-gray-50 dark:hover:bg-white/5 transition-all active:scale-95"
            @click="$emit('edit-avatar')"
        >
          <i class="ph ph-image text-blue-500 text-xl"></i>
          <span class="text-xs text-gray-700 dark:text-gray-300">头像</span>
        </button>

        <button
            class="flex flex-col items-center gap-2 p-3 rounded-lg border border-gray-200 dark:border-white/10 hover:bg-gray-50 dark:hover:bg-white/5 transition-all active:scale-95"
            @click="$emit('edit-basic')"
        >
          <i class="ph ph-identification-card text-green-500 text-xl"></i>
          <span class="text-xs text-gray-700 dark:text-gray-300">基本</span>
        </button>

        <button
            class="flex flex-col items-center gap-2 p-3 rounded-lg border border-gray-200 dark:border-white/10 hover:bg-gray-50 dark:hover:bg-white/5 transition-all active:scale-95"
            @click="$emit('edit-contact')"
        >
          <i class="ph ph-address-book text-purple-500 text-xl"></i>
          <span class="text-xs text-gray-700 dark:text-gray-300">联系</span>
        </button>

        <button
            class="flex flex-col items-center gap-2 p-3 rounded-lg border border-gray-200 dark:border-white/10 hover:bg-gray-50 dark:hover:bg-white/5 transition-all active:scale-95"
            @click="$emit('edit-social')"
        >
          <i class="ph ph-link text-orange-500 text-xl"></i>
          <span class="text-xs text-gray-700 dark:text-gray-300">社交</span>
        </button>
      </div>
    </div>

    <!-- Blog 个人栏快捷入口 -->
    <div
      v-else-if="blogQuickActions.length"
      class="bg-white/90 dark:bg-[#1e1e1e]/90 backdrop-blur-xl rounded-2xl shadow-lg border border-gray-200/50 dark:border-white/5 p-4"
    >
      <div class="flex items-center gap-2 mb-3">
        <i class="ph ph-lightning text-gray-400"></i>
        <h3 class="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide">快速操作</h3>
      </div>

      <div class="grid grid-cols-1 gap-2">
        <button
          v-for="item in blogQuickActions"
          :key="item.id"
          class="flex items-center gap-3 px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 hover:bg-gray-50 dark:hover:bg-white/5 transition-all active:scale-[0.99] text-left"
          @click="openBlogQuickAction(item)"
        >
          <div class="w-9 h-9 rounded-lg bg-[#007AFF]/10 flex items-center justify-center flex-shrink-0">
            <i :class="['ph', item.icon, 'text-[#007AFF]', 'text-lg']"></i>
          </div>
          <div class="min-w-0">
            <div class="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{{ item.title }}</div>
            <div class="text-xs text-gray-500 dark:text-gray-400 truncate">{{ item.description || '' }}</div>
          </div>
        </button>
      </div>
    </div>
  </div>
</template>
