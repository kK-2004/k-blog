<script setup lang="ts">
const goHome = () => {
  window.location.hash = '#/blog'
}
</script>

<template>
  <div class="fixed inset-0 flex items-center justify-center bg-white/50 dark:bg-black/30 backdrop-blur-sm">
    <div class="not-found-card">
      <!-- 动画404数字 -->
      <div class="number-404">404</div>

      <!-- emoji插图 -->
      <div class="emoji">🧸</div>

      <!-- 提示文字 -->
      <p class="message">哎呀，文章好像跑丢了...</p>
      <p class="sub-message">你要找的文章可能已被删除或从未存在</p>

      <!-- 返回按钮 -->
      <button @click="goHome" class="back-button">
        <i class="ph ph-house"></i>
        <span>返回首页</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.not-found-card {
  @apply bg-white/90 dark:bg-[#1c1c1e]/90 backdrop-blur-xl;
  @apply border border-gray-200 dark:border-gray-700 rounded-3xl;
  @apply p-10 md:p-14;
  @apply shadow-2xl;
  @apply flex flex-col items-center;
  @apply max-w-md mx-auto;
  animation: fadeInUp 0.5s ease-out;
}

.number-404 {
  @apply text-[120px] md:text-[140px] font-bold leading-none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: float 3s ease-in-out infinite;
}

.emoji {
  @apply text-6xl md:text-7xl;
  animation: bounce 2s ease-in-out infinite;
  margin-top: -20px;
  margin-bottom: 20px;
}

.message {
  @apply text-xl md:text-2xl font-medium text-gray-800 dark:text-gray-200;
  @apply text-center;
  @apply mb-2;
}

.sub-message {
  @apply text-sm md:text-base text-gray-500 dark:text-gray-400;
  @apply text-center;
  @apply mb-8;
}

.back-button {
  @apply flex items-center gap-2;
  @apply px-6 py-3;
  @apply bg-gradient-to-r from-[#667eea] to-[#764ba2];
  @apply text-white font-medium;
  @apply rounded-xl;
  @apply shadow-lg shadow-purple-500/25;
  @apply transition-all duration-300;
  @apply hover:shadow-xl hover:shadow-purple-500/40;
  @apply hover:-translate-y-1 hover:scale-105;
  @apply active:translate-y-0 active:scale-100;
}

.back-button i {
  @apply text-xl;
}

/* 动画定义 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  25% {
    transform: translateY(-10px) rotate(-2deg);
  }
  75% {
    transform: translateY(-5px) rotate(2deg);
  }
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-15px) scale(1.1);
  }
}
</style>
