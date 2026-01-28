<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserProfile } from '@/composables/useUserProfile'
import { useUserAvatar } from '@/composables/useUserAvatar'
import { useMessage } from '@/composables/useMessage'
import { updatePassword } from '@/api/admin'
import type { Gender, FieldVisibility } from '@/types/user'

const props = defineProps<{
  currentTab: string
}>()

const { profile, loadProfile, saveProfile, updateVisibility, isFieldVisible, isFieldFilled } = useUserProfile()
const { avatarUrl, hasCustomAvatar, uploadAvatar, removeAvatar } = useUserAvatar(true)
const { success: showSuccess, error: showError } = useMessage()

// 表单状态
const formData = ref({
  username: '',
  gender: null as Gender | null,
  age: null as number | null,
  email: '',
  phone: '',
  qq: '',
  wechat: '',
  github: '',
  gitee: '',
})

const visibility = ref({
  gender: 'public' as FieldVisibility,
  age: 'public' as FieldVisibility,
  email: 'public' as FieldVisibility,
  phone: 'public' as FieldVisibility,
  qq: 'public' as FieldVisibility,
  wechat: 'public' as FieldVisibility,
  github: 'public' as FieldVisibility,
  gitee: 'public' as FieldVisibility,
})

const isEditing = ref(false)

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const isUpdatingPassword = ref(false)

onMounted(async () => {
  await loadProfile()
  // 初始化表单数据
  resetForm()
})

const resetForm = () => {
  formData.value = {
    username: profile.value.username || '',
    gender: profile.value.gender,
    age: profile.value.age,
    email: profile.value.email || '',
    phone: profile.value.phone || '',
    qq: profile.value.qq || '',
    wechat: profile.value.wechat || '',
    github: profile.value.github || '',
    gitee: profile.value.gitee || '',
  }
  visibility.value = { ...profile.value.visibility }
}

const startEdit = () => {
  isEditing.value = true
  resetForm()
}

const cancelEdit = () => {
  isEditing.value = false
  resetForm()
}

