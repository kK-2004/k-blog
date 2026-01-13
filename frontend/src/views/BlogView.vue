<script setup lang="ts">
import type { Post } from '@/api/types'
import MacBlogCard from '@/components/mac/MacBlogCard.vue'

defineProps<{
  posts: Post[]
}>()
</script>

<template>
  <div class="max-w-2xl mx-auto animate-[slideIn_0.5s_ease-out]">
    <MacBlogCard v-for="post in posts" :key="post.id" :post="post" />
    <div class="text-center mt-12 mb-8 opacity-50 text-[10px] font-mono">POWERED BY kk</div>
  </div>
</template>
