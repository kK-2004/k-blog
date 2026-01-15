<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps<{
  targetRef: HTMLElement | null
  isSidebarOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const guidePosition = ref({ top: 0, left: 0 })
const arrowPath = ref('')
const svgSize = ref({ width: 0, height: 0 })
const arrowTipPosition = ref({ x: 0, y: 0 })

const updatePosition = () => {
  if (!props.targetRef) return

  const targetRect = props.targetRef.getBoundingClientRect()
  const guideWidth = 260
  const guideHeight = 120
  const verticalGap = 60

  guidePosition.value = {
    top: targetRect.bottom + verticalGap,
    left: targetRect.left + targetRect.width / 2 - guideWidth / 2
  }

  const guideLeft = guidePosition.value.left
  const guideTop = guidePosition.value.top
  const trafficLightCenterX = targetRect.left + targetRect.width / 2
  const trafficLightBottomY = targetRect.bottom

  arrowTipPosition.value = { x: trafficLightCenterX, y: trafficLightBottomY + 10}

  arrowPath.value = `M ${trafficLightCenterX} ${guideTop} L ${trafficLightCenterX} ${trafficLightBottomY + 2}`

  svgSize.value = {
    width: Math.max(guideLeft + guideWidth + 50, targetRect.right + 50),
    height: Math.max(guideTop + guideHeight + 50, targetRect.bottom + 100)
  }
}

const handleClose = () => {
  localStorage.setItem('article-detail-first-visited', 'true')
  emit('close')
}

onMounted(() => {
  updatePosition()
  window.addEventListener('resize', updatePosition)
})

// 监听 targetRef 变化，当从 null 变为有效值时更新位置
watch(() => props.targetRef, (newRef) => {
  if (newRef) {
    updatePosition()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updatePosition)
})

defineExpose({
  updatePosition
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade-slide">
      <div
        v-if="targetRef && arrowPath"
        class="fixed z-[60] pointer-events-none"
        :style="{ 
          width: `${svgSize.width}px`, 
          height: `${svgSize.height}px`,
          top: '0',
          left: '0'
        }"
      >
        <svg
          :width="svgSize.width"
          :height="svgSize.height"
          style="overflow: visible;"
        >
          <path
            :d="arrowPath"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-dasharray="6,6"
            class="text-orange-400 dark:text-orange-500"
            stroke-linecap="round"
            stroke-linejoin="round"
          />

          <polygon
            :points="`${arrowTipPosition.x - 6},${arrowTipPosition.y - 6} ${arrowTipPosition.x + 6},${arrowTipPosition.y - 6} ${arrowTipPosition.x},${arrowTipPosition.y - 14}`"
            fill="currentColor"
            class="text-orange-400 dark:text-orange-500"
          />
        </svg>

        <div
          class="absolute pointer-events-auto"
          :style="{ top: `${guidePosition.top}px`, left: `${guidePosition.left}px` }"
        >
          <div class="bg-white dark:bg-[#1c1c1e] border border-gray-200 dark:border-gray-700 rounded-2xl p-5 shadow-xl max-w-[260px]">
            <p class="text-sm text-gray-700 dark:text-gray-300 leading-relaxed mb-4">
              点击这里的 <span class="inline-flex items-center gap-1 text-red-500 font-medium">🚥</span>按钮可以返回博客首页查看更多文章哦～
            </p>

            <button
              @click="handleClose"
              class="w-full py-2.5 px-4 bg-white hover:bg-gray-50 text-gray-800 border border-gray-200 text-sm font-medium rounded-xl transition-all duration-200 hover:shadow-md active:scale-95"
            >
              我知道啦 ✨
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease-out;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
