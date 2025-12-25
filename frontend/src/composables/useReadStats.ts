import { computed, type ComputedRef } from 'vue'

export function useReadStats(content: ComputedRef<string> | { value: string }) {
  return computed(() => {
    const text = content.value || ''
    const len = text.replace(/[\s\n`*#]/g, '').length

    let countStr = ''
    if (len < 1000) countStr = `${len} 字`
    else if (len < 10000) countStr = `${(len / 1000).toFixed(1)}千字`
    else countStr = `${(len / 10000).toFixed(1)}万字`

    const minutes = Math.max(1, Math.ceil(len / 400))
    return { count: countStr, time: `${minutes} 分钟` }
  })
}

