<script setup lang="ts">
import { watch } from 'vue'

const props = defineProps<{
  imageUrl: string
  isOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const handleClose = () => emit('close')

// ESC 键关闭
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && props.isOpen) {
    e.preventDefault()
    handleClose()
  }
}

watch(() => props.isOpen, (isOpen) => {
  if (isOpen) {
    document.addEventListener('keydown', handleKeydown)
  } else {
    document.removeEventListener('keydown', handleKeydown)
  }
})

// 点击背景关闭
const handleBackdropClick = (e: MouseEvent) => {
  if ((e.target as HTMLElement).classList.contains('lightbox-backdrop')) {
    handleClose()
  }
}

// 阻止图片点击冒泡
const handleImageClick = (e: MouseEvent) => {
  e.stopPropagation()
}
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="isOpen"
        class="lightbox-backdrop fixed inset-0 z-[9999] bg-black/80 flex items-center justify-center p-4 cursor-zoom-out"
        @click="handleBackdropClick"
      >
        <!-- 关闭按钮 -->
        <button
          class="absolute top-4 right-4 text-white/70 hover:text-white transition-colors p-2 rounded-full hover:bg-white/10"
          @click="handleClose"
        >
          <i class="ph ph-x text-3xl"></i>
        </button>

        <!-- 图片容器 -->
        <div
          class="max-w-full max-h-full flex items-center justify-center"
          @click="handleImageClick"
        >
          <img
            :src="imageUrl"
            class="max-w-full max-h-[90vh] object-contain rounded-lg shadow-2xl"
            alt="预览图片"
          />
        </div>

        <!-- 加载提示 -->
        <div class="absolute bottom-4 left-1/2 -translate-x-1/2 text-white/50 text-sm">
          点击背景或按 ESC 关闭
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
