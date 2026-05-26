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

  /**
   * 새로고침 시 HttpOnly refresh token 쿠키로 세션 복원.
   * axios 인터셉터를 거치지 않도록 fetch 직접 사용 (오류 시 리다이렉트 방지).
   */
  async function restoreSession() {
    try {
      const base = import.meta.env.VITE_API_BASE_URL || '/api'
      const res = await fetch(`${base}/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
      })
      if (!res.ok) return
      const json = await res.json()
      accessToken.value = json.data.accessToken
      await fetchMyInfo()
    } catch {
      // refresh token 없거나 만료 → 로그아웃 상태 유지
    }
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
    restoreSession,
  }
})
