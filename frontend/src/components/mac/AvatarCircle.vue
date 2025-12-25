<script setup lang="ts">
import { computed } from 'vue'
import { getAvatarChar, getAvatarColor } from '@/composables/useAvatar'

const props = defineProps<{
  name?: string
  size?: 'sm' | 'md' | 'lg'
}>()

const colorClass = computed(() => getAvatarColor(props.name))
const char = computed(() => getAvatarChar(props.name))
const sizeClass = computed(() => {
  switch (props.size) {
    case 'sm':
      return 'w-5 h-5 text-[10px]'
    case 'md':
      return 'w-8 h-8 text-xs'
    case 'lg':
      return 'w-11 h-11 text-sm'
    default:
      return 'w-8 h-8 text-xs'
  }
})
</script>

<template>
  <div
    class="rounded-full flex items-center justify-center text-white font-bold shadow-sm select-none transition-colors duration-300"
    :class="[sizeClass, colorClass]"
  >
    {{ char }}
  </div>
</template>