const handleUpdatePassword = async () => {
  if (isUpdatingPassword.value) return
  const oldPassword = passwordForm.value.oldPassword.trim()
  const newPassword = passwordForm.value.newPassword.trim()
  const confirmPassword = passwordForm.value.confirmPassword.trim()

  if (!oldPassword || !newPassword) {
    showError('请输入旧密码和新密码')
    return
  }
  if (newPassword.length < 6) {
    showError('新密码至少 6 位')
    return
  }
  if (newPassword !== confirmPassword) {
    showError('两次输入的新密码不一致')
    return
  }

  isUpdatingPassword.value = true
  try {
    await updatePassword({ oldPassword, newPassword })
    showSuccess('密码修改成功')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (err) {
    showError(err instanceof Error ? err.message : '密码修改失败')
  } finally {
    isUpdatingPassword.value = false
  }
}

const handleSave = async () => {
  try {
    // 验证昵称
    if (!formData.value.username.trim()) {
      showError('昵称不能为空')
      return
    }

    // 检查新填写的字段，自动设置为公开
    // 检查每个字段，如果是新填写的（之前为空，现在有值），则设置为公开
    if (formData.value.gender && !profile.value.gender) {
      visibility.value.gender = 'public'
    }
    if (formData.value.age && !profile.value.age) {
      visibility.value.age = 'public'
    }
    if (formData.value.email && !profile.value.email) {
      visibility.value.email = 'public'
    }
    if (formData.value.phone && !profile.value.phone) {
      visibility.value.phone = 'public'
    }
    if (formData.value.qq && !profile.value.qq) {
      visibility.value.qq = 'public'
    }
    if (formData.value.wechat && !profile.value.wechat) {
      visibility.value.wechat = 'public'
    }
    if (formData.value.github && !profile.value.github) {
      visibility.value.github = 'public'
    }
    if (formData.value.gitee && !profile.value.gitee) {
      visibility.value.gitee = 'public'
    }

    // 保存基本信息
    await saveProfile({
      username: formData.value.username,
      gender: formData.value.gender,
      age: formData.value.age,
      email: formData.value.email || null,
      phone: formData.value.phone || null,
      qq: formData.value.qq || null,
      wechat: formData.value.wechat || null,
      github: formData.value.github || null,
      gitee: formData.value.gitee || null,
      visibility: visibility.value,
    })

    showSuccess('保存成功！')
    isEditing.value = false
  } catch (err) {
    showError(err instanceof Error ? err.message : '保存失败')
  }
}

const toggleVisibility = async (field: keyof typeof visibility.value) => {
  const newVisibility = visibility.value[field] === 'public' ? 'private' : 'public'
  visibility.value[field] = newVisibility
  await updateVisibility(field, newVisibility)
}

// 性别选项
const genderOptions = [
  { value: 'male' as Gender, label: '男', icon: 'ph-gender-male' },
  { value: 'female' as Gender, label: '女', icon: 'ph-gender-female' },
  { value: 'other' as Gender, label: '其他', icon: 'ph-gender-intersex' },
  { value: 'secret' as Gender, label: '保密', icon: 'ph-lock' },
]
</script>

<template>
  <div class="space-y-6">
    <!-- 修改密码 -->
    <div v-if="currentTab === 'password'" class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden">
      <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between px-6">
        <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
          <i class="ph ph-lock-key text-lg"></i>
          修改密码
        </h2>
      </div>

      <div class="p-6 space-y-5">
        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-key text-lg"></i>
            旧密码
          </label>
          <input
            v-model="passwordForm.oldPassword"
            type="password"
            autocomplete="current-password"
            class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#2a2a2a] text-gray-800 dark:text-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
            placeholder="请输入旧密码"
          />
        </div>

        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-lock text-lg"></i>
            新密码
          </label>
          <input
            v-model="passwordForm.newPassword"
            type="password"
            autocomplete="new-password"
            class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#2a2a2a] text-gray-800 dark:text-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
            placeholder="请输入新密码（至少 6 位）"
          />
        </div>

        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-lock-simple text-lg"></i>
            确认新密码
          </label>
          <input
            v-model="passwordForm.confirmPassword"
            type="password"
            autocomplete="new-password"
            class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#2a2a2a] text-gray-800 dark:text-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
            placeholder="请再次输入新密码"
            @keyup.enter="handleUpdatePassword"
          />
        </div>

        <div class="pt-2 border-t border-gray-200 dark:border-gray-700">
          <button
            class="w-full px-4 py-2 bg-blue-500 hover:bg-blue-600 disabled:bg-blue-400/60 text-white text-sm font-medium rounded-lg transition-colors"
            :disabled="isUpdatingPassword"
            @click="handleUpdatePassword"
          >
            <i class="ph ph-check mr-1"></i>
            {{ isUpdatingPassword ? '提交中...' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 头像设置 -->
    <div v-if="currentTab === 'avatar'" class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden">
      <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between px-6">
        <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
          <i class="ph ph-image text-lg"></i>
          头像设置
        </h2>
      </div>

      <div class="p-6">
        <div class="flex items-start gap-6">
          <!-- 头像预览 -->
          <div class="flex-shrink-0">
            <div class="w-20 h-20 rounded-full overflow-hidden bg-gray-200 dark:bg-gray-700 border-4 border-white dark:border-gray-600 shadow-lg">
              <img v-if="avatarUrl" :src="avatarUrl" alt="Avatar" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full flex items-center justify-center text-3xl font-bold text-gray-400">
                {{ formData.username.charAt(0).toUpperCase() }}
              </div>
            </div>
          </div>

          <!-- 上传区域 -->
          <div class="flex-1 space-y-4">
            <p class="text-sm text-gray-600 dark:text-gray-300">
              上传自定义头像，支持 JPG、PNG、GIF 格式，最大 2MB
            </p>
            <div class="flex gap-2">
              <label class="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium rounded-lg transition-colors shadow-sm flex items-center gap-2 cursor-pointer">
                <i class="ph ph-upload-simple"></i>
                选择图片
                <input type="file" accept="image/*" class="hidden" @change="async (e) => {
                  const file = (e.target as HTMLInputElement).files?.[0]
                  if (file) await uploadAvatar(file)
                }" />
              </label>
              <button
                  v-if="hasCustomAvatar"
                  class="px-4 py-2 bg-red-500 hover:bg-red-600 text-white text-sm font-medium rounded-lg transition-colors shadow-sm flex items-center gap-2"
                  @click="removeAvatar"
              >
                <i class="ph ph-trash"></i>
                删除
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 基本信息表单 -->
    <div v-if="currentTab === 'basic'" class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden">
      <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between px-6">
        <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
          <i class="ph ph-identification-card text-lg"></i>
          基本信息
        </h2>
        <button
            v-if="!isEditing"
            class="text-xs px-3 py-1.5 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors"
            @click="startEdit"
        >
          <i class="ph ph-pencil-simple mr-1"></i>
          编辑
        </button>
      </div>

      <div class="p-6 space-y-6">
        <!-- 昵称（必填） -->
        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-user text-lg"></i>
            昵称
            <span class="text-red-500">*</span>
          </label>
          <input
              v-model="formData.username"
              type="text"
              :disabled="!isEditing"
              class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#2a2a2a] text-gray-800 dark:text-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed transition-all"
              placeholder="请输入昵称"
          />
        </div>

        <!-- 性别 -->
        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-gender-intersex text-lg"></i>
            性别
          </label>
          <div class="flex gap-2">
            <div :class="{ 'pointer-events-none opacity-60': !isEditing }" class="flex-1 flex gap-3">
              <label
                  v-for="option in genderOptions"
                  :key="option.value"
                  class="flex-1 flex items-center gap-2 px-4 py-3 rounded-lg border cursor-pointer transition-all"
                  :class="[
                  formData.gender === option.value
                    ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400'
                    : 'border-gray-300 dark:border-gray-600 hover:border-gray-400 dark:hover:border-gray-500'
                ]"
              >
                <input
                    v-model="formData.gender"
                    :value="option.value"
                    type="radio"
                    :disabled="!isEditing"
                    class="hidden"
                />
                <i :class="['ph', option.icon, 'text-lg']"></i>
                {{ option.label }}
              </label>
            </div>
            <button
                v-if="isFieldFilled('gender')"
                class="px-3 py-2 rounded-lg border transition-colors text-xs font-medium"
                :class="isFieldVisible('gender')
                ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700 text-blue-600 dark:text-blue-400'
                : 'bg-gray-100 dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-500 dark:text-gray-400'"
                :title="isFieldVisible('gender') ? '已公开' : '已隐藏'"
                @click="toggleVisibility('gender')"
            >
              <i :class="['ph', isFieldVisible('gender') ? 'ph-eye' : 'ph-eye-slash']"></i>
            </button>
          </div>
        </div>

        <!-- 年龄 -->
        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-calendar text-lg"></i>
            年龄
          </label>
          <input
              v-model.number="formData.age"
              type="number"
              min="1"
              max="150"
              :disabled="!isEditing"
              class="w-full px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#2a2a2a] text-gray-800 dark:text-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed transition-all"
              placeholder="请输入年龄"
          />
        </div>

        <!-- 编辑按钮 -->
        <div v-if="isEditing" class="flex gap-2 pt-4 border-t border-gray-200 dark:border-gray-700">
          <button
              class="flex-1 px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium rounded-lg transition-colors"
              @click="handleSave"
          >
            <i class="ph ph-check mr-1"></i>
            保存
          </button>
          <button
              class="flex-1 px-4 py-2 bg-gray-300 dark:bg-gray-700 hover:bg-gray-400 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 text-sm font-medium rounded-lg transition-colors"
              @click="cancelEdit"
          >
            <i class="ph ph-x mr-1"></i>
            取消
          </button>
        </div>
      </div>
    </div>

    <!-- 联系方式 -->
    <div v-if="currentTab === 'contact'" class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden">
      <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between px-6">
        <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
          <i class="ph ph-address-book text-lg"></i>
          联系方式
        </h2>
        <button
            v-if="!isEditing"
            class="text-xs px-3 py-1.5 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors"
            @click="startEdit"
        >
          <i class="ph ph-pencil-simple mr-1"></i>
          编辑
        </button>
      </div>

      <div class="p-6 space-y-6">
        <!-- 邮箱 -->
        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-envelope text-lg"></i>
            邮箱
          </label>
          <div class="flex gap-2">
            <input
                v-model="formData.email"
                type="email"
                :disabled="!isEditing"
                class="flex-1 px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#2a2a2a] text-gray-800 dark:text-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed transition-all"
                placeholder="请输入邮箱"
            />
            <button
                v-if="isFieldFilled('email')"
                class="px-3 py-2 rounded-lg border transition-colors text-xs font-medium"
                :class="isFieldVisible('email')
                ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700 text-blue-600 dark:text-blue-400'
                : 'bg-gray-100 dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-500 dark:text-gray-400'"
                :title="isFieldVisible('email') ? '已公开' : '已隐藏'"
                @click="toggleVisibility('email')"
            >
              <i :class="['ph', isFieldVisible('email') ? 'ph-eye' : 'ph-eye-slash']"></i>
            </button>
          </div>
        </div>

        <!-- 手机号 -->
        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-phone text-lg"></i>
            手机号
          </label>
          <div class="flex gap-2">
            <input
                v-model="formData.phone"
                type="tel"
                :disabled="!isEditing"
                class="flex-1 px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#2a2a2a] text-gray-800 dark:text-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed transition-all"
                placeholder="请输入手机号"
            />
            <button
                v-if="isFieldFilled('phone')"
                class="px-3 py-2 rounded-lg border transition-colors text-xs font-medium"
                :class="isFieldVisible('phone')
                ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700 text-blue-600 dark:text-blue-400'
                : 'bg-gray-100 dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-500 dark:text-gray-400'"
                :title="isFieldVisible('phone') ? '已公开' : '已隐藏'"
                @click="toggleVisibility('phone')"
            >
              <i :class="['ph', isFieldVisible('phone') ? 'ph-eye' : 'ph-eye-slash']"></i>
            </button>
          </div>
        </div>

        <!-- QQ -->
        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-qq-logo text-lg"></i>
            QQ
          </label>
          <div class="flex gap-2">
            <input
                v-model="formData.qq"
                type="text"
                :disabled="!isEditing"
                class="flex-1 px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#2a2a2a] text-gray-800 dark:text-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed transition-all"
                placeholder="请输入QQ号"
            />
            <button
                v-if="isFieldFilled('qq')"
                class="px-3 py-2 rounded-lg border transition-colors text-xs font-medium"
                :class="isFieldVisible('qq')
                ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700 text-blue-600 dark:text-blue-400'
                : 'bg-gray-100 dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-500 dark:text-gray-400'"
                :title="isFieldVisible('qq') ? '已公开' : '已隐藏'"
                @click="toggleVisibility('qq')"
            >
              <i :class="['ph', isFieldVisible('qq') ? 'ph-eye' : 'ph-eye-slash']"></i>
            </button>
          </div>
        </div>

        <!-- 微信 -->
        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-wechat-logo text-lg"></i>
            微信
          </label>
          <div class="flex gap-2">
            <input
                v-model="formData.wechat"
                type="text"
                :disabled="!isEditing"
                class="flex-1 px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#2a2a2a] text-gray-800 dark:text-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed transition-all"
                placeholder="请输入微信号"
            />
            <button
                v-if="isFieldFilled('wechat')"
                class="px-3 py-2 rounded-lg border transition-colors text-xs font-medium"
                :class="isFieldVisible('wechat')
                ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700 text-blue-600 dark:text-blue-400'
                : 'bg-gray-100 dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-500 dark:text-gray-400'"
                :title="isFieldVisible('wechat') ? '已公开' : '已隐藏'"
                @click="toggleVisibility('wechat')"
            >
              <i :class="['ph', isFieldVisible('wechat') ? 'ph-eye' : 'ph-eye-slash']"></i>
            </button>
          </div>
        </div>

        <!-- 编辑按钮 -->
        <div v-if="isEditing" class="flex gap-2 pt-4 border-t border-gray-200 dark:border-gray-700">
          <button
              class="flex-1 px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium rounded-lg transition-colors"
              @click="handleSave"
          >
            <i class="ph ph-check mr-1"></i>
            保存
          </button>
          <button
              class="flex-1 px-4 py-2 bg-gray-300 dark:bg-gray-700 hover:bg-gray-400 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 text-sm font-medium rounded-lg transition-colors"
              @click="cancelEdit"
          >
            <i class="ph ph-x mr-1"></i>
            取消
          </button>
        </div>
      </div>
    </div>

    <!-- 社交链接 -->
    <div v-if="currentTab === 'social'" class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden">
      <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between px-6">
        <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
          <i class="ph ph-link text-lg"></i>
          社交链接
        </h2>
        <button
            v-if="!isEditing"
            class="text-xs px-3 py-1.5 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors"
            @click="startEdit"
        >
          <i class="ph ph-pencil-simple mr-1"></i>
          编辑
        </button>
      </div>

      <div class="p-6 space-y-6">
        <!-- GitHub -->
        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-github-logo text-lg"></i>
            GitHub
          </label>
          <div class="flex gap-2">
            <div class="relative flex-1">
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <span class="text-gray-400 text-sm">github.com/</span>
              </div>
              <input
                  v-model="formData.github"
                  type="text"
                  :disabled="!isEditing"
                  class="w-full pl-[90px] pr-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#2a2a2a] text-gray-800 dark:text-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed transition-all"
                  placeholder="username"
              />
            </div>
            <button
                v-if="isFieldFilled('github')"
                class="px-3 py-2 rounded-lg border transition-colors text-xs font-medium"
                :class="isFieldVisible('github')
                ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700 text-blue-600 dark:text-blue-400'
                : 'bg-gray-100 dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-500 dark:text-gray-400'"
                :title="isFieldVisible('github') ? '已公开' : '已隐藏'"
                @click="toggleVisibility('github')"
            >
              <i :class="['ph', isFieldVisible('github') ? 'ph-eye' : 'ph-eye-slash']"></i>
            </button>
          </div>
        </div>

        <!-- Gitee -->
        <div class="space-y-2">
          <label class="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <i class="ph ph-git-branch text-lg"></i>
            Gitee
          </label>
          <div class="flex gap-2">
            <div class="relative flex-1">
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <span class="text-gray-400 text-sm">gitee.com/</span>
              </div>
              <input
                  v-model="formData.gitee"
                  type="text"
                  :disabled="!isEditing"
                  class="w-full pl-[78px] pr-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#2a2a2a] text-gray-800 dark:text-gray-200 focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-60 disabled:cursor-not-allowed transition-all"
                  placeholder="username"
              />
            </div>
            <button
                v-if="isFieldFilled('gitee')"
                class="px-3 py-2 rounded-lg border transition-colors text-xs font-medium"
                :class="isFieldVisible('gitee')
                ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700 text-blue-600 dark:text-blue-400'
                : 'bg-gray-100 dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-500 dark:text-gray-400'"
                :title="isFieldVisible('gitee') ? '已公开' : '已隐藏'"
                @click="toggleVisibility('gitee')"
            >
              <i :class="['ph', isFieldVisible('gitee') ? 'ph-eye' : 'ph-eye-slash']"></i>
            </button>
          </div>
        </div>

        <!-- 编辑按钮 -->
        <div v-if="isEditing" class="flex gap-2 pt-4 border-t border-gray-200 dark:border-gray-700">
          <button
              class="flex-1 px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium rounded-lg transition-colors"
              @click="handleSave"
          >
            <i class="ph ph-check mr-1"></i>
            保存
          </button>
          <button
              class="flex-1 px-4 py-2 bg-gray-300 dark:bg-gray-700 hover:bg-gray-400 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 text-sm font-medium rounded-lg transition-colors"
              @click="cancelEdit"
          >
            <i class="ph ph-x mr-1"></i>
            取消
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
