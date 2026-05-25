import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const accessToken = ref(null)

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  function setAccessToken(token) {
    accessToken.value = token
  }

  function setUser(userData) {
    user.value = userData
  }

  async function login(credentials) {
    const { data } = await authApi.login(credentials)
    accessToken.value = data.data.accessToken
    user.value = data.data.user
    return data.data
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      accessToken.value = null
      user.value = null
    }
  }

  async function fetchMyInfo() {
    const { default: api } = await import('@/api/axios')
    const { data } = await api.get('/users/me')
    user.value = data.data
    return data.data
  }

  return {
    user,
    accessToken,
    isLoggedIn,
    isAdmin,
    setAccessToken,
    setUser,
    login,
    logout,
    fetchMyInfo,
  }
})
