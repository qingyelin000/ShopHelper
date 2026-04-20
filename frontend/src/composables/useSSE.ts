import { ref, onUnmounted } from 'vue'
import { sendChatMessage, type ChatMessage } from '@/api/agent'

/**
 * SSE 聊天组合式函数
 * 封装 Agent 对话的流式消息处理
 */
export function useSSE() {
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)
  let abortController: AbortController | null = null

  function send(content: string) {
    // 添加用户消息
    messages.value.push({ role: 'user', content })

    // 添加空的 assistant 消息占位
    const assistantIndex = messages.value.length
    messages.value.push({ role: 'assistant', content: '' })

    loading.value = true
    abortController = new AbortController()

    sendChatMessage(content, {
      signal: abortController.signal,
      onMessage(chunk: string) {
        const msg = messages.value[assistantIndex]
        if (msg) msg.content += chunk
      },
      onDone() {
        loading.value = false
        abortController = null
      },
      onError() {
        loading.value = false
        abortController = null
      },
    })
  }

  function stop() {
    abortController?.abort()
    loading.value = false
    abortController = null
  }

  function clearMessages() {
    messages.value = []
  }

  onUnmounted(() => stop())

  return { messages, loading, send, stop, clearMessages }
}
