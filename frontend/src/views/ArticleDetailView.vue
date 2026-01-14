<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { getPost, incrementLikes, incrementViews } from '@/api/posts'
import { useReadStats } from '@/composables/useReadStats'
import { useScrollProgress } from '@/composables/useScrollProgress'
import type { Post, AdminMe } from '@/api/types'
import type { Heading } from '@/components/article/ArticleTOC.vue'

// 组件导入
import ArticleHeader from '@/components/article/ArticleHeader.vue'
import ArticleMeta from '@/components/article/ArticleMeta.vue'
import ArticleContent from '@/components/article/ArticleContent.vue'
import ArticleTOC from '@/components/article/ArticleTOC.vue'
import ArticleInteractionBar from '@/components/article/ArticleInteractionBar.vue'
import AiSummaryCard from '@/components/article/AiSummaryCard.vue'
import CommentSection from '@/components/comment/CommentSection.vue'

// Props
const props = defineProps<{
  postId: number
  isAuthenticated: boolean
  adminMe: AdminMe | null
  authorAvatarUrl: string | null
  isSidebarOpen: boolean
}>()

// 数据状态
const post = ref<Post | null>(null)
const loading = ref(true)
const error = ref(false)

// 滚动和头部状态
const { scrollProgress, activeHeadingId } = useScrollProgress()
const showHeaderTitle = ref(true) // 一开始就显示标题

// 标题
const headings = ref<Heading[]>([])

// 点赞状态
const localLikes = ref(0)
const hasLiked = ref(false)

// 阅读统计
const readStats = computed(() => {
  if (!post.value) return { count: '', time: '' }
  return useReadStats(computed(() => post.value?.content || '')).value
})

// 组件引用
const commentSectionRef = ref<InstanceType<typeof CommentSection> | null>(null)
const interactionBarRef = ref<InstanceType<typeof ArticleInteractionBar> | null>(null)

// 获取文章详情
const fetchPost = async () => {
  try {
    loading.value = true
    error.value = false
    const data = await getPost(props.postId)
    post.value = data
    localLikes.value = data.likes

    // 增加浏览量
    await incrementViews(props.postId)
  } catch (e) {
    console.error('获取文章失败:', e)
    error.value = true
  } finally {
    loading.value = false
  }
}

// 点赞
const handleLike = () => {
  if (hasLiked.value) return
  localLikes.value++
  hasLiked.value = true
  incrementLikes(props.postId).catch(() => {
    localLikes.value = Math.max(0, localLikes.value - 1)
    hasLiked.value = false
  })
}

// 滚动到评论区
const scrollToComment = () => {
  const el = document.getElementById('comments-section')
  const scrollContainer = document.querySelector('article.overflow-y-auto')

  if (el && scrollContainer) {
    const containerRect = (scrollContainer as HTMLElement).getBoundingClientRect()
    const elementRect = el.getBoundingClientRect()
    const offset = 100

    // 计算元素相对于滚动容器的位置
    const scrollTop = (scrollContainer as HTMLElement).scrollTop
    const elementPosition = scrollTop + elementRect.top - containerRect.top - offset;

    (scrollContainer as HTMLElement).scrollTo({
      top: elementPosition,
      behavior: 'smooth'
    })
  }
}

// 处理标题更新
const handleHeadingsUpdate = (newHeadings: Heading[]) => {
  headings.value = newHeadings
}

// 处理回复按钮点击 - 由 CommentSection 发射
const handleReply = (value: { rootId: number; toUser?: string }) => {
  // 调用底部输入框的 setReplyingTo 方法
  interactionBarRef.value?.setReplyingTo(value)
}

// 处理评论提交成功 - 由 ArticleInteractionBar 发射
const handleCommentSubmitted = (data: {
  id: number
  user: string
  text: string
  rootId?: number
  toUser?: string
}) => {
  // 调用 CommentSection 的 addComment 方法添加到列表
  commentSectionRef.value?.addComment(data)
}

// 组件挂载时获取数据
onMounted(() => {
  const prevHtmlOverflow = document.documentElement.style.overflow
  const prevBodyOverflow = document.body.style.overflow
  document.documentElement.style.overflow = 'hidden'
  document.body.style.overflow = 'hidden'

  onBeforeUnmount(() => {
    document.documentElement.style.overflow = prevHtmlOverflow
    document.body.style.overflow = prevBodyOverflow
  })

  fetchPost()
})
</script>

