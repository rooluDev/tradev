<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { notificationApi } from '@/api/notification'

const router = useRouter()
const notificationStore = useNotificationStore()

const isOpen = ref(false)
const isLoading = ref(false)

const typeIcon = {
  TRADE_REQUESTED: '🛍️',
  TRADE_ACCEPTED: '✅',
  TRADE_REJECTED: '❌',
  TRADE_CANCELLED: '🚫',
  TRADE_COMPLETED: '🎉',
  RESERVATION_REQUESTED: '📅',
  RESERVATION_ACCEPTED: '✅',
  RESERVATION_CANCELLED: '🚫',
  RESERVATION_REMINDER: '⏰',
  CHAT_MESSAGE: '💬',
  REVIEW_RECEIVED: '⭐',
  REPORT_PROCESSED: '🔔',
}

async function toggle() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    isLoading.value = true
    try {
      await notificationStore.fetchNotifications({ size: 10 })
    } finally {
      isLoading.value = false
    }
  }
}

async function handleClick(noti) {
  if (!noti.isRead) {
    await notificationStore.markAsRead(noti.id)
  }
  isOpen.value = false
  if (noti.link) router.push(noti.link)
}

async function markAll() {
  await notificationStore.markAllAsRead()
}

function formatTime(dt) {
  if (!dt) return ''
  const date = new Date(dt)
  const now = new Date()
  const diff = Math.floor((now - date) / 1000)
  if (diff < 60) return '방금 전'
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`
  return `${Math.floor(diff / 86400)}일 전`
}
</script>

<template>
  <div class="relative">
    <!-- 벨 아이콘 버튼 -->
    <button
      @click="toggle"
      class="relative p-2 rounded-lg hover:bg-gray-100 text-gray-600"
    >
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
      </svg>
      <span
        v-if="notificationStore.hasUnread"
        class="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"
      />
    </button>

    <!-- 드롭다운 -->
    <div
      v-if="isOpen"
      class="absolute right-0 top-full mt-1 w-80 bg-white rounded-xl shadow-lg border border-gray-200 z-50 overflow-hidden"
    >
      <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100">
        <span class="font-semibold text-sm text-gray-900">알림</span>
        <button
          v-if="notificationStore.hasUnread"
          @click="markAll"
          class="text-xs text-orange-500 hover:underline"
        >
          모두 읽음
        </button>
      </div>

      <div v-if="isLoading" class="py-8 text-center text-gray-400 text-sm">
        <div class="inline-block w-4 h-4 border-2 border-orange-400 border-t-transparent rounded-full animate-spin"></div>
      </div>

      <ul v-else-if="notificationStore.notifications.length === 0" class="py-8 text-center text-gray-400 text-sm">
        알림이 없습니다
      </ul>

      <ul v-else class="max-h-80 overflow-y-auto divide-y divide-gray-50">
        <li
          v-for="noti in notificationStore.notifications"
          :key="noti.id"
          @click="handleClick(noti)"
          :class="[
            'flex items-start gap-3 px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors',
            !noti.isRead && 'bg-orange-50'
          ]"
        >
          <span class="text-lg flex-shrink-0 mt-0.5">{{ typeIcon[noti.type] || '🔔' }}</span>
          <div class="flex-1 min-w-0">
            <p class="text-sm text-gray-900 line-clamp-2">{{ noti.message }}</p>
            <p class="text-xs text-gray-400 mt-1">{{ formatTime(noti.createdAt) }}</p>
          </div>
          <div v-if="!noti.isRead" class="w-2 h-2 bg-orange-500 rounded-full flex-shrink-0 mt-2"></div>
        </li>
      </ul>

      <div class="border-t border-gray-100 px-4 py-2 text-center">
        <router-link
          to="/notifications"
          @click="isOpen = false"
          class="text-xs text-gray-500 hover:text-orange-500"
        >
          전체 알림 보기
        </router-link>
      </div>
    </div>

    <!-- 외부 클릭 닫기 -->
    <div v-if="isOpen" @click="isOpen = false" class="fixed inset-0 z-40" />
  </div>
</template>
