import api from './axios'

export const slotApi = {
  /** 슬롯 일괄 생성 */
  create: (slots) => api.post('/slots', { slots }),

  /** 판매자 슬롯 월별 조회 */
  getByMonth: (sellerId, year, month) =>
    api.get(`/slots/seller/${sellerId}`, { params: { year, month } }),

  /** 판매자 예약 가능 슬롯 조회 */
  getAvailable: (sellerId) => api.get(`/slots/seller/${sellerId}/available`),

  /** 슬롯 삭제 */
  delete: (slotId) => api.delete(`/slots/${slotId}`),
}

export const reservationApi = {
  /** 예약 요청 */
  create: (data) => api.post('/reservations', data),

  /** 내 예약 목록 */
  getList: (params) => api.get('/reservations/me', { params }),

  /** 예약 상세 */
  getDetail: (reservationId) => api.get(`/reservations/${reservationId}`),

  /** 예약 수락 (판매자) */
  accept: (reservationId) => api.patch(`/reservations/${reservationId}/accept`),

  /** 예약 완료 */
  complete: (reservationId) => api.patch(`/reservations/${reservationId}/complete`),

  /** 예약 취소 */
  cancel: (reservationId, reason) =>
    api.patch(`/reservations/${reservationId}/cancel`, { reason }),
}
