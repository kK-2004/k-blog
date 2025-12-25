function escapeHtml(input: string) {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

export function renderMarkdown(text?: string) {
  if (!text) return ''

  const anyWindow = window as unknown as {
    marked?: { parse?: (s: string) => string; setOptions?: (o: Record<string, unknown>) => void }
  }

  anyWindow.marked?.setOptions?.({ breaks: true, gfm: true })
  const parse = anyWindow.marked?.parse
  if (typeof parse === 'function') return parse(text)

  return escapeHtml(text).replace(/\n/g, '<br/>')
}
