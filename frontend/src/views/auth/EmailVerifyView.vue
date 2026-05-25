<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 px-4">
    <div class="w-full max-w-sm text-center">
      <div v-if="verified" class="bg-white rounded-xl shadow-sm border border-gray-100 p-8">
        <div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h2 class="text-xl font-bold text-gray-900 mb-2">이메일 인증 완료!</h2>
        <p class="text-sm text-gray-500 mb-6">이제 Tradev를 이용하실 수 있습니다.</p>
        <RouterLink to="/login" class="btn-primary inline-block px-6 py-2.5 rounded-lg text-sm">
          로그인하기
        </RouterLink>
      </div>

      <div v-else class="bg-white rounded-xl shadow-sm border border-gray-100 p-8">
        <div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
        </div>
        <h2 class="text-xl font-bold text-gray-900 mb-2">이메일 인증 필요</h2>
        <p class="text-sm text-gray-500 mb-1">
          <span class="font-medium text-gray-700">{{ email }}</span>으로
        </p>
        <p class="text-sm text-gray-500 mb-6">인증 링크를 발송했습니다.</p>

        <p class="text-sm text-gray-500 mb-2">이메일을 받지 못하셨나요?</p>
        <button
          :disabled="countdown > 0 || resending"
          class="text-sm text-primary-600 hover:underline disabled:text-gray-400 disabled:no-underline"
          @click="resend"
        >
          {{ countdown > 0 ? `재발송 (${countdown}s)` : '인증 메일 재발송' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { authApi } from '@/api/auth'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const { success, error } = useToast()

const verified = ref(false)
const email = ref(route.query.email || '')
const countdown = ref(0)
const resending = ref(false)

onMounted(async () => {
  const token = route.query.token
  if (token) {
    try {
      await authApi.verifyEmail(token)
      verified.value = true
    } catch (err) {
      error('인증 링크가 유효하지 않거나 만료되었습니다.')
    }
  }
})

async function resend() {
  if (!email.value) return
  resending.value = true
  try {
    await authApi.resendVerification(email.value)
    success('인증 메일을 재발송했습니다.')
    startCountdown()
  } catch (err) {
    error('재발송에 실패했습니다.')
  } finally {
    resending.value = false
  }
}

function startCountdown() {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}
</script>
