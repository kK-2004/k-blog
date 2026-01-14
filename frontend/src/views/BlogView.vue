<script setup lang="ts">
import type { Post, AdminMe } from '@/api/types'
import MacBlogCard from '@/components/mac/MacBlogCard.vue'

defineProps<{
  posts: Post[]
  isAuthenticated: boolean
  adminMe: AdminMe | null
  authorAvatarUrl: string | null
}>()
</script>

<template>
  <div class="max-w-2xl mx-auto animate-[slideIn_0.5s_ease-out]">
    <MacBlogCard
      v-for="post in posts"
      :key="post.id"
      :post="post"
      :isAuthenticated="isAuthenticated"
      :adminMe="adminMe"
      :authorAvatarUrl="authorAvatarUrl"
    />
    <div class="text-center mt-12 mb-8 opacity-50 text-[10px] font-mono">POWERED BY kk</div>
  </div>
</template>
