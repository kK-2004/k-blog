<template>
  <div class="flex flex-col items-center justify-center h-[60vh]">
    <div class="w-80 bg-white/70 dark:bg-[#1e1e1e]/70 backdrop-blur-2xl p-8 rounded-2xl shadow-2xl border border-white/20 dark:border-white/10 text-center animate-[fadeIn_0.5s_ease-out]">
      <div class="w-16 h-16 bg-gray-200 dark:bg-gray-700 rounded-full mx-auto mb-4 flex items-center justify-center text-3xl">
        🧑‍💻
      </div>
      <h2 class="text-lg font-bold text-gray-800 dark:text-white mb-1">KK Admin</h2>
      <p class="text-xs text-gray-500 mb-6">请输入管理员密码</p>
      
      <div class="space-y-3">
        <div class="relative">
          <input 
            type="password" 
            placeholder="Password"
            class="w-full pl-9 pr-4 py-2 bg-white dark:bg-black/20 border border-gray-300 dark:border-gray-600 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-white"
          />
          <div class="absolute left-3 top-2.5 text-gray-400">
            <i class="ph ph-lock"></i>
          </div>
        </div>
        <button 
          @click="handleLogin"
          class="w-full bg-[#007AFF] hover:bg-[#0062cc] text-white py-2 rounded-lg text-sm font-medium transition-colors shadow-lg shadow-blue-500/20"
        >
          登 录
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
const emit = defineEmits(['login-success']);

const handleLogin = () => {
  emit('login-success');
};
</script>