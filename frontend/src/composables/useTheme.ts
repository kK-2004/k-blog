import { computed, onMounted, onUnmounted, ref } from 'vue'

export type ThemeMode = 'light' | 'dark'

export function useTheme() {
  const theme = ref<ThemeMode>('light')
  let mq: MediaQueryList | null = null
  let handler: ((e: MediaQueryListEvent) => void) | null = null

  const applyThemeClass = (mode: ThemeMode) => {
    document.documentElement.classList.remove('light', 'dark')
    document.documentElement.classList.add(mode)

    // 动态切换 highlight.js 主题
    const hljsThemeLink = document.getElementById('hljs-theme') as HTMLLinkElement
    if (hljsThemeLink) {
      hljsThemeLink.href = mode === 'dark'
        ? 'https://cdn.jsdelivr.net/npm/highlight.js@11.9.0/styles/github-dark.min.css'
        : 'https://cdn.jsdelivr.net/npm/highlight.js@11.9.0/styles/github.min.css'
    }
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
      backgroundPosition: 'center',
      backgroundAttachment: 'fixed',
    } as const
  })

  return { theme, toggleTheme, bgStyle }
}

