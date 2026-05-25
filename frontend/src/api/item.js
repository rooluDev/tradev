import api from './axios'

export const itemApi = {
  getCategories: () => api.get('/categories'),
  getItems: (params) => api.get('/items', { params }),
  getItem: (itemId) => api.get(`/items/${itemId}`),
  createItem: (data) => api.post('/items', data),
  updateItem: (itemId, data) => api.put(`/items/${itemId}`, data),
  deleteItem: (itemId) => api.delete(`/items/${itemId}`),
  boostItem: (itemId) => api.post(`/items/${itemId}/boost`),
  toggleVisibility: (itemId) => api.patch(`/items/${itemId}/visibility`),
  toggleWishlist: (itemId) => api.post(`/items/${itemId}/wishlist`),
  getPresignedUrls: (files) => api.post('/items/images/presigned', { files }),
}
