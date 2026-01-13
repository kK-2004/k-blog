import { marked } from 'marked'
import hljs from 'highlight.js'

// 语言映射表 - 将常见缩写映射到完整名称
const languageMap: Record<string, string> = {
  js: 'JavaScript',
  ts: 'TypeScript',
  jsx: 'React JSX',
  tsx: 'React TSX',
  py: 'Python',
  java: 'Java',
  cpp: 'C++',
  c: 'C',
  cs: 'C#',
  go: 'Go',
  rs: 'Rust',
  rb: 'Ruby',
  php: 'PHP',
  swift: 'Swift',
  kt: 'Kotlin',
  scala: 'Scala',
  sh: 'Shell',
  bash: 'Bash',
  zsh: 'Zsh',
  css: 'CSS',
  scss: 'SCSS',
  sass: 'Sass',
  less: 'Less',
  html: 'HTML',
  xml: 'XML',
  svg: 'SVG',
  json: 'JSON',
  yaml: 'YAML',
  yml: 'YAML',
  toml: 'TOML',
  sql: 'SQL',
  md: 'Markdown',
  vue: 'Vue',
  nginx: 'Nginx',
  docker: 'Docker',
}

// 获取语言显示名称
function getLanguageName(lang: string): string {
  if (!lang) return 'Code'

  // 标准化语言标识符：转小写并去除特殊字符
  const normalizedLang = lang.toLowerCase().trim()

  // 查找映射表
  if (languageMap[normalizedLang]) {
    return languageMap[normalizedLang]
  }

  // 如果映射表中没有，尝试别名匹配
  const aliasMap: Record<string, string> = {
    'javascript': 'JavaScript',
    'typescript': 'TypeScript',
    'js': 'JavaScript',
    'ts': 'TypeScript',
    'python': 'Python',
    'py': 'Python',
    'java': 'Java',
    'cpp': 'C++',
    'c++': 'C++',
    'c': 'C',
    'go': 'Go',
    'rust': 'Rust',
    'rs': 'Rust',
    'ruby': 'Ruby',
    'php': 'PHP',
    'swift': 'Swift',
    'kotlin': 'Kotlin',
    'kt': 'Kotlin',
    'scala': 'Scala',
    'shell': 'Shell',
    'bash': 'Bash',
    'css': 'CSS',
    'scss': 'SCSS',
    'html': 'HTML',
    'json': 'JSON',
    'yaml': 'YAML',
    'yml': 'YAML',
    'sql': 'SQL',
    'markdown': 'Markdown',
    'md': 'Markdown',
    'vue': 'Vue',
  }

  if (aliasMap[normalizedLang]) {
    return aliasMap[normalizedLang]
  }

  // 最后的fallback：首字母大写
  return lang.charAt(0).toUpperCase() + lang.slice(1)
}

// 高亮代码
function highlightCode(code: string, language: string): string {
  if (language && hljs.getLanguage(language)) {
    try {
      return hljs.highlight(code, { language }).value
    } catch (e) {
      // 代码高亮失败，回退到自动检测
    }
  }
  return hljs.highlightAuto(code).value
}

// 自定义代码块渲染器
const renderer = new marked.Renderer()

renderer.code = function (options: { text: string; lang?: string; escaped?: boolean }) {
  const { text: code, lang: language = '' } = options
  const langName = getLanguageName(language)
  const highlightedCode = highlightCode(code, language)

  return `
    <div class="markdown-code-block">
      <div class="code-block-header">
        <span class="code-language">${langName}</span>
        <button class="code-copy-btn" onclick="copyCode(this)" title="Copy code">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15.666 3.888A2.25 2.25 0 0013.5 2.25h-3c-1.03 0-1.9.693-2.166 1.638m7.332 0c.055.194.084.418.084.612v0a.75.75 0 01-.75.75H9.75a.75.75 0 01-.75-.75v0c0-.212.03-.418.084-.612m7.332 0c.646.049 1.288.11 1.927.184 1.1.128 1.907 1.077 1.907 2.16V15a2.25 2.25 0 01-2.25 2.25H6.75A2.25 2.25 0 014.5 15V5.637c0-1.082.806-1.03 1.907-2.16.639-.074 1.281-.135 1.927-.184" />
          </svg>
          <span class="copy-text">Copy</span>
        </button>
      </div>
      <pre class="hljs"><code class="language-${language}">${highlightedCode}</code></pre>
    </div>
  `
}

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
  renderer,
})

// 全局复制函数（需要在 window 上定义）
if (typeof window !== 'undefined') {
  ;(window as any).copyCode = async function (button: HTMLButtonElement) {
    const codeBlock = button.closest('.markdown-code-block')
    const code = codeBlock?.querySelector('code')?.textContent || ''

    try {
      await navigator.clipboard.writeText(code)
      const textSpan = button.querySelector('.copy-text') as HTMLElement
      textSpan.textContent = 'Copied!'
      button.classList.add('copied')

      setTimeout(() => {
        textSpan.textContent = 'Copy'
        button.classList.remove('copied')
      }, 2000)
    } catch (err) {
      const textSpan = button.querySelector('.copy-text') as HTMLElement
      textSpan.textContent = 'Failed'
      setTimeout(() => {
        textSpan.textContent = 'Copy'
      }, 2000)
    }
  }
}

export function renderMarkdown(text?: string) {
  if (!text) return ''
  return marked(text)
}
