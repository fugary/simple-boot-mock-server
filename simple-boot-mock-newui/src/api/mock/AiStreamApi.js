import { useGlobalConfigStore } from '@/stores/GlobalConfigStore'
import { useLoginConfigStore } from '@/stores/LoginConfigStore'
import { $i18nBundle } from '@/messages'

const eventCallbacks = { delta: 'onDelta', finish: 'onFinish', error: 'onError' }

/** 共用鉴权、SSE 分帧与中断检测。 */
export const streamAiRequest = async (path, params, callbacks = {}, signal = null) => {
  const loginStore = useLoginConfigStore()

  const url = `${(import.meta.env.VITE_APP_API_BASE_URL || '').replace(/\/$/, '')}${path}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
    locale: useGlobalConfigStore().currentLocale
  }
  if (loginStore.accessToken) {
    headers.Authorization = `Bearer ${loginStore.accessToken}`
  }

  const response = await fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify(params),
    signal
  })

  // 鉴权失败等 JSON 响应不能作为 SSE 读取。
  if (response.headers.get('content-type')?.includes('application/json')) {
    const data = await response.json()
    const error = Object.assign(new Error(data.message || $i18nBundle('mock.msg.aiStreamInterrupted')), { data })
    throw error
  }

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || `Request failed with status ${response.status}`)
  }

  if (!response.body) throw new Error($i18nBundle('mock.msg.aiStreamInterrupted'))
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let currentEvent = 'message'
  let dataLines = []

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (signal?.aborted) throw new DOMException('Aborted', 'AbortError')
      if (done) throw new Error($i18nBundle('mock.msg.aiStreamInterrupted'))

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const rawLine of lines) {
        if (signal?.aborted) throw new DOMException('Aborted', 'AbortError')
        const line = rawLine.replace(/\r$/, '')
        if (!line) {
          // 仅分发完整 SSE 事件，避免将被截断的 finish 消息当作成功。
          if (dataLines.length) {
            let data
            try {
              data = JSON.parse(dataLines.join('\n'))
            } catch {
              throw new Error($i18nBundle('mock.msg.aiStreamInterrupted'))
            }
            if (currentEvent === 'finish' && data?.status !== 'SUCCESS') {
              throw new Error($i18nBundle('mock.msg.aiStreamInterrupted'))
            }
            callbacks[eventCallbacks[currentEvent]]?.(data)
            if (currentEvent === 'finish' || currentEvent === 'error') return
          }
          currentEvent = 'message'
          dataLines = []
        } else if (line.startsWith('event:')) {
          currentEvent = line.substring(6).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.substring(5).replace(/^ /, ''))
        }
      }
    }
  } finally {
    // 终止事件之后不再等待连接关闭；清理失败不能覆盖原始错误。
    reader.cancel().catch(() => {})
    reader.releaseLock()
  }
}
