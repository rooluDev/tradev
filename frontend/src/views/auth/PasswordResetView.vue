<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 px-4">
    <div class="w-full max-w-sm">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <!-- Step 1: Request -->
        <template v-if="step === 'request'">
          <h2 class="text-lg font-bold text-gray-900 mb-1">비밀번호 찾기</h2>
          <p class="text-sm text-gray-500 mb-6">가입한 이메일을 입력하시면 재설정 링크를 보내드립니다.</p>
          <form @submit.prevent="onRequest" class="space-y-4">
            <BaseInput v-model="email" label="이메일" type="email" placeholder="이메일 입력" required />
            <BaseButton type="submit" class="w-full" :loading="loading">재설정 링크 발송</BaseButton>
          </form>
        </template>

        <!-- Step 2: Sent -->
        <template v-else-if="step === 'sent'">
          <div class="text-center py-4">
            <div class="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-3">
              <svg class="w-6 h-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8" />
              </svg>
            </div>
            <p class="text-sm text-gray-600">이메일을 확인해주세요.<br />재설정 링크는 30분간 유효합니다.</p>
          </div>
        </template>

        <!-- Step 3: Reset -->
        <template v-else-if="step === 'reset'">
          <h2 class="text-lg font-bold text-gray-900 mb-6">새 비밀번호 설정</h2>
          <form @submit.prevent="onReset" class="space-y-4">
            <BaseInput v-model="newPassword" label="새 비밀번호" type="password" placeholder="영문+숫자 포함 8~20자" required />
            <BaseButton type="submit" class="w-full" :loading="loading">비밀번호 변경</BaseButton>
          </form>
        </template>

        <!-- Step 4: Done -->
        <template v-else>
          <div class="text-center py-4">
            <p class="text-sm text-gray-600 mb-4">비밀번호가 변경되었습니다.</p>
            <RouterLink to="/login" class="text-primary-600 font-medium hover:underline text-sm">로그인하기</RouterLink>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { authApi } from '@/api/auth'
import { useToast } from '@/composables/useToast'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseButton from '@/components/common/BaseButton.vue'

const route = useRoute()
const { success, error } = useToast()

const step = ref('request')
const email = ref('')
const newPassword = ref('')
const loading = ref(false)
const token = ref('')

onMounted(() => {
  if (route.query.token) {
    token.value = route.query.token
    step.value = 'reset'
  }
})

async function onRequest() {
  loading.value = true
  try {
    await authApi.requestPasswordReset(email.value)
    step.value = 'sent'
  } catch {
    // 보안: 이메일 존재 여부 노출 안 함
    step.value = 'sent'
  } finally {
    loading.value = false
  }
}

async function onReset() {
  loading.value = true
  try {
    await authApi.confirmPasswordReset(token.value, newPassword.value)
    success('비밀번호가 변경되었습니다.')
    step.value = 'done'
  } catch (err) {
    error('유효하지 않은 링크이거나 만료되었습니다.')
  } finally {
    loading.value = false
  }
}
</script>
