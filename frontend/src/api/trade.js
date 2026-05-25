import api from './axios'

export const tradeApi = {
  /** 거래 요청 */
  request: (data) => api.post('/trades', data),

  /** 내 거래 목록 */
  getList: (params) => api.get('/trades', { params }),

  /** 거래 상세 */
  getDetail: (tradeId) => api.get(`/trades/${tradeId}`),

  /** 거래 수락 (판매자) */
  accept: (tradeId) => api.patch(`/trades/${tradeId}/accept`),

  /** 거래 거절 (판매자) */
  reject: (tradeId) => api.patch(`/trades/${tradeId}/reject`),

  /** 거래 완료 확인 (구매자/판매자) */
  confirm: (tradeId) => api.patch(`/trades/${tradeId}/confirm`),

  /** 거래 취소 */
  cancel: (tradeId, reason) => api.patch(`/trades/${tradeId}/cancel`, { reason }),
}
