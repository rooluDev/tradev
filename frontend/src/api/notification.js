import api from './axios'

export const notificationApi = {
  /** 알림 목록 */
  getList: (params) => api.get('/notifications', { params }),

  /** 안 읽은 수 */
  getUnreadCount: () => api.get('/notifications/unread-count'),

  /** 특정 알림 읽음 처리 */
  markAsRead: (id) => api.patch(`/notifications/${id}/read`),

  /** 전체 읽음 처리 */
  markAllAsRead: () => api.patch('/notifications/read-all'),
}
