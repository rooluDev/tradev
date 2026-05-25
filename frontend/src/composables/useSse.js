import { ref, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

export function useSse() {
  const connected = ref(false)
  let eventSource = null
  let retryCount = 0
  let retryTimer = null

  function connect() {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) return

    // SSE는 EventSource API가 Authorization 헤더를 지원하지 않아 쿼리 파라미터로 전달
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
    const token = authStore.accessToken
    const url = `${baseUrl}/notifications/subscribe?token=${encodeURIComponent(token)}`

    eventSource = new EventSource(url)

    eventSource.addEventListener('connect', () => {
      connected.value = true
      retryCount = 0
    })

    eventSource.addEventListener('notification', (e) => {
      const notiStore = useNotificationStore()
      try {
        notiStore.addNotification(JSON.parse(e.data))
      } catch (err) {
        console.warn('[SSE] 알림 파싱 실패:', err)
      }
    })

    eventSource.onerror = () => {
      connected.value = false
      eventSource?.close()
      eventSource = null
      scheduleRetry()
    }
  }

  function scheduleRetry() {
    // 지수 백오프: 1s → 2s → 4s → ... 최대 30s
    const delay = Math.min(1000 * Math.pow(2, retryCount), 30000)
    retryCount++
    retryTimer = setTimeout(connect, delay)
  }

  function disconnect() {
    clearTimeout(retryTimer)
    eventSource?.close()
    eventSource = null
    connected.value = false
    retryCount = 0
  }

  onUnmounted(disconnect)

  return { connected, connect, disconnect }
}
