import api from './axios'

export const reviewApi = {
  /** 리뷰 작성 */
  create: (data) => api.post('/reviews', data),

  /** 특정 유저의 리뷰 목록 (커서 기반) */
  getByUser: (userId, params) =>
    api.get(`/users/${userId}/reviews`, { params }),

  /** 리뷰 답글 작성 */
  reply: (reviewId, data) =>
    api.post(`/reviews/${reviewId}/reply`, data),
}
