import api from './axios'

export const reportApi = {
  /** 신고 접수 */
  create: (data) => api.post('/reports', data),
}
