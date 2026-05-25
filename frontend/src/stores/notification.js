import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const unreadCount = ref(0)

  const hasUnread = computed(() => unreadCount.value > 0)

  async function fetchNotifications(params = {}) {
    const { data } = await api.get('/notifications', { params })
    notifications.value = data.data.items
    return data.data
  }

  async function markAsRead(id) {
    await api.patch(`/notifications/${id}/read`)
    const noti = notifications.value.find((n) => n.id === id)
    if (noti) noti.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }

  async function markAllAsRead() {
    await api.patch('/notifications/read-all')
    notifications.value.forEach((n) => (n.isRead = true))
    unreadCount.value = 0
  }

  function addNotification(noti) {
    notifications.value.unshift(noti)
    unreadCount.value++
  }

  function setUnreadCount(count) {
    unreadCount.value = count
  }

  return {
    notifications,
    unreadCount,
    hasUnread,
    fetchNotifications,
    markAsRead,
    markAllAsRead,
    addNotification,
    setUnreadCount,
  }
})
