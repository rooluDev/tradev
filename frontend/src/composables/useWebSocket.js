import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '@/stores/auth'

/**
 * WebSocket STOMP 채팅 연결 훅
 * @param {number} roomId - 구독할 채팅방 ID
 * @param {function} onMessage - 메시지 수신 콜백
 */
export function useWebSocket(roomId, onMessage) {
  const authStore = useAuthStore()
  const isConnected = ref(false)
  let stompClient = null

  function connect() {
    const token = authStore.accessToken
    if (!token) return

    stompClient = new Client({
      webSocketFactory: () =>
        new SockJS(`${import.meta.env.VITE_WS_URL || ''}/ws/chat?token=${token}`),

      onConnect: () => {
        isConnected.value = true

        // 채팅방 구독
        stompClient.subscribe(`/topic/chat/${roomId}`, (frame) => {
          try {
            const message = JSON.parse(frame.body)
            onMessage(message)
          } catch (e) {
            console.error('[WS] 메시지 파싱 오류:', e)
          }
        })
      },

      onDisconnect: () => {
        isConnected.value = false
      },

      onStompError: (frame) => {
        console.error('[WS] STOMP 오류:', frame)
        isConnected.value = false
      },

      reconnectDelay: 3000,
    })

    stompClient.activate()
  }

  function disconnect() {
    if (stompClient?.active) {
      stompClient.deactivate()
    }
    isConnected.value = false
  }

  /**
   * 메시지 전송 (WebSocket)
   */
  function sendMessage(content, type = 'TEXT') {
    if (!stompClient?.active) return false
    stompClient.publish({
      destination: `/app/chat/${roomId}/send`,
      body: JSON.stringify({ type, content }),
    })
    return true
  }

  onUnmounted(() => disconnect())

  return { isConnected, connect, disconnect, sendMessage }
}
