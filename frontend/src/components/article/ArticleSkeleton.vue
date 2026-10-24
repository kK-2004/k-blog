<script setup lang="ts">
// 文章详情加载骨架屏：布局与 ArticleDetailView 正文区保持一致，避免数据到达后跳动
defineProps<{
  isSidebarOpen: boolean
}>()

// 正文段落行宽，模拟自然段落的参差感
const paragraphs = [
  ['100%', '96%', '98%', '72%'],
  ['100%', '94%', '88%'],
  ['100%', '97%', '92%', '99%', '60%'],
]
</script>

<template>
  <main
    class="fixed inset-0 pt-8 h-[calc(100dvh-2rem)] overflow-hidden z-10 transition-all duration-300"
    :class="isSidebarOpen ? 'left-64' : 'left-0'"
    aria-busy="true"
    aria-label="文章加载中"
  >
    <div class="h-full w-full px-3 sm:px-4 md:px-6">
      <div class="flex h-full gap-6 2xl:gap-8">

        <div class="flex-1 min-w-0 overflow-hidden">
          <div class="w-full max-w-3xl lg:max-w-4xl mx-auto py-6">

            <!-- 文章信息卡片 -->
            <div class="pl-0 lg:pl-20 rounded-xl border border-gray-200/60 dark:border-gray-800/60 mb-6">
              <div class="p-8 md:p-12 lg:p-14">
                <div class="sk h-8 md:h-10 w-3/4 rounded-lg mb-4"></div>
                <div class="sk h-8 md:h-10 w-1/2 rounded-lg mb-8"></div>
                <div class="flex items-center gap-3">
                  <div class="sk h-10 w-10 rounded-full shrink-0"></div>
                  <div class="flex-1 space-y-2">
                    <div class="sk h-3.5 w-28 rounded"></div>
                    <div class="sk h-3 w-48 max-w-full rounded"></div>
                  </div>
                </div>
              </div>
            </div>

            <!-- AI 摘要卡片 -->
            <div class="pl-0 lg:pl-20 mb-12">
              <div class="rounded-xl border border-gray-200/60 dark:border-gray-800/60 p-5 space-y-3">
                <div class="sk h-4 w-32 rounded"></div>
                <div class="sk h-3.5 w-full rounded"></div>
                <div class="sk h-3.5 w-5/6 rounded"></div>
              </div>
            </div>

            <!-- 正文段落 -->
            <div class="pl-0 lg:pl-20 space-y-10">
              <div v-for="(lines, i) in paragraphs" :key="i" class="space-y-3">
                <div v-if="i > 0" class="sk h-6 w-2/5 rounded-md mb-5"></div>
                <div v-for="(w, j) in lines" :key="j" class="sk h-4 rounded" :style="{ width: w }"></div>
              </div>
            </div>

          </div>
        </div>

        <!-- 右侧目录 -->
        <aside class="hidden xl:block w-56 shrink-0 py-6">
          <div class="pt-8 md:pt-10 lg:pt-12 space-y-3">
            <div class="sk h-4 w-20 rounded mb-5"></div>
            <div class="sk h-3 w-40 rounded"></div>
            <div class="sk h-3 w-32 rounded ml-3"></div>
            <div class="sk h-3 w-36 rounded ml-3"></div>
            <div class="sk h-3 w-44 rounded"></div>
            <div class="sk h-3 w-28 rounded ml-3"></div>
          </div>
        </aside>

      </div>
    </div>
  </main>
</template>

<style scoped>
/* 骨架块 + 扫光：渐变按视口定位（background-attachment: fixed），光带整体斜向扫过页面，而非每块各扫各的 */
.sk {
  --sk-base: rgba(0, 0, 0, 0.06);
  --sk-shine: rgba(255, 255, 255, 0.65);
  background-color: var(--sk-base);
  background-image: linear-gradient(100deg, transparent 40%, var(--sk-shine) 50%, transparent 60%);
  background-size: 200vw 100%;
  background-attachment: fixed;
  background-repeat: no-repeat;
  animation: sk-shimmer 1.6s ease-in-out infinite;
}

:global(.dark) .sk {
  --sk-base: rgba(255, 255, 255, 0.06);
  --sk-shine: rgba(255, 255, 255, 0.08);
}

@keyframes sk-shimmer {
  from { background-position: 150vw 0; }
  to   { background-position: -150vw 0; }
}

@media (prefers-reduced-motion: reduce) {
  .sk { animation: none; }
}
</style>
