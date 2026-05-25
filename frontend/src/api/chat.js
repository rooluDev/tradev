import api from './axios'

export const chatApi = {
  /** 채팅방 생성 또는 조회 */
  getOrCreateRoom: (itemId) => api.post('/chat/rooms', null, { params: { itemId } }),

  /** 내 채팅 목록 */
  getRooms: (params) => api.get('/chat/rooms', { params }),

  /** 메시지 목록 */
  getMessages: (roomId, params) => api.get(`/chat/rooms/${roomId}/messages`, { params }),

  /** 메시지 전송 (REST) */
  sendMessage: (roomId, data) => api.post(`/chat/rooms/${roomId}/messages`, data),

  /** 읽음 처리 */
  markAsRead: (roomId) => api.patch(`/chat/rooms/${roomId}/read`),
}
