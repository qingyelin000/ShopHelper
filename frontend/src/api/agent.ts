import { fetchEventSource } from '@microsoft/fetch-event-source'
import { useUserStore } from '@/stores/user'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

/**
 * 发送 Agent 对话请求（SSE 流式）
 * 使用 @microsoft/fetch-event-source 支持 POST + JWT
 */
export function sendChatMessage(
  message: string,
  options: {
    onMessage: (content: string) => void
    onDone: () => void
    onError?: (err: Error) => void
    signal?: AbortSignal
  },
) {
  const userStore = useUserStore()
  const apiBase = import.meta.env.VITE_API_BASE

  fetchEventSource(`${apiBase}/v1/agent/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${userStore.accessToken}`,
    },
    body: JSON.stringify({ message }),
    signal: options.signal,

    onmessage(ev) {
      if (ev.data === '[DONE]') {
        options.onDone()
        return
      }
      options.onMessage(ev.data)
    },

    onerror(err) {
      options.onError?.(err as Error)
      throw err // 阻止自动重连
    },

    openWhenHidden: true,
  })
}
