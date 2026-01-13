<script setup lang="ts">
import type { Post } from '@/api/types'
import AdminDashboard from '@/components/mac/AdminDashboard.vue'

defineProps<{
  posts: Post[]
}>()

const emit = defineEmits<{
  (e: 'update:posts', posts: Post[]): void
  (e: 'refresh'): void
}>()
</script>

<template>
  <AdminDashboard
    :posts="posts"
    @update:posts="(newPosts) => emit('update:posts', newPosts)"
    @refresh="emit('refresh')"
  />
</template>
