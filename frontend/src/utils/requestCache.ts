/**
 * 请求去重：防止同一请求同时发起多次
 * 当多个组件同时请求相同资源时，只发起一次 HTTP 请求，其他组件等待同一个 Promise
 */
class RequestDedup {
  private pending = new Map<string, Promise<any>>()

  async request<T>(key: string, fetcher: () => Promise<T>): Promise<T> {
    // 如果已有相同请求正在进行，返回同一个 Promise
    if (this.pending.has(key)) {
      return this.pending.get(key)!
    }

    // 发起新请求
    const promise = fetcher().finally(() => {
      // 请求完成后清除缓存，允许后续请求重新发起
      this.pending.delete(key)
    })

    this.pending.set(key, promise)
    return promise
  }

  /**
   * 清除所有待处理请求
   */
  clear(): void {
    this.pending.clear()
  }
}

/**
 * 响应缓存：缓存不常变化的数据
 * 减少 HTTP 请求次数，提高响应速度
 */
class ResponseCache {
  private cache = new Map<string, { data: any; expiresAt: number }>()

  get(key: string, _maxAge: number = 60000): any | null {
    const cached = this.cache.get(key)
    if (!cached) return null

    // 检查是否过期
    if (Date.now() > cached.expiresAt) {
      this.cache.delete(key)
      return null
    }

    return cached.data
  }

  set(key: string, data: any, maxAge: number = 60000): void {
    this.cache.set(key, { data, expiresAt: Date.now() + maxAge })
  }

  /**
   * 清除缓存
   * @param pattern 可选的模式匹配，如 '/api/site' 会清除所有以该前缀开头的缓存
   */
  invalidate(pattern?: string): void {
    if (!pattern) {
      this.cache.clear()
      return
    }
    for (const key of this.cache.keys()) {
      if (key.startsWith(pattern)) {
        this.cache.delete(key)
      }
    }
  }

  /**
   * 清除特定键的缓存
   */
  delete(key: string): void {
    this.cache.delete(key)
  }

  /**
   * 获取缓存大小
   */
  size(): number {
    return this.cache.size
  }
}

export const requestDedup = new RequestDedup()
export const responseCache = new ResponseCache()
