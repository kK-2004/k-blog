<script setup lang="ts">
import type { Post } from '@/data/initialPosts'
import AdminDashboard from '@/components/mac/AdminDashboard.vue'

defineProps<{
  posts: Post[]
}>()
</script>

<template>
  <AdminDashboard :posts="posts" />
</template>

