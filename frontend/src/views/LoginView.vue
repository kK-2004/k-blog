<script setup lang="ts">
import AdminLogin from '@/components/mac/AdminLogin.vue'
import type { AdminMe } from '@/api/types'

defineEmits<{
  (e: 'login-success', me: AdminMe): void
}>()
</script>

<template>
  <AdminLogin @login-success="(me) => $emit('login-success', me)" />
</template>
