<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { listPostsPage } from '@/api/posts'
import type { Post, AdminMe } from '@/api/types'
import MacBlogCard from '@/components/mac/MacBlogCard.vue'

const props = defineProps<{
  isAuthenticated: boolean
  adminMe: AdminMe | null
  authorAvatarUrl: string | null
  scrollContainer: HTMLElement | null
}>()

const posts = ref<Post[]>([])
const page = ref(0)
const hasMore = ref(true)
const loading = ref(false)
const loadError = ref(false)
const sentinelRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const PAGE_SIZE = 5

const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  loading.value = true
  loadError.value = false
  try {
    const data = await listPostsPage(page.value, PAGE_SIZE)
    posts.value.push(...data)
    if (data.length < PAGE_SIZE) {
      hasMore.value = false
    } else {
      page.value += 1
    }
  } catch (e) {
    console.error('加载文章失败:', e)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadMore()
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
  observer?.disconnect()
  observer = null
})
</script>

  <template>
  <div class="animate-[slideIn_0.5s_ease-out]">
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