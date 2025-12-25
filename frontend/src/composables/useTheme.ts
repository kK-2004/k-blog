import { computed, onMounted, onUnmounted, ref } from 'vue'

export type ThemeMode = 'light' | 'dark'

export function useTheme() {
  const theme = ref<ThemeMode>('light')
  let mq: MediaQueryList | null = null
  let handler: ((e: MediaQueryListEvent) => void) | null = null

  const applyThemeClass = (mode: ThemeMode) => {
    document.documentElement.classList.remove('light', 'dark')
    document.documentElement.classList.add(mode)
  }

  const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    applyThemeClass(theme.value)
  }

  onMounted(() => {
    mq = window.matchMedia('(prefers-color-scheme: dark)')
    const sync = (matches: boolean) => {
      theme.value = matches ? 'dark' : 'light'
      applyThemeClass(theme.value)
    }
    handler = (e) => sync(e.matches)
    sync(mq.matches)
    mq.addEventListener?.('change', handler)
  })

  onUnmounted(() => {
    if (mq && handler) mq.removeEventListener?.('change', handler)
  })

  const bgStyle = computed(() => {
    const lightDot = '#e5e5e5'
    const darkDot = '#333333'
    const dotPattern =
      theme.value === 'light'
        ? `radial-gradient(${lightDot} 2px, transparent 2px)`
        : `radial-gradient(${darkDot} 2px, transparent 2px)`

    return {
      backgroundColor: theme.value === 'light' ? '#ffffff' : '#000000',
      backgroundImage: dotPattern,
      backgroundSize: '24px 24px',
      backgroundPosition: '0 0, 12px 12px',
      backgroundAttachment: 'fixed',
    } as const
  })

  return { theme, toggleTheme, bgStyle }
}

