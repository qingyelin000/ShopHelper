<script setup lang="ts">
import type { ChatMessage } from '@/api/agent'

defineProps<{
  message: ChatMessage
}>()
</script>

<template>
  <div class="chat-bubble" :class="message.role === 'user' ? 'justify-end' : 'justify-start'" >
    <div
      class="max-w-70% rounded-lg px-4 py-2 text-sm leading-relaxed"
      :class="message.role === 'user'
        ? 'bg-primary text-white rounded-br-none'
        : 'bg-gray-100 text-gray-800 rounded-bl-none'"
    >
      {{ message.content }}
    </div>
  </div>
</template>

<style scoped>
.chat-bubble {
  display: flex;
  margin-bottom: 12px;
}
</style>
