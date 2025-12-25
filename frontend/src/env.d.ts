/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>;
  export default component;
}

declare module '@phosphor-icons/web/regular'

interface Window {
  marked?: {
    parse?: (markdown: string) => string
    setOptions?: (options: Record<string, unknown>) => void
  }
}
