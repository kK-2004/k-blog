import { ref, nextTick } from 'vue'

export interface Heading {
  id: string
  text: string
  level: number
}

/**
 * 从文章内容中提取标题结构
 * @returns headings 标题列表, extractHeadings 提取标题的方法
 */
export function useArticleHeadings() {
  const headings = ref<Heading[]>([])

  /**
   * 从渲染后的 HTML 中提取标题
   * @param htmlContent 渲染后的 HTML 内容
   */
  const extractHeadingsFromHTML = (htmlContent: string): Heading[] => {
    const parser = new DOMParser()
    const doc = parser.parseFromString(htmlContent, 'text/html')
    const headingElements = doc.querySelectorAll('h1, h2, h3, h4, h5, h6')

    const extractedHeadings: Heading[] = []

    headingElements.forEach((heading, index) => {
      const id = `heading-${index}`
      const text = heading.textContent || ''
      const level = parseInt(heading.tagName.charAt(1))

      extractedHeadings.push({
        id,
        text,
        level
      })
    })

    return extractedHeadings
  }

  /**
   * 更新标题列表并为其添加 ID
   * 应该在内容渲染后调用
   */
  const updateHeadings = () => {
    nextTick(() => {
      const proseElements = document.querySelectorAll('.prose')

      if (proseElements.length === 0) {
        return
      }

      // 获取第一个 .prose 元素（通常是文章内容）
      const proseElement = proseElements[0]
      if (!proseElement) return

      const headingElements = proseElement.querySelectorAll('h1, h2, h3, h4, h5, h6')

      const newHeadings: Heading[] = []

      headingElements.forEach((heading, index) => {
        const id = `heading-${index}`

        // 为标题元素添加 ID
        if (heading instanceof HTMLElement) {
          heading.id = id
        }

        const text = heading.textContent || ''
        const level = parseInt(heading.tagName.charAt(1))

        newHeadings.push({
          id,
          text,
          level
        })
      })

      headings.value = newHeadings
    })
  }

  return {
    headings,
    extractHeadingsFromHTML,
    updateHeadings
  }
}