<template>
  <div class="h-screen h-[100dvh] overflow-hidden bg-[#f5f5f700] dark:bg-[#12121200] transition-colors duration-300 font-sans selection:bg-blue-100 selection:text-blue-900">

    <div v-if="loading" class="fixed inset-0 pt-32 flex items-start justify-center">
    </div>
    <div v-else-if="error || !post" class="fixed inset-0 pt-32 flex items-start justify-center">
    </div>

    <template v-else>
      <ArticleHeader
          :post="post"
          :scrollProgress="scrollProgress"
          :showTitle="showHeaderTitle"
          :isSidebarOpen="isSidebarOpen"
      />

      <!-- 固定视口容器：左侧滚动，右侧固定 -->
      <main class="fixed inset-0 pt-2 overflow-hidden z-10">
        <div class="h-full w-full px-4 sm:px-6">
          <div class="flex h-full gap-6 2xl:gap-8">

            <!-- 左侧：正文滚动区域 -->
            <article class="flex-1 overflow-y-auto custom-scrollbar scroll-smooth">
              <div class="max-w-[110rem] mx-auto py-6 pb-64">

                <!-- 文章卡片 -->
                <div class="rounded-xl border border-gray-200/60 dark:border-gray-800/60 overflow-hidden">
                  <div class="p-8 md:p-12 lg:p-14">

                    <ArticleMeta
                        :author="post.author"
                        :createdAt="post.createdAt"
                        :readTime="readStats.time"
                        :wordCount="readStats.count"
                        :views="post.views"
                        :tags="[]"
                        :authorAvatarUrl="authorAvatarUrl"
                        class="mb-8"
                    />

                    <AiSummaryCard
                        :postId="post.id"
                        :content="post.content"
                        class="mb-12"
                    />

                    <div class="article-body">
                      <ArticleContent
                          :content="post.content"
                          @headings="handleHeadingsUpdate"
                      />
                    </div>
                  </div>
                </div>

                <!-- 评论区域 -->
                <section id="comments-section" class="mt-8 rounded-xl border border-gray-200/60 dark:border-gray-800/60 p-6 md:p-8">
                  <CommentSection
                      ref="commentSectionRef"
                      :postId="post.id"
                      :isAuthenticated="isAuthenticated"
                      :adminMe="adminMe"
                      :authorAvatarUrl="authorAvatarUrl"
                      @reply-to="handleReply"
                  />
                </section>

              </div>
            </article>

            <!-- 右侧：目录固定 -->
            <aside class="hidden xl:block 2xl:w-48 shrink-0 py-6">
              <div class="sticky top-0 pt-8 md:pt-10 lg:pt-12">
                <ArticleTOC
                    :headings="headings"
                    :activeId="activeHeadingId"
                />
              </div>
            </aside>

          </div>
        </div>
      </main>

      <!-- 底部交互栏 -->
      <Teleport to="body">
        <ArticleInteractionBar
            ref="interactionBarRef"
            :postId="post.id"
            :likes="localLikes"
            :hasLiked="hasLiked"
            :hasComments="post.comments > 0"
            :isAuthenticated="isAuthenticated"
            :adminMe="adminMe"
            :isSidebarOpen="isSidebarOpen"
            @like="handleLike"
            @comment="scrollToComment"
            @share="() => {}"
            @comment-submitted="handleCommentSubmitted"
        />
      </Teleport>

    </template>
  </div>
</template>

<style scoped>
/* 正文区域滚动条美化 */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(156, 163, 175, 0.2);
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(156, 163, 175, 0.4);
}

/* 解决正文标题重复问题：
  如果 ArticleContent 渲染的 Markdown 里包含 h1，
  这里将其强制隐藏（因为我们在上面已经手动渲染了 H1）
*/
:deep(.article-body h1) {
  display: none;
}

/* 标题锚点偏移
  增加偏移量以避免被 Header 遮挡
*/
:deep(.prose h2[id]),
:deep(.prose h3[id]) {
  scroll-margin-top: 140px;
}

/* 全局滚动平滑 */
:deep(html) {
  scroll-behavior: smooth;
}
</style>
