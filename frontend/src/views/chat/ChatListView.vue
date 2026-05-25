<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { chatApi } from '@/api/chat'
import { useAuthStore } from '@/stores/auth'
import { useApiError } from '@/composables/useApiError'

const router = useRouter()
const authStore = useAuthStore()
const { handle } = useApiError()

const rooms = ref([])
const cursor = ref(null)
const hasNext = ref(false)
const isLoading = ref(false)

async function fetchRooms(reset = false) {
  if (isLoading.value) return
  isLoading.value = true
  try {
    const params = { size: 20 }
    if (!reset && cursor.value) params.cursor = cursor.value
    const { data } = await chatApi.getRooms(params)
    if (reset) rooms.value = data.data.items
    else rooms.value.push(...data.data.items)
    cursor.value = data.data.nextCursor
    hasNext.value = data.data.hasNext
  } catch (e) {
    handle(e)
  } finally {
    isLoading.value = false
  }
}

function goToRoom(roomId) {
  router.push(`/chat/${roomId}`)
}

function formatTime(dt) {
  if (!dt) return ''
  const date = new Date(dt)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  if (isToday) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

onMounted(() => fetchRooms(true))
</script>

<template>
  <div class="max-w-2xl mx-auto">
    <div class="sticky top-0 bg-white z-10 px-4 py-4 border-b border-gray-100">
      <h1 class="text-lg font-bold text-gray-900">채팅</h1>
    </div>

    <div v-if="isLoading && rooms.length === 0" class="py-16 text-center text-gray-400">
      <div class="inline-block w-6 h-6 border-2 border-orange-500 border-t-transparent rounded-full animate-spin"></div>
    </div>

    <div v-else-if="rooms.length === 0" class="py-16 text-center text-gray-400">
      <p class="text-4xl mb-3">💬</p>
      <p>진행 중인 채팅이 없습니다.</p>
    </div>

    <ul v-else class="divide-y divide-gray-100">
      <li
        v-for="room in rooms"
        :key="room.id"
        @click="goToRoom(room.id)"
        class="flex items-center gap-3 px-4 py-4 hover:bg-gray-50 cursor-pointer transition-colors"
      >
        <!-- 상대방 프로필 이미지 -->
        <div class="relative flex-shrink-0">
          <img
            :src="room.opponent.profileImageUrl || '/default-avatar.png'"
            :alt="room.opponent.nickname"
            class="w-12 h-12 rounded-full object-cover bg-gray-200"
          />
          <span
            v-if="room.unreadCount > 0"
            class="absolute -top-1 -right-1 min-w-[18px] h-[18px] bg-red-500 text-white text-xs font-bold rounded-full flex items-center justify-center px-1"
          >
            {{ room.unreadCount > 99 ? '99+' : room.unreadCount }}
          </span>
        </div>

        <!-- 채팅 정보 -->
        <div class="flex-1 min-w-0">
          <div class="flex items-baseline justify-between">
            <span class="font-medium text-gray-900 truncate">{{ room.opponent.nickname }}</span>
            <span class="text-xs text-gray-400 flex-shrink-0 ml-2">{{ formatTime(room.updatedAt) }}</span>
          </div>
          <p class="text-sm text-gray-500 truncate mt-0.5">
            {{ room.item.title }}
          </p>
          <p v-if="room.lastMessage" class="text-sm text-gray-400 truncate">
            {{ room.lastMessage }}
          </p>
        </div>
      </li>
    </ul>

    <!-- 더 보기 -->
    <div v-if="hasNext" class="p-4 text-center">
      <button
        @click="fetchRooms(false)"
        :disabled="isLoading"
        class="text-sm text-orange-500 hover:underline disabled:opacity-50"
      >
        {{ isLoading ? '불러오는 중...' : '더 보기' }}
      </button>
    </div>
  </div>
</template>
