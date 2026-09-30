<script setup lang="ts">
import { ref } from 'vue'
import { login } from '@/api/admin'
import type { AdminMe } from '@/api/types'

const emit = defineEmits<{
  (e: 'login-success', me: AdminMe): void
}>()

const password = ref('')
const loading = ref(false)

const onSubmit = async () => {
  if (!password.value.trim() || loading.value) return
  loading.value = true
  try {
    const me = await login(password.value.trim())
    emit('login-success', me)
    password.value = ''
  } catch {
    // 错误提示已在 login() 内通过 ElMessage 显示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex flex-col items-center justify-center h-[60vh]">
    <div
      class="w-80 bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl p-8 rounded-2xl shadow-sm border border-black/5 dark:border-white/10 text-center animate-[fadeIn_0.5s_ease-out]"
    >
      <div class="w-14 h-14 bg-[#007AFF]/10 rounded-full mx-auto mb-4 flex items-center justify-center">
        <i class="ph ph-lock-key text-2xl text-[#007AFF]"></i>
      </div>
      <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-6">登录后台</h2>
      <div class="space-y-3">
        <input
          v-model="password"
          type="password"
          placeholder="密码"
          autocomplete="current-password"
          class="w-full px-4 py-2 bg-white dark:bg-black/20 border border-gray-300 dark:border-gray-600 rounded-lg text-sm outline-none focus:ring-2 focus:ring-[#007AFF] dark:text-white"
          @keyup.enter="onSubmit"
        />
        <button
          class="w-full bg-[#007AFF] hover:bg-[#0062cc] text-white py-2 rounded-lg text-sm font-medium transition-colors"
          :disabled="loading || !password.trim()"
          :class="loading || !password.trim() ? 'opacity-60 cursor-not-allowed' : ''"
          @click="onSubmit"
        >
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </div>
    </div>
  </div>
</template>
