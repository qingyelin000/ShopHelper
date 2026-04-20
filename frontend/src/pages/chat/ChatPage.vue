<script setup lang="ts">
import { ref } from 'vue'
import { useSSE } from '@/composables/useSSE'
import ChatBubble from '@/components/ChatBubble.vue'

const { messages, loading, send, stop, clearMessages } = useSSE()
const inputText = ref('')

function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  inputText.value = ''
  send(text)
}
</script>

<template>
  <div class="flex flex-col h-[calc(100vh-180px)]">
    <div class="flex-between mb-4">
      <h2 class="text-lg font-bold">🤖 AI 导购助手</h2>
      <el-button text size="small" @click="clearMessages">清空对话</el-button>
    </div>

    <!-- 消息列表 -->
    <div class="flex-1 overflow-y-auto px-4 py-2 bg-white rounded-lg border border-gray-200">
      <div v-if="messages.length === 0" class="flex-center h-full text-gray-400">
        <p>你好！我是 ShopHelper AI 导购，有什么可以帮你的？</p>
      </div>
      <ChatBubble v-for="(msg, i) in messages" :key="i" :message="msg" />
      <div v-if="loading" class="text-center text-gray-400 text-sm py-2">AI 正在思考...</div>
    </div>

    <!-- 输入框 -->
    <div class="flex gap-2 mt-4">
      <el-input
        v-model="inputText"
        placeholder="输入你想咨询的问题..."
        size="large"
        @keyup.enter="handleSend"
      />
      <el-button v-if="loading" type="warning" size="large" @click="stop">停止</el-button>
      <el-button v-else type="primary" size="large" :disabled="!inputText.trim()" @click="handleSend">
        发送
      </el-button>
    </div>
  </div>
</template>
