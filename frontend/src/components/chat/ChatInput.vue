<script setup>
import { ref } from 'vue'

const emit = defineEmits(['send'])
const content = ref('')
const isSending = ref(false)

async function send() {
  const text = content.value.trim()
  if (!text || isSending.value) return
  isSending.value = true
  try {
    await emit('send', text, 'TEXT')
    content.value = ''
  } finally {
    isSending.value = false
  }
}

function onKeydown(e) {
  // Shift+Enter → 줄바꿈, Enter → 전송
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}
</script>

<template>
  <div class="bg-white border-t border-gray-100 px-4 py-3 safe-area-inset-bottom">
    <div class="flex items-end gap-2">
      <textarea
        v-model="content"
        @keydown="onKeydown"
        rows="1"
        placeholder="메시지를 입력하세요..."
        class="flex-1 resize-none border border-gray-200 rounded-2xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-orange-400 max-h-32 overflow-y-auto"
        style="min-height: 42px;"
      />
      <button
        @click="send"
        :disabled="!content.trim() || isSending"
        class="flex-shrink-0 w-10 h-10 bg-orange-500 text-white rounded-full flex items-center justify-center disabled:opacity-40 hover:bg-orange-600 transition-colors"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/>
        </svg>
      </button>
    </div>
  </div>
</template>
