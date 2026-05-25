import api from './axios'

export const authApi = {
  signup: (data) => api.post('/auth/signup', data),
  login: (data) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout'),
  refresh: () => api.post('/auth/refresh'),
  checkEmail: (email) => api.get('/auth/check-email', { params: { email } }),
  checkNickname: (nickname) => api.get('/auth/check-nickname', { params: { nickname } }),
  verifyEmail: (token) => api.post('/auth/email-verify', null, { params: { token } }),
  resendVerification: (email) => api.post('/auth/email-verify/resend', null, { params: { email } }),
  requestPasswordReset: (email) => api.post('/auth/password-reset/request', { email }),
  confirmPasswordReset: (token, newPassword) => api.post('/auth/password-reset/confirm', { token, newPassword }),
}
