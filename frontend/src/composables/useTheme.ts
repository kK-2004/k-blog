import { ref, onMounted, onUnmounted } from 'vue';

export const useTheme = () => {
  const theme = ref<'light' | 'dark'>('light');

  const applyTheme = (nextTheme: 'light' | 'dark') => {
    theme.value = nextTheme;
    document.documentElement.classList.remove('light', 'dark');
    document.documentElement.classList.add(nextTheme);
  };

  const toggleTheme = () => {
    applyTheme(theme.value === 'light' ? 'dark' : 'light');
  };

  // Initialize theme based on system preference
  const initTheme = () => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    
    const updateTheme = (e: MediaQueryList | MediaQueryListEvent) => {
      applyTheme(e.matches ? 'dark' : 'light');
    };

    // Initialize with system preference
    updateTheme(mq);
    
    // Listen for system theme changes
    mq.addEventListener('change', updateTheme);

    // Clean up listener on unmount
    onUnmounted(() => mq.removeEventListener('change', updateTheme));
  };

  onMounted(initTheme);

  return {
    theme,
    toggleTheme
  };
};
