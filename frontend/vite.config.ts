import { defineConfig } from 'vite'
import type { Plugin } from 'vite'
import path from 'node:path'
import vue from '@vitejs/plugin-vue'

function aiSummaryDevServer(): Plugin {
  return {
    name: 'ai-summary-dev-server',
    configureServer(server) {
      server.middlewares.use('/api/ai/summary/stream', async (req, res) => {
        if (req.method !== 'POST') {
          res.statusCode = 405
          res.setHeader('Content-Type', 'text/plain; charset=utf-8')
          res.end('Method Not Allowed')
          return
        }

        let body = ''
        req.on('data', (chunk) => {
          body += String(chunk)
        })

        req.on('end', () => {
          try {
            const parsed = body ? JSON.parse(body) : {}
            const content = String(parsed?.content || '')
              .replace(/\s+/g, ' ')
              .trim()

            const summary =
              content.length <= 140
                ? content || '暂无可摘要的内容。'
                : `${content.slice(0, 140)}…`

            res.statusCode = 200
            res.setHeader('Content-Type', 'text/plain; charset=utf-8')
            res.setHeader('Cache-Control', 'no-cache')
            res.setHeader('Connection', 'keep-alive')
            res.setHeader('X-Accel-Buffering', 'no')
            res.flushHeaders?.()

            const chunks = summary.match(/.{1,8}/g) || []
            let idx = 0

            const timer = setInterval(() => {
              if (idx >= chunks.length) {
                clearInterval(timer)
                res.end()
                return
              }
              res.write(chunks[idx])
              idx += 1
            }, 20)

            req.on('close', () => {
              clearInterval(timer)
            })
          } catch {
            res.statusCode = 400
            res.setHeader('Content-Type', 'text/plain; charset=utf-8')
            res.end('Invalid JSON')
          }
        })
      })

      server.middlewares.use('/api/ai/summary', async (req, res) => {
        if (req.method !== 'POST') {
          res.statusCode = 405
          res.setHeader('Content-Type', 'text/plain; charset=utf-8')
          res.end('Method Not Allowed')
          return
        }

        let body = ''
        req.on('data', (chunk) => {
          body += String(chunk)
        })

        req.on('end', () => {
          try {
            const parsed = body ? JSON.parse(body) : {}
            const content = String(parsed?.content || '')
              .replace(/\s+/g, ' ')
              .trim()

            const summary =
              content.length <= 140
                ? content || '暂无可摘要的内容。'
                : `${content.slice(0, 140)}…`

            res.statusCode = 200
            res.setHeader('Content-Type', 'application/json; charset=utf-8')
            res.end(JSON.stringify({ summary }))
          } catch {
            res.statusCode = 400
            res.setHeader('Content-Type', 'text/plain; charset=utf-8')
            res.end('Invalid JSON')
          }
        })
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const enableAiMock = mode === 'mock' || process.env.VITE_AI_MOCK === 'true'

  return {
    plugins: [vue(), enableAiMock ? aiSummaryDevServer() : undefined].filter(Boolean) as Plugin[],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8087',
          changeOrigin: true,
        },
      },
    },
  }
})
