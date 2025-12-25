import { defineConfig } from 'vite'
import type { Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'

function aiSummaryDevServer(): Plugin {
  return {
    name: 'ai-summary-dev-server',
    configureServer(server) {
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
export default defineConfig({
  plugins: [vue(), aiSummaryDevServer()],
})
