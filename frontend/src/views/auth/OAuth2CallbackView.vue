<script setup>
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

onMounted(async () => {
  const token = route.query.token

  if (!token) {
    router.replace('/login?error=oauth2')
    return
  }

  try {
    // access token 저장
    authStore.setAccessToken(token)

    // 사용자 정보 불러오기
    await authStore.fetchMyInfo()

    // 홈으로 이동
    router.replace('/')
  } catch (e) {
    authStore.setAccessToken(null)
    router.replace('/login?error=oauth2')
  }
})
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-screen bg-gray-50">
    <div class="flex flex-col items-center gap-4">
      <svg class="animate-spin h-10 w-10 text-green-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
      </svg>
      <p class="text-gray-600 text-sm">Google 로그인 처리 중...</p>
    </div>
  </div>
</template>
