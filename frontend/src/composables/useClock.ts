import { onMounted, onUnmounted, ref } from 'vue'

export function useClock() {
  const timeStr = ref('')
  let timer: number | undefined

  onMounted(() => {
    const tick = () => {
      const date = new Date()
      timeStr.value = date.toLocaleString('zh-CN', {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
      })
    }
    tick()
    timer = window.setInterval(tick, 1000)
  })

  onUnmounted(() => {
    if (timer) window.clearInterval(timer)
  })

  return { timeStr }
}

