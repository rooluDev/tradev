import api from './axios'

export const userApi = {
  getProfile: (userId) => api.get(`/users/${userId}`),
  getMyInfo: () => api.get('/users/me'),
  updateProfile: (data) => api.put('/users/me', data),
  withdraw: () => api.delete('/users/me'),
  getMyWishlist: (params) => api.get('/users/me/wishlist', { params }),
}
