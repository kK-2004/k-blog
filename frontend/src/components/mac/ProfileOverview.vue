<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserProfile } from '@/composables/useUserProfile'
import { useUserAvatar } from '@/composables/useUserAvatar'

const { profile, loadProfile, isFieldVisible, isFieldFilled } = useUserProfile()
const { avatarUrl, hasCustomAvatar } = useUserAvatar()

const isHovering = ref(false)

onMounted(async () => {
  await loadProfile()
})

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
  <div class="space-y-6">
    <!-- 用户名片 -->
    <div class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden">
      <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center px-6">
        <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
          <i class="ph ph-user text-lg"></i>
          个人名片
        </h2>
      </div>

      <div class="p-6">
        <div class="flex items-start gap-6">
          <!-- 头像 -->
          <div class="relative group" @mouseenter="isHovering = true" @mouseleave="isHovering = false">
            <div class="w-24 h-24 rounded-full overflow-hidden bg-gray-200 dark:bg-gray-700 border-4 border-white dark:border-gray-600 shadow-lg">
              <img
                v-if="avatarUrl"
                :src="avatarUrl"
                alt="Avatar"
                class="w-full h-full object-cover"
              />
              <div v-else class="w-full h-full flex items-center justify-center text-4xl font-bold text-gray-400">
                {{ profile.username.charAt(0).toUpperCase() }}
              </div>
            </div>
            <div
              v-if="isHovering && hasCustomAvatar"
              class="absolute inset-0 bg-black/50 flex items-center justify-center rounded-full cursor-pointer"
            >
              <i class="ph ph-camera text-white text-2xl"></i>
            </div>
          </div>

          <!-- 基本信息 -->
          <div class="flex-1 space-y-3">
            <div>
              <h3 class="text-2xl font-bold text-gray-800 dark:text-gray-200">
                {{ profile.username }}
              </h3>
              <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
                欢迎来到我的博客
              </p>
            </div>

            <!-- 可见信息标签 -->
            <div class="flex flex-wrap gap-2">
              <!-- 性别 -->
              <span
                v-if="isFieldFilled('gender') && isFieldVisible('gender')"
                class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-800"
              >
                <i :class="['ph', getGenderIcon(profile.gender), 'text-sm']"></i>
                {{ getGenderText(profile.gender) }}
              </span>

              <!-- 年龄 -->
              <span
                v-if="isFieldFilled('age') && isFieldVisible('age')"
                class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400 border border-green-200 dark:border-green-800"
              >
                <i class="ph ph-calendar text-sm"></i>
                {{ profile.age }} 岁
              </span>

              <!-- 邮箱 -->
              <span
                v-if="isFieldFilled('email') && isFieldVisible('email')"
                class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium bg-purple-50 dark:bg-purple-900/20 text-purple-600 dark:text-purple-400 border border-purple-200 dark:border-purple-800"
              >
                <i class="ph ph-envelope text-sm"></i>
                {{ profile.email }}
              </span>

              <!-- 手机号 -->
              <span
                v-if="isFieldFilled('phone') && isFieldVisible('phone')"
                class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium bg-orange-50 dark:bg-orange-900/20 text-orange-600 dark:text-orange-400 border border-orange-200 dark:border-orange-800"
              >
                <i class="ph ph-phone text-sm"></i>
                {{ profile.phone }}
              </span>

              <!-- QQ -->
              <span
                v-if="isFieldFilled('qq') && isFieldVisible('qq')"
                class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium bg-cyan-50 dark:bg-cyan-900/20 text-cyan-600 dark:text-cyan-400 border border-cyan-200 dark:border-cyan-800"
              >
                <i class="ph ph-qq-logo text-sm"></i>
                QQ: {{ profile.qq }}
              </span>

              <!-- 微信 -->
              <span
                v-if="isFieldFilled('wechat') && isFieldVisible('wechat')"
                class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800"
              >
                <i class="ph ph-wechat-logo text-sm"></i>
                {{ profile.wechat }}
              </span>
            </div>

            <!-- 社交链接 -->
            <div v-if="isFieldFilled('github') || isFieldFilled('gitee')" class="flex gap-2">
              <!-- GitHub -->
              <a
                v-if="isFieldFilled('github') && isFieldVisible('github')"
                :href="`https://github.com/${profile.github}`"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium bg-gray-800 dark:bg-gray-700 text-white hover:bg-gray-700 dark:hover:bg-gray-600 transition-colors border border-gray-700 dark:border-gray-600"
              >
                <i class="ph ph-github-logo text-sm"></i>
                GitHub
              </a>

              <!-- Gitee -->
              <a
                v-if="isFieldFilled('gitee') && isFieldVisible('gitee')"
                :href="`https://gitee.com/${profile.gitee}`"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium bg-red-600 text-white hover:bg-red-700 transition-colors border border-red-700"
              >
                <i class="ph ph-git-branch text-sm"></i>
                Gitee
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 字段完成度 -->
    <div class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden">
      <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center px-6">
        <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
          <i class="ph ph-chart-bar text-lg"></i>
          信息完整度
        </h2>
      </div>

      <div class="p-6 space-y-4">
        <div class="flex items-center justify-between text-sm">
          <span class="text-gray-600 dark:text-gray-400">已填写字段</span>
          <span class="font-medium text-gray-800 dark:text-gray-200">
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
            }}
            / 8
          </span>
        </div>

        <!-- 进度条 -->
        <div class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
          <div
            class="h-full bg-gradient-to-r from-blue-500 to-blue-600 dark:from-blue-400 dark:to-blue-500 transition-all duration-500"
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

        <p class="text-xs text-gray-500 dark:text-gray-400">
          完善个人信息可以让他人更好地了解你
        </p>
      </div>
    </div>

    <!-- 快速操作 -->
    <div class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden">
      <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center px-6">
        <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
          <i class="ph ph-lightning text-lg"></i>
          快速操作
        </h2>
      </div>

      <div class="p-6 grid grid-cols-2 gap-3">
        <button
          class="flex items-center gap-3 px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
          @click="$emit('edit-avatar')"
        >
          <div class="w-10 h-10 rounded-lg bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center">
            <i class="ph ph-image text-blue-500 dark:text-blue-400 text-xl"></i>
          </div>
          <div>
            <div class="text-sm font-medium text-gray-800 dark:text-gray-200">设置头像</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">上传你的头像</div>
          </div>
        </button>

        <button
          class="flex items-center gap-3 px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
          @click="$emit('edit-basic')"
        >
          <div class="w-10 h-10 rounded-lg bg-green-50 dark:bg-green-900/20 flex items-center justify-center">
            <i class="ph ph-identification-card text-green-500 dark:text-green-400 text-xl"></i>
          </div>
          <div>
            <div class="text-sm font-medium text-gray-800 dark:text-gray-200">基本信息</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">昵称、性别、年龄</div>
          </div>
        </button>

        <button
          class="flex items-center gap-3 px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
          @click="$emit('edit-contact')"
        >
          <div class="w-10 h-10 rounded-lg bg-purple-50 dark:bg-purple-900/20 flex items-center justify-center">
            <i class="ph ph-address-book text-purple-500 dark:text-purple-400 text-xl"></i>
          </div>
          <div>
            <div class="text-sm font-medium text-gray-800 dark:text-gray-200">联系方式</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">邮箱、手机、QQ</div>
          </div>
        </button>

        <button
          class="flex items-center gap-3 px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
          @click="$emit('edit-social')"
        >
          <div class="w-10 h-10 rounded-lg bg-orange-50 dark:bg-orange-900/20 flex items-center justify-center">
            <i class="ph ph-link text-orange-500 dark:text-orange-400 text-xl"></i>
          </div>
          <div>
            <div class="text-sm font-medium text-gray-800 dark:text-gray-200">社交链接</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">GitHub、Gitee</div>
          </div>
        </button>
      </div>
    </div>
  </div>
</template>
