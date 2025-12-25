<script setup lang="ts">
import AdminLogin from '@/components/mac/AdminLogin.vue'

defineEmits<{
  (e: 'login-success'): void
}>()
</script>

<template>
  <AdminLogin @login-success="$emit('login-success')" />
</template>

