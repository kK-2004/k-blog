<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { listPostsPage } from '@/api/posts'
import type { Post, AdminMe } from '@/api/types'
import MacBlogCard from '@/components/mac/MacBlogCard.vue'
import ProfileOverview from '@/components/mac/ProfileOverview.vue'

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
    <!-- 移动端: 个人信息卡片在顶部 -->
    <div class="lg:hidden mb-6">
      <ProfileOverview :show-full-features="false" :is-authenticated="props.isAuthenticated" />
    </div>

    <!-- 移动端文章列表 -->
    <div class="lg:hidden px-4 pb-8">
      <MacBlogCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
          :isAuthenticated="props.isAuthenticated"
          :adminMe="props.adminMe"
          :authorAvatarUrl="props.authorAvatarUrl"
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

    <!-- 桌面端: 整体居中，左侧窄，右侧宽 -->
    <div class="hidden lg:flex lg:gap-8 lg:items-start lg:justify-center lg:max-w-[1400px] lg:mx-auto lg:px-8">
      <!-- 左侧个人信息卡片 - 260px 窄边栏 -->
      <aside class="sticky top-[88px] z-20 w-[260px] shrink-0 py-4">
        <ProfileOverview :show-full-features="false" :is-authenticated="props.isAuthenticated" />
      </aside>

      <!-- 右侧文章列表 - 900px 宽内容区 -->
      <div class="w-full max-w-[900px] mx-auto">
        <MacBlogCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            :isAuthenticated="props.isAuthenticated"
            :adminMe="props.adminMe"
            :authorAvatarUrl="props.authorAvatarUrl"
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
  </div>
</template>