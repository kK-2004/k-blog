<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps<{
  targetRef: HTMLElement | null       // 步骤1的目标（红绿灯）
  nicknameInputRef: HTMLElement | null // 步骤2的目标（昵称输入框）
  isSidebarOpen: boolean
  step: 1 | 2
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'next'): void
}>()

// 根据步骤获取当前目标元素
const currentTarget = computed(() => {
  return props.step === 1 ? props.targetRef : props.nicknameInputRef
})

// 根据步骤获取引导内容
const guideContent = computed(() => {
  if (props.step === 1) {
    return {
      icon: '🚥',
      text: '点击这里的按钮可以返回博客首页查看更多文章哦～',
      buttonText: '下一步'
    }
  } else {
    return {
      icon: '✏️',
      text: '在这里设置你的评论昵称，默认为 Guest 哦～',
      buttonText: '我知道啦 ✨'
    }
  }
})

const guidePosition = ref({ top: 0, left: 0 })
const arrowPath = ref('')
const svgSize = ref({ width: 0, height: 0 })
const arrowTipPosition = ref({ x: 0, y: 0 })

const updatePosition = () => {
  const target = props.step === 1 ? props.targetRef : props.nicknameInputRef
  if (!target) return

  const targetRect = target.getBoundingClientRect()
  const guideWidth = 260
  const guideHeight = 120

  // 步骤1在目标下方显示，步骤2在目标上方显示（因为输入框在底部）
  const verticalGap = props.step === 1 ? 60 : 20

  guidePosition.value = {
    top: props.step === 1
      ? targetRect.bottom + verticalGap
      : targetRect.top - guideHeight - verticalGap - 80,
    left: targetRect.left + targetRect.width / 2 - guideWidth / 2
  }

  const guideLeft = guidePosition.value.left
  const guideTop = guidePosition.value.top
  const targetCenterX = targetRect.left + targetRect.width / 2

  // 步骤1：箭头从引导框底部指向目标顶部；步骤2：箭头从引导框底部指向目标顶部
  arrowTipPosition.value = {
    x: targetCenterX,
    y: props.step === 1 ? targetRect.bottom + 10 : targetRect.top - 10
  }

  // 步骤1：引导框在目标下方，箭头向下；步骤2：引导框在目标上方，箭头向上
  if (props.step === 1) {
    arrowPath.value = `M ${targetCenterX} ${guideTop} L ${targetCenterX} ${targetRect.bottom + 2}`
  } else {
    arrowPath.value = `M ${targetCenterX} ${guideTop + guideHeight} L ${targetCenterX} ${targetRect.top - 10}`
  }

  svgSize.value = {
    width: Math.max(guideLeft + guideWidth + 50, targetRect.right + 50),
    height: Math.max(guideTop + guideHeight + 50, targetRect.bottom + 100)
  }
}

const handleClose = () => {
  if (props.step === 1) {
    emit('next')
  } else {
    localStorage.setItem('article-detail-first-visited', 'true')
    emit('close')
  }
}

onMounted(() => {
  updatePosition()
  window.addEventListener('resize', updatePosition)
})

// 监听当前目标元素变化，当从 null 变为有效值时更新位置
watch(currentTarget, (newRef) => {
  if (newRef) {
    updatePosition()
  }
})

// 监听步骤变化，更新位置
watch(() => props.step, () => {
  updatePosition()
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
        v-if="currentTarget && arrowPath"
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
            v-if="step === 1"
            :points="`${arrowTipPosition.x - 6},${arrowTipPosition.y - 6} ${arrowTipPosition.x + 6},${arrowTipPosition.y - 6} ${arrowTipPosition.x},${arrowTipPosition.y - 14}`"
            fill="currentColor"
            class="text-orange-400 dark:text-orange-500"
          />
          <polygon
            v-else
            :points="`${arrowTipPosition.x - 6},${arrowTipPosition.y + 6} ${arrowTipPosition.x + 6},${arrowTipPosition.y + 6} ${arrowTipPosition.x},${arrowTipPosition.y + 14}`"
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
              {{ guideContent.text }}
            </p>

            <button
              @click="handleClose"
              class="w-full py-2.5 px-4 bg-white hover:bg-gray-50 text-gray-800 border border-gray-200 text-sm font-medium rounded-xl transition-all duration-200 hover:shadow-md active:scale-95"
            >
              {{ guideContent.buttonText }}
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
