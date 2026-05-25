<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chatApi } from '@/api/chat'
import { useAuthStore } from '@/stores/auth'
import { useApiError } from '@/composables/useApiError'
import { useWebSocket } from '@/composables/useWebSocket'
import ChatInput from '@/components/chat/ChatInput.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { handle } = useApiError()

const roomId = Number(route.params.roomId)
const room = ref(null)
const messages = ref([])
const cursor = ref(null)
const hasMore = ref(false)
const isLoading = ref(false)
const messageContainer = ref(null)

// WebSocket 연결
const { isConnected, connect, disconnect } = useWebSocket(roomId, (msg) => {
  // 중복 방지
  if (!messages.value.find((m) => m.id === msg.id)) {
    messages.value.push(msg)
    scrollToBottom()
  }
})

async function fetchMessages(reset = false) {
  if (isLoading.value) return
  isLoading.value = true
  try {
    const params = { size: 50 }
    if (!reset && cursor.value) params.cursor = cursor.value
    const { data } = await chatApi.getMessages(roomId, params)

    // API는 최신순으로 오므로 역순으로 넣기
    const fetched = [...data.data.items].reverse()
    if (reset) messages.value = fetched
    else messages.value = [...fetched, ...messages.value]

    cursor.value = data.data.nextCursor
    hasMore.value = data.data.hasNext

    if (reset) await nextTick(() => scrollToBottom())
  } catch (e) {
    handle(e)
  } finally {
    isLoading.value = false
  }
}

async function sendMessage(content, type = 'TEXT') {
  try {
    // WebSocket 우선, 실패 시 REST fallback
    const sent = isConnected.value
      ? false // WebSocket 전송은 useWebSocket.sendMessage를 직접 사용 (ChatInput에서 호출)
      : false

    const { data } = await chatApi.sendMessage(roomId, { type, content })
    if (!messages.value.find((m) => m.id === data.data.id)) {
      messages.value.push(data.data)
      await nextTick(() => scrollToBottom())
    }
  } catch (e) {
    handle(e)
  }
}

function scrollToBottom() {
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}

function formatTime(dt) {
  const d = new Date(dt)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const myId = computed(() => authStore.user?.id)

onMounted(async () => {
  await fetchMessages(true)
  await chatApi.markAsRead(roomId)
  connect()
})

onUnmounted(() => {
  disconnect()
  chatApi.markAsRead(roomId).catch(() => {})
})
</script>

<template>
  <div class="flex flex-col h-screen max-w-2xl mx-auto">
    <!-- 헤더 -->
    <div class="flex items-center gap-3 px-4 py-3 border-b border-gray-100 bg-white">
      <button @click="router.back()" class="p-1 rounded-lg hover:bg-gray-100">
        <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </button>
      <div class="flex-1">
        <p class="font-semibold text-gray-900 text-sm">채팅방</p>
      </div>
    </div>

    <!-- 이전 메시지 불러오기 -->
    <div v-if="hasMore" class="text-center py-2">
      <button
        @click="fetchMessages(false)"
        class="text-xs text-gray-400 hover:text-gray-600"
        :disabled="isLoading"
      >
        이전 메시지 보기
      </button>
    </div>

    <!-- 메시지 목록 -->
    <div
      ref="messageContainer"
      class="flex-1 overflow-y-auto px-4 py-4 space-y-3 bg-gray-50"
    >
      <div
        v-for="msg in messages"
        :key="msg.id"
        :class="['flex', msg.sender.id === myId ? 'justify-end' : 'justify-start']"
      >
        <!-- 상대방 -->
        <div v-if="msg.sender.id !== myId" class="flex items-end gap-2 max-w-[70%]">
          <img
            :src="msg.sender.profileImageUrl || '/default-avatar.png'"
            class="w-8 h-8 rounded-full object-cover flex-shrink-0"
          />
          <div>
            <p class="text-xs text-gray-500 mb-1">{{ msg.sender.nickname }}</p>
            <div class="bg-white rounded-2xl rounded-tl-none px-4 py-2.5 shadow-sm">
              <p class="text-sm text-gray-900 whitespace-pre-wrap">{{ msg.content }}</p>
            </div>
            <p class="text-xs text-gray-400 mt-1">{{ formatTime(msg.createdAt) }}</p>
          </div>
        </div>

        <!-- 내 메시지 -->
        <div v-else class="flex items-end gap-2 max-w-[70%] flex-row-reverse">
          <div>
            <div class="bg-orange-500 rounded-2xl rounded-tr-none px-4 py-2.5">
              <p class="text-sm text-white whitespace-pre-wrap">{{ msg.content }}</p>
            </div>
            <p class="text-xs text-gray-400 mt-1 text-right">{{ formatTime(msg.createdAt) }}</p>
          </div>
        </div>
      </div>

      <div v-if="messages.length === 0 && !isLoading" class="py-10 text-center text-gray-400 text-sm">
        대화를 시작해보세요!
      </div>
    </div>

    <!-- 입력창 -->
    <ChatInput @send="sendMessage" />
  </div>
</template>
