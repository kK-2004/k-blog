<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import type { AdminMe } from '@/api/types'
import MacBlogCard from '@/components/mac/MacBlogCard.vue'
import { PAGE_SIZE, useBlogFeed } from '@/composables/useBlogFeed'

const props = defineProps<{
  isAuthenticated: boolean
  adminMe: AdminMe | null
  authorAvatarUrl: string | null
  scrollContainer: HTMLElement | null
}>()

const {
  posts, hasMore, loading, loadError, newCount, hasCache,
  loadMore, checkForNew, applyNew, saveScroll, restoreScroll,
} = useBlogFeed()

const sentinelRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

// 一页全是新文章时，实际可能更多
const newCountLabel = computed(() => (newCount.value >= PAGE_SIZE ? `${PAGE_SIZE}+` : String(newCount.value)))

const onClickNew = () => {
  applyNew()
  props.scrollContainer?.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  if (hasCache.value) {
    // 返回首页：先展示缓存并恢复滚动位置，后台检查是否有新文章
    await nextTick()
    restoreScroll(props.scrollContainer)
    void checkForNew()
  } else {
    await loadMore()
  }
  observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          loadMore()
        }
      },
      { root: props.scrollContainer, rootMargin: '200px 0px' },
  )
  if (sentinelRef.value) observer.observe(sentinelRef.value)
})

onBeforeUnmount(() => {
  saveScroll(props.scrollContainer)
  observer?.disconnect()
  observer = null
})
</script>

  <template>
  <div class="animate-[slideIn_0.5s_ease-out]">
    <!-- 新文章胶囊提示：高度为 0 的 sticky 容器，不占位、不推动列表 -->
    <div class="sticky top-3 z-30 h-0 flex items-start justify-center pointer-events-none">
      <Transition name="capsule">
        <button
            v-if="newCount > 0"
            type="button"
            class="pointer-events-auto flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-medium text-white bg-blue-500/90 dark:bg-blue-500/80 backdrop-blur-md shadow-lg shadow-blue-500/25 hover:bg-blue-500 active:scale-95 transition-[background-color,transform]"
            @click="onClickNew"
        >
          <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M12 19V5M5 12l7-7 7 7" />
          </svg>
          有 {{ newCountLabel }} 篇新的博客
        </button>
      </Transition>
    </div>

    <!-- 文章列表 -->
    <div class="px-0 pb-4 pt-4">
      <MacBlogCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
          :isAuthenticated="props.isAuthenticated"
          :adminMe="props.adminMe"
          :authorAvatarUrl="props.authorAvatarUrl"
          :scrollContainer="scrollContainer"
          class="mx-auto"
      />

      <div v-if="loadError" class="text-center mt-6">
        <button
            class="px-4 py-2 rounded-lg text-xs bg-gray-100 dark:bg-white/10 hover:bg-gray-200 dark:hover:bg-white/15 transition-colors"
            @click="loadMore"
        >
          加载失败，点击重试
        </button>
      </div>

      <div v-else-if="loading" class="text-center mt-6 opacity-70 text-xs">
        加载中...
      </div>

      <div v-else-if="!hasMore && posts.length > 0" class="text-center mt-6 opacity-60 text-xs">
        没有更多了
      </div>

      <div ref="sentinelRef" class="h-10"></div>
      <div class="text-center mt-12 mb-8 opacity-50 text-[10px] font-mono">POWERED BY kk</div>
    </div>
  </div>
</template>

<style scoped>
.capsule-enter-active,
.capsule-leave-active {
  transition: opacity 0.3s ease, transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.capsule-enter-from,
.capsule-leave-to {
  opacity: 0;
  transform: translateY(-12px) scale(0.9);
}
</style>
