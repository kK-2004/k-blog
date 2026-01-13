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
  } catch (e: any) {
    // 错误已在 http.ts 中通过 ElMessage 统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex flex-col items-center justify-center h-[60vh]">
    <div
      class="w-80 bg-white/70 dark:bg-[#1e1e1e]/70 backdrop-blur-2xl p-8 rounded-2xl shadow-2xl border border-white/20 dark:border-white/10 text-center animate-[fadeIn_0.5s_ease-out]"
    >
      <div class="w-16 h-16 bg-gray-200 dark:bg-gray-700 rounded-full mx-auto mb-4 flex items-center justify-center text-3xl">
        🔑
      </div>
      <h2 class="text-lg font-bold text-gray-800 dark:text-white mb-6">Login</h2>
      <div class="space-y-3">
        <input
          type="password"
          placeholder="Password"
          class="w-full px-4 py-2 bg-white dark:bg-black/20 border border-gray-300 dark:border-gray-600 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500 dark:text-white"
          v-model="password"
          @keyup.enter="onSubmit"
        />
        <button
          class="w-full bg-[#007AFF] hover:bg-[#0062cc] text-white py-2 rounded-lg text-sm font-medium shadow-lg"
          :disabled="loading || !password.trim()"
          :class="loading ? 'opacity-70 cursor-not-allowed' : ''"
          @click="onSubmit"
        >
          {{ loading ? 'Signing in...' : 'Enter' }}
        </button>
      </div>
    </div>
  </div>
</template>
