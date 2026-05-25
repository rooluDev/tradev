<template>
  <div class="max-w-screen-sm mx-auto px-4 py-4">
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-lg font-bold text-gray-900">알림</h1>
      <button
        v-if="notiStore.notifications.length"
        class="text-xs text-primary-600 hover:underline"
        @click="notiStore.markAllAsRead"
      >
        모두 읽음
      </button>
    </div>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 5" :key="i" class="skeleton h-16 rounded-xl" />
    </div>

    <div v-else-if="notiStore.notifications.length === 0" class="text-center py-20">
      <p class="text-gray-400 text-sm">알림이 없습니다.</p>
    </div>

    <div v-else class="space-y-2">
      <div
        v-for="noti in notiStore.notifications"
        :key="noti.id"
        :class="['bg-white rounded-xl border p-4 cursor-pointer transition-colors', noti.isRead ? 'border-gray-100' : 'border-primary-100 bg-primary-50/30']"
        @click="notiStore.markAsRead(noti.id)"
      >
        <p class="text-sm text-gray-800">{{ noti.content }}</p>
        <p class="text-xs text-gray-400 mt-1">{{ formatDate(noti.createdAt) }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useNotificationStore } from '@/stores/notification'

const notiStore = useNotificationStore()
const loading = ref(true)

onMounted(async () => {
  try {
    await notiStore.fetchNotifications({ pageSize: 30 })
  } finally {
    loading.value = false
  }
})

function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '방금'
  if (diff < 3600) return Math.floor(diff / 60) + '분 전'
  if (diff < 86400) return Math.floor(diff / 3600) + '시간 전'
  return d.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })
}
</script>
